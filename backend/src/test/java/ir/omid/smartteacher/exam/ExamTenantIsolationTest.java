package ir.omid.smartteacher.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.exam.internal.api.dto.CreateExamQuestionRequest;
import ir.omid.smartteacher.exam.internal.api.dto.CreateExamRequest;
import ir.omid.smartteacher.exam.internal.api.dto.ExamAttemptResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamResponse;
import ir.omid.smartteacher.exam.internal.api.dto.StartExamRequest;
import ir.omid.smartteacher.exam.internal.api.dto.SubmitAnswerRequest;
import ir.omid.smartteacher.exam.internal.application.ExamService;
import ir.omid.smartteacher.exam.internal.domain.AttemptStatus;
import ir.omid.smartteacher.exam.internal.domain.QuestionType;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
 * جداسازی مستأجر + محرمانگی تلاش دانش‌آموز — گام ۱ فاز ۸.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ExamTenantIsolationTest {

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
    private ExamService examService;

    @Test
    @DisplayName("معلم مستأجر ب آزمون مستأجر الف را نمی‌بیند")
    void tenantB_cannotSeeTenantA_exams() {
        Actor a = newTenant(MemberRole.TEACHER);
        Actor b = newTenant(MemberRole.TEACHER);
        UUID classroomA = insertClassroom(a);
        UUID examId = TenantContext.callAs(a.principal(), () ->
                examService.create(new CreateExamRequest(
                        classroomA, "آزمون " + unique(), null, 30, null,
                        null, null, null, null, null)).id());

        assertThat(TenantContext.callAs(b.principal(),
                () -> examService.list(0, 50, null, null, null).content()))
                .noneMatch(e -> e.id().equals(examId));

        assertThatThrownBy(() -> TenantContext.callAs(b.principal(),
                () -> examService.get(examId)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("دانش‌آموز تلاش دانش‌آموز دیگر را نمی‌بیند")
    void student_cannotSeeOtherAttempts() {
        Actor teacher = newTenant(MemberRole.TEACHER);
        UUID classroomId = insertClassroom(teacher);
        UUID studentA = insertStudent(teacher);
        UUID studentB = insertStudent(teacher);

        ExamResponse exam = TenantContext.callAs(teacher.principal(), () -> {
            ExamResponse created = examService.create(new CreateExamRequest(
                    classroomId, "MCQ " + unique(), null, 20, BigDecimal.TEN,
                    null, null, null, null, null));
            examService.addQuestion(created.id(), new CreateExamQuestionRequest(
                    QuestionType.MCQ, "۲+۲؟", "[\"۳\",\"۴\"]", "۴",
                    BigDecimal.TEN, "EASY", null, 0));
            return examService.publish(created.id(), null);
        });

        ExamAttemptResponse attemptA = TenantContext.callAs(teacher.principal(), () ->
                examService.start(exam.id(), new StartExamRequest(studentA)));

        // دانش‌آموز ب تلاش الف را نمی‌بیند
        assertThatThrownBy(() -> TenantContext.callAs(teacher.principal(), () ->
                examService.getAttemptForStudent(attemptA.id(), studentB)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> TenantContext.callAs(teacher.principal(), () ->
                examService.submit(attemptA.id(), studentB)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("ارسال MCQ صحیح، نمره کامل می‌گیرد")
    void submit_autoGradesMcq() {
        Actor teacher = newTenant(MemberRole.TEACHER);
        UUID classroomId = insertClassroom(teacher);
        UUID studentId = insertStudent(teacher);

        var question = TenantContext.callAs(teacher.principal(), () -> {
            ExamResponse created = examService.create(new CreateExamRequest(
                    classroomId, "نمره " + unique(), null, 10, BigDecimal.valueOf(5),
                    null, null, null, null, null));
            var q = examService.addQuestion(created.id(), new CreateExamQuestionRequest(
                    QuestionType.MCQ, "پایتخت ایران؟", "[\"تهران\",\"اصفهان\"]", "تهران",
                    BigDecimal.valueOf(5), null, null, 0));
            examService.publish(created.id(), null);
            return q;
        });

        ExamAttemptResponse graded = TenantContext.callAs(teacher.principal(), () -> {
            ExamAttemptResponse attempt = examService.start(
                    question.examId(), new StartExamRequest(studentId));
            examService.saveAnswer(attempt.id(), studentId,
                    new SubmitAnswerRequest(question.id(), "تهران", null));
            return examService.submit(attempt.id(), studentId);
        });

        assertThat(graded.status()).isEqualTo(AttemptStatus.GRADED);
        assertThat(graded.score()).isEqualByComparingTo("5");
        assertThat(graded.answers()).hasSize(1);
        assertThat(graded.answers().getFirst().correct()).isTrue();
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

    private UUID insertClassroom(Actor actor) {
        UUID id = UuidV7Generator.generate();
        jdbc.update("""
                insert into classrooms (id, tenant_id, name, status)
                values (?, ?, ?, 'ACTIVE')
                """,
                id, actor.tenantId(), "کلاس " + unique());
        return id;
    }

    private UUID insertStudent(Actor actor) {
        UUID id = UuidV7Generator.generate();
        jdbc.update("""
                insert into students (id, tenant_id, full_name, status)
                values (?, ?, ?, 'ACTIVE')
                """,
                id, actor.tenantId(), "دانش‌آموز " + unique());
        return id;
    }

    private static String unique() {
        return "x" + SEQ.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private record Actor(UUID userId, UUID tenantId, TenantPrincipal principal) {
    }
}
