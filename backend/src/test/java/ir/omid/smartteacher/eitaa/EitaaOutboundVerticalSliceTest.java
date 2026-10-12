package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;

import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.eitaa.internal.MockEitaaAdapter;
import ir.omid.smartteacher.eitaa.internal.api.dto.ConnectClassRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.CreateEitaaGroupRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.ScheduleMessageRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.ScheduledMessageResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.SendGroupMessageRequest;
import ir.omid.smartteacher.eitaa.internal.api.dto.SendMessageResponse;
import ir.omid.smartteacher.eitaa.internal.application.EitaaOutboundService;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * برش عمودی فاز ۹ گام ۱ — گروه‌ها و ارسال با Mock.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class EitaaOutboundVerticalSliceTest {

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
    private MockEitaaAdapter mockAdapter;

    @BeforeEach
    void clearOutbox() {
        mockAdapter.clearOutbox();
    }

    @Test
    @DisplayName("ثبت گروه، اتصال به کلاس، ارسال فوری با Mock")
    void createConnectAndSend_usesMock() {
        Actor teacher = newTenant();
        UUID classroomId = insertClassroom(teacher);
        String chatId = "@group-" + unique();

        var group = TenantContext.callAs(teacher.principal(), () ->
                outboundService.createGroup(new CreateEitaaGroupRequest(chatId, "گروه ریاضی", "GROUP")));

        assertThat(group.eitaaChatId()).isEqualTo(chatId);

        var connected = TenantContext.callAs(teacher.principal(), () ->
                outboundService.connectClass(group.id(), new ConnectClassRequest(classroomId)));
        assertThat(connected.classroomId()).isEqualTo(classroomId);

        SendMessageResponse sent = TenantContext.callAs(teacher.principal(), () ->
                outboundService.sendMessage(group.id(),
                        new SendGroupMessageRequest("تکلیف فردا")));

        assertThat(sent.success()).isTrue();
        assertThat(mockAdapter.outbox()).hasSize(1);
        assertThat(mockAdapter.lastMessage().target()).isEqualTo(chatId);
        assertThat(mockAdapter.lastMessage().text()).isEqualTo("تکلیف فردا");
        assertThat(mockAdapter.lastMessage().options().hasScheduling()).isFalse();
    }

    @Test
    @DisplayName("زمان‌بندی PENDING می‌ماند و جاب با پارامتر date ارسال می‌کند")
    void schedule_storesPending_jobSendsWithDateParam() {
        Actor teacher = newTenant();
        String chatId = "@sched-" + unique();
        Instant when = Instant.parse("2026-11-01T08:00:00Z");

        var group = TenantContext.callAs(teacher.principal(), () ->
                outboundService.createGroup(new CreateEitaaGroupRequest(chatId, "گروه", "GROUP")));

        ScheduledMessageResponse scheduled = TenantContext.callAs(teacher.principal(), () ->
                outboundService.scheduleMessage(new ScheduleMessageRequest(
                        group.id(), "پیام زمان‌بندی", when)));

        assertThat(scheduled.status()).isEqualTo("PENDING");
        assertThat(scheduled.scheduledAt()).isEqualTo(when);
        assertThat(mockAdapter.outbox()).isEmpty();

        int processed = TenantContext.callAs(teacher.principal(), () ->
                outboundService.processDueScheduledMessages(when.plusSeconds(1)));

        assertThat(processed).isEqualTo(1);
        assertThat(mockAdapter.outbox()).hasSize(1);
        assertThat(mockAdapter.lastMessage().options().scheduledAt()).isEqualTo(when);
        assertThat(mockAdapter.lastMessage().options().hasScheduling()).isTrue();
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
                userId, tenantId, null, Set.of(MemberRole.TEACHER.name()),
                MemberRole.TEACHER, "fa"));
    }

    private UUID insertClassroom(Actor actor) {
        UUID id = UuidV7Generator.generate();
        jdbc.update("""
                insert into classrooms (id, tenant_id, name, status)
                values (?, ?, ?, 'ACTIVE')
                """,
                id, actor.tenantId(), "کلاس " + unique());
        return id;
    }

    private static String unique() {
        return "x" + SEQ.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private record Actor(UUID userId, UUID tenantId, TenantPrincipal principal) {
    }
}
