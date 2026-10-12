package ir.omid.smartteacher.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.assignment.internal.api.dto.AssignmentResponse;
import ir.omid.smartteacher.assignment.internal.api.dto.CreateAssignmentRequest;
import ir.omid.smartteacher.assignment.internal.api.dto.CreateSubmissionRequest;
import ir.omid.smartteacher.assignment.internal.api.dto.PublishAssignmentRequest;
import ir.omid.smartteacher.assignment.internal.api.dto.SubmissionResponse;
import ir.omid.smartteacher.assignment.internal.application.AssignmentService;
import ir.omid.smartteacher.assignment.internal.domain.AssignmentStatus;
import ir.omid.smartteacher.assignment.internal.domain.EitaaSendStatus;
import ir.omid.smartteacher.classroom.internal.api.dto.CreateClassRoomRequest;
import ir.omid.smartteacher.classroom.internal.application.ClassRoomService;
import ir.omid.smartteacher.classroom.internal.application.EnrollmentService;
import ir.omid.smartteacher.classroom.internal.api.dto.EnrollStudentRequest;
import ir.omid.smartteacher.billing.internal.api.dto.CheckoutRequest;
import ir.omid.smartteacher.billing.internal.application.BillingService;
import ir.omid.smartteacher.billing.internal.domain.Plan;
import ir.omid.smartteacher.billing.internal.domain.PlanRepository;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.eitaa.internal.MockEitaaAdapter;
import ir.omid.smartteacher.student.internal.api.dto.CreateStudentRequest;
import ir.omid.smartteacher.student.internal.application.StudentService;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.math.BigDecimal;
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
 * جداسازی مستأجر + حریم ارسال دانش‌آموز + انتشار بدون ایتای واقعی.
 *
 * <pre>{@code
 * mvn test -Dtest=AssignmentLifecycleTest -Dtest.postgres.image=postgres:17-alpine
 * }</pre>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class AssignmentLifecycleTest {

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
    private AssignmentService assignmentService;

    @Autowired
    private ClassRoomService classRoomService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private MockEitaaAdapter mockEitaa;

    @Autowired
    private BillingService billingService;

    @Autowired
    private PlanRepository planRepository;

    @BeforeEach
    void clearMockOutbox() {
        mockEitaa.clearOutbox();
        mockEitaa.resetFailures();
    }

    private Actor newTenant(MemberRole role) {
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
                userId, tenantId, null, Set.of(role.name()), role, "fa"));
    }

    private Actor studentInSameTenant(Actor teacher) {
        UUID userId = UuidV7Generator.generate();
        jdbc.update("insert into users (id, full_name) values (?, ?)",
                userId, "دانش‌آموز " + unique());
        return new Actor(userId, teacher.tenantId(), TenantPrincipal.ofTenantUser(
                userId, teacher.tenantId(), null, Set.of(), MemberRole.STUDENT, "fa"));
    }

    private static String unique() {
        return "x" + SEQ.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private UUID createClass(Actor teacher) {
        return TenantContext.callAs(teacher.principal(), () -> classRoomService.create(
                new CreateClassRoomRequest("کلاس " + unique(), "پنجم", "1404-1405", null, 30)
        )).id();
    }

    private UUID createStudent(Actor teacher) {
        return TenantContext.callAs(teacher.principal(), () -> studentService.create(
                new CreateStudentRequest("دانش " + unique(), null, null, null, null, null)
        )).id();
    }

    private void enroll(Actor teacher, UUID classId, UUID studentId) {
        TenantContext.callAs(teacher.principal(), () -> {
            enrollmentService.enroll(classId, new EnrollStudentRequest(studentId, null));
            return null;
        });
    }

    private UUID createAssignment(Actor teacher, UUID classId) {
        return TenantContext.callAs(teacher.principal(), () -> assignmentService.create(
                new CreateAssignmentRequest(
                        classId, "تمرین " + unique(), "توضیح", null,
                        BigDecimal.TEN, null, null, null, null, false)
        )).id();
    }

    private void activateFreeSubscription(Actor teacher) {
        Plan free = planRepository.findByCode("FREE").orElseThrow();
        TenantContext.callAs(teacher.principal(), () -> {
            billingService.checkout(new CheckoutRequest(free.getId()));
            return null;
        });
    }

    @Test
    @DisplayName("معلم مستأجر ب تکلیف مستأجر الف را نمی‌بیند")
    void tenantB_cannotSeeTenantA_assignments() {
        Actor a = newTenant(MemberRole.TEACHER);
        Actor b = newTenant(MemberRole.TEACHER);

        UUID classId = createClass(a);
        UUID assignmentId = createAssignment(a, classId);

        assertThat(TenantContext.callAs(b.principal(),
                () -> assignmentService.list(0, 50, null, null, null, null).content()))
                .noneMatch(item -> item.id().equals(assignmentId));

        assertThatThrownBy(() -> TenantContext.callAs(b.principal(),
                () -> assignmentService.get(assignmentId)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("دانش‌آموز ارسال دانش‌آموز دیگر را نمی‌بیند")
    void student_cannotSeeOtherStudentsSubmission() {
        Actor teacher = newTenant(MemberRole.TEACHER);
        Actor studentA = studentInSameTenant(teacher);
        Actor studentB = studentInSameTenant(teacher);

        UUID classId = createClass(teacher);
        UUID studentEntityA = createStudent(teacher);
        UUID studentEntityB = createStudent(teacher);
        enroll(teacher, classId, studentEntityA);
        enroll(teacher, classId, studentEntityB);

        UUID assignmentId = createAssignment(teacher, classId);
        TenantContext.callAs(teacher.principal(), () -> assignmentService.publish(
                assignmentId, new PublishAssignmentRequest(null)));

        SubmissionResponse subA = TenantContext.callAs(studentA.principal(), () ->
                assignmentService.submit(assignmentId,
                        new CreateSubmissionRequest(studentEntityA, "پاسخ الف")));

        TenantContext.callAs(studentB.principal(), () ->
                assignmentService.submit(assignmentId,
                        new CreateSubmissionRequest(studentEntityB, "پاسخ ب")));

        assertThatThrownBy(() -> TenantContext.callAs(studentB.principal(),
                () -> assignmentService.getSubmission(subA.id())))
                .as("دانش‌آموز ب نباید ارسال الف را ببیند")
                .isInstanceOf(ResourceNotFoundException.class);

        SubmissionResponse own = TenantContext.callAs(studentA.principal(),
                () -> assignmentService.getSubmission(subA.id()));
        assertThat(own.answerText()).isEqualTo("پاسخ الف");
    }

    @Test
    @DisplayName("انتشار با Mock ایتا — شکست ارسال، وضعیت PUBLISHED را برنمی‌گرداند")
    void publish_withMockEitaa_failureDoesNotRollback() {
        Actor teacher = newTenant(MemberRole.TEACHER);
        UUID classId = createClass(teacher);
        UUID assignmentId = createAssignment(teacher, classId);

        mockEitaa.failNextSends(1, "403", "blocked");

        AssignmentResponse published = TenantContext.callAs(teacher.principal(), () ->
                assignmentService.publish(assignmentId,
                        new PublishAssignmentRequest("@teaching_group")));

        assertThat(published.status()).isEqualTo(AssignmentStatus.PUBLISHED);
        assertThat(published.eitaaMessageStatus()).isEqualTo(EitaaSendStatus.FAILED);

        AssignmentResponse reloaded = TenantContext.callAs(teacher.principal(),
                () -> assignmentService.get(assignmentId));
        assertThat(reloaded.status()).isEqualTo(AssignmentStatus.PUBLISHED);
        assertThat(reloaded.eitaaMessageStatus()).isEqualTo(EitaaSendStatus.FAILED);
    }

    @Test
    @DisplayName("انتشار موفق با Mock ایتا پیام را در outbox می‌گذارد")
    void publish_withMockEitaa_successRecordsOutbox() {
        Actor teacher = newTenant(MemberRole.TEACHER);
        UUID classId = createClass(teacher);
        UUID assignmentId = createAssignment(teacher, classId);

        AssignmentResponse published = TenantContext.callAs(teacher.principal(), () ->
                assignmentService.publish(assignmentId,
                        new PublishAssignmentRequest("@class_group")));

        assertThat(published.status()).isEqualTo(AssignmentStatus.PUBLISHED);
        assertThat(published.eitaaMessageStatus()).isEqualTo(EitaaSendStatus.SENT);
        assertThat(mockEitaa.outbox()).hasSize(1);
        assertThat(mockEitaa.lastMessage().target()).isEqualTo("@class_group");
        assertThat(mockEitaa.lastMessage().text()).contains("asg_");
    }

    @Test
    @DisplayName("حذف نرم تکلیف را از get پنهان می‌کند")
    void softDelete_hidesAssignment() {
        Actor teacher = newTenant(MemberRole.TEACHER);
        UUID classId = createClass(teacher);
        UUID assignmentId = createAssignment(teacher, classId);

        TenantContext.callAs(teacher.principal(), () -> {
            assignmentService.delete(assignmentId);
            return null;
        });

        assertThatThrownBy(() -> TenantContext.callAs(teacher.principal(),
                () -> assignmentService.get(assignmentId)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("تصحیح AI ردیف ai_evaluations می‌سازد")
    void evaluate_createsAiEvaluationRow() {
        Actor teacher = newTenant(MemberRole.TEACHER);
        activateFreeSubscription(teacher);
        UUID classId = createClass(teacher);
        UUID studentId = createStudent(teacher);
        enroll(teacher, classId, studentId);
        UUID assignmentId = createAssignment(teacher, classId);

        TenantContext.callAs(teacher.principal(), () -> assignmentService.publish(
                assignmentId, new PublishAssignmentRequest(null)));

        SubmissionResponse submission = TenantContext.callAs(teacher.principal(), () ->
                assignmentService.submit(assignmentId,
                        new CreateSubmissionRequest(studentId, "پاسخ تستی برای تصحیح")));

        var evaluation = TenantContext.callAs(teacher.principal(), () ->
                assignmentService.evaluate(assignmentId, submission.id()));

        assertThat(evaluation.submissionId()).isEqualTo(submission.id());
        assertThat(evaluation.aiScore()).isNotNull();
        assertThat(evaluation.feedback()).isNotBlank();

        Integer count = jdbc.queryForObject(
                "select count(*) from ai_evaluations where submission_id = ?",
                Integer.class, submission.id());
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("بازبینی معلم Append-Only است")
    void review_isAppendOnly() {
        Actor teacher = newTenant(MemberRole.TEACHER);
        activateFreeSubscription(teacher);
        UUID classId = createClass(teacher);
        UUID studentId = createStudent(teacher);
        enroll(teacher, classId, studentId);
        UUID assignmentId = createAssignment(teacher, classId);

        TenantContext.callAs(teacher.principal(), () -> assignmentService.publish(
                assignmentId, new PublishAssignmentRequest(null)));

        SubmissionResponse submission = TenantContext.callAs(teacher.principal(), () ->
                assignmentService.submit(assignmentId,
                        new CreateSubmissionRequest(studentId, "پاسخ")));

        TenantContext.callAs(teacher.principal(), () ->
                assignmentService.evaluate(assignmentId, submission.id()));

        var first = TenantContext.callAs(teacher.principal(), () ->
                assignmentService.review(assignmentId, submission.id(),
                        new ir.omid.smartteacher.assignment.internal.api.dto.ReviewSubmissionRequest(
                                ir.omid.smartteacher.assignment.internal.domain.ReviewAction.EDIT_SCORE,
                                new BigDecimal("9.5"),
                                "اول")));

        var second = TenantContext.callAs(teacher.principal(), () ->
                assignmentService.review(assignmentId, submission.id(),
                        new ir.omid.smartteacher.assignment.internal.api.dto.ReviewSubmissionRequest(
                                ir.omid.smartteacher.assignment.internal.domain.ReviewAction.ACCEPT,
                                new BigDecimal("9.5"),
                                "دوم")));

        assertThat(first.id()).isNotEqualTo(second.id());

        var reviews = TenantContext.callAs(teacher.principal(), () ->
                assignmentService.listReviews(assignmentId, submission.id()));
        assertThat(reviews).hasSize(2);
        assertThat(reviews).extracting(r -> r.comment()).containsExactly("اول", "دوم");
    }

    private record Actor(UUID userId, UUID tenantId, TenantPrincipal principal) {
    }
}
