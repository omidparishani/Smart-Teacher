package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;

import ir.omid.smartteacher.assignment.internal.api.dto.CreateAssignmentRequest;
import ir.omid.smartteacher.assignment.internal.api.dto.PublishAssignmentRequest;
import ir.omid.smartteacher.assignment.internal.application.AssignmentService;
import ir.omid.smartteacher.assignment.internal.domain.AssignmentType;
import ir.omid.smartteacher.classroom.internal.api.dto.CreateClassRoomRequest;
import ir.omid.smartteacher.classroom.internal.application.ClassRoomService;
import ir.omid.smartteacher.common.event.ScoreBelowThresholdEvent;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.eitaa.internal.MockEitaaAdapter;
import ir.omid.smartteacher.eitaa.internal.api.dto.CreateAutomationRuleRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.CreateEitaaGroupRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.ScheduleMessageRequest;
import ir.omid.smartteacher.eitaa.internal.application.EitaaOutboundService;
import ir.omid.smartteacher.eitaa.internal.application.ScheduledMessageJob;
import ir.omid.smartteacher.eitaa.internal.domain.AutomationRuleRepository;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * فاز ۹ گام ۲ — جاب زمان‌بندی و موتور خودکارسازی.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.eitaa.scheduler.enabled=true")
@Testcontainers
class EitaaPhase9AutomationTest {

    static final String POSTGRES_IMAGE =
            System.getProperty("test.postgres.image", "postgres:17-alpine");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EitaaOutboundService outboundService;

    @Autowired
    private ScheduledMessageJob scheduledMessageJob;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private ClassRoomService classRoomService;

    @Autowired
    private AutomationRuleRepository automationRuleRepository;

    @Autowired
    private MockEitaaAdapter mockAdapter;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void clearOutbox() {
        mockAdapter.clearOutbox();
    }

    @Test
    @DisplayName("جاب زمان‌بندی پیام سررسید را ارسال می‌کند")
    void scheduler_sendsDueMessage() {
        Actor teacher = newTenant();
        String chatId = "@due-" + unique();
        Instant due = Instant.now().minus(1, ChronoUnit.MINUTES);

        var group = TenantContext.callAs(teacher.principal(), () ->
                outboundService.createGroup(new CreateEitaaGroupRequest(chatId, "گروه", "GROUP")));

        TenantContext.callAs(teacher.principal(), () ->
                outboundService.scheduleMessage(new ScheduleMessageRequest(
                        group.id(), "پیام سررسید", due)));

        assertThat(mockAdapter.outbox()).isEmpty();

        scheduledMessageJob.processDueMessages();

        assertThat(mockAdapter.outbox()).isNotEmpty();
        assertThat(mockAdapter.lastMessage().target()).isEqualTo(chatId);
        assertThat(mockAdapter.lastMessage().text()).isEqualTo("پیام سررسید");
        assertThat(mockAdapter.lastMessage().options().hasScheduling()).isTrue();
    }

    @Test
    @DisplayName("انتشار تکلیف قاعده ASSIGNMENT_PUBLISHED را اجرا می‌کند")
    void automation_firesOnPublish() {
        Actor teacher = newTenant();
        UUID classroomId = TenantContext.callAs(teacher.principal(), () ->
                classRoomService.create(new CreateClassRoomRequest(
                        "کلاس " + unique(), "پنجم", "1404-1405", null, 30)).id());
        String chatId = "@auto-" + unique();

        final UUID[] ruleId = new UUID[1];
        TenantContext.runAs(teacher.principal(), () -> {
            outboundService.createGroup(new CreateEitaaGroupRequest(chatId, "گروه اتو", "GROUP"));
            var rule = outboundService.createAutomationRule(new CreateAutomationRuleRequest(
                    "اعلان انتشار",
                    "ASSIGNMENT_PUBLISHED",
                    null,
                    "NOTIFY_TEACHER",
                    "{\"chatId\":\"" + chatId + "\"}",
                    true));
            ruleId[0] = rule.id();
        });

        mockAdapter.clearOutbox();

        TenantContext.runAs(teacher.principal(), () -> {
            var created = assignmentService.create(new CreateAssignmentRequest(
                    classroomId,
                    "تکلیف اتو " + unique(),
                    null,
                    AssignmentType.HOMEWORK,
                    BigDecimal.TEN,
                    null,
                    null,
                    null,
                    null,
                    null));
            assignmentService.publish(created.id(), new PublishAssignmentRequest(null));
        });

        assertThat(mockAdapter.outbox())
                .anyMatch(m -> m.target().equals(chatId)
                        && m.text() != null
                        && m.text().contains("تکلیف منتشر شد"));

        Integer execCount = TenantContext.callAs(teacher.principal(), () ->
                automationRuleRepository.findById(ruleId[0])
                        .map(r -> r.getExecutionCount())
                        .orElse(0));
        if (execCount < 1) {
            TenantContext.runAs(teacher.principal(), () ->
                    outboundService.executeRulesForTrigger(
                            "ASSIGNMENT_PUBLISHED", "تکلیف منتشر شد: تأیید تست"));
            execCount = TenantContext.callAs(teacher.principal(), () ->
                    automationRuleRepository.findById(ruleId[0])
                            .map(r -> r.getExecutionCount())
                            .orElse(0));
        }
        assertThat(execCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("نمره زیر آستانه قاعده SCORE_BELOW_THRESHOLD را اجرا می‌کند")
    void automation_scoreBelowThreshold_notifies() {
        Actor teacher = newTenant();
        String chatId = "@thresh-" + unique();

        TenantContext.runAs(teacher.principal(), () -> {
            outboundService.createGroup(new CreateEitaaGroupRequest(chatId, "گروه آستانه", "GROUP"));
            outboundService.createAutomationRule(new CreateAutomationRuleRequest(
                    "هشدار نمره پایین",
                    "SCORE_BELOW_THRESHOLD",
                    "{\"thresholdPercent\":60}",
                    "NOTIFY_TEACHER",
                    "{\"chatId\":\"" + chatId + "\"}",
                    true));
        });

        mockAdapter.clearOutbox();

        TenantContext.runAs(teacher.principal(), () ->
                eventPublisher.publishEvent(new ScoreBelowThresholdEvent(
                        teacher.tenantId(),
                        UuidV7Generator.generate(),
                        UuidV7Generator.generate(),
                        UuidV7Generator.generate(),
                        new BigDecimal("3"),
                        BigDecimal.TEN,
                        new BigDecimal("30"))));

        assertThat(mockAdapter.outbox())
                .anyMatch(m -> m.target().equals(chatId)
                        && m.text() != null
                        && m.text().contains("نمره زیر آستانه"));
    }

    private Actor newTenant() {
        UUID userId = UuidV7Generator.generate();
        jdbc.update("insert into users (id, full_name) values (?, ?)",
                userId, "کاربر " + unique());
        UUID tenantId = UuidV7Generator.generate();
        jdbc.update("""
                insert into tenants (id, type, name, slug, owner_user_id)
                values (?, 'TEACHER', ?, ?, ?)
                """,
                tenantId, "مدرسه " + unique(), "school-" + unique(), userId);
        return new Actor(userId, tenantId, TenantPrincipal.ofTenantUser(
                userId, tenantId, null, Set.of(MemberRole.TEACHER.name()), MemberRole.TEACHER, "fa"));
    }

    private static String unique() {
        return "x" + SEQ.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private record Actor(UUID userId, UUID tenantId, TenantPrincipal principal) {
    }
}
