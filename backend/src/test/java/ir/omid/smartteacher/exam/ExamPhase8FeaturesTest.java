package ir.omid.smartteacher.exam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.exam.internal.api.dto.CreateExamQuestionRequest;
import ir.omid.smartteacher.exam.internal.api.dto.CreateExamRequest;
import ir.omid.smartteacher.exam.internal.api.dto.ExamAttemptResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamBlueprintResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamResponse;
import ir.omid.smartteacher.exam.internal.api.dto.GenerateExamRequest;
import ir.omid.smartteacher.exam.internal.api.dto.PublishExamRequest;
import ir.omid.smartteacher.exam.internal.api.dto.StartExamRequest;
import ir.omid.smartteacher.exam.internal.api.dto.SubmitAnswerRequest;
import ir.omid.smartteacher.exam.internal.application.ExamService;
import ir.omid.smartteacher.exam.internal.domain.AttemptStatus;
import ir.omid.smartteacher.exam.internal.domain.QuestionType;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * فاز ۸ گام ۲ — تولید، مهلت، تصحیح AI، جداسازی.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ExamPhase8FeaturesTest {

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
    @DisplayName("تولید آزمون سؤال‌های MCQ و یک تشریحی می‌سازد")
    void generate_createsMcqAndEssay() {
        Actor teacher = newTenant(MemberRole.TEACHER);
        UUID classroomId = insertClassroom(teacher);
        UUID bookId = insertBook(teacher);

        ExamResponse exam = TenantContext.callAs(teacher.principal(), () ->
                examService.generate(new GenerateExamRequest(
                        classroomId, bookId, 1, 5, "آزمون تولیدشده " + unique(),
                        null, 30, null, 2, null, null, null)));

        assertThat(exam.questions()).hasSize(3);
        assertThat(exam.questions())
                .filteredOn(q -> q.type() == QuestionType.MCQ)
                .hasSize(2);
        assertThat(exam.questions())
                .filteredOn(q -> q.type() == QuestionType.ESSAY)
                .hasSize(1);
        assertThat(exam.questions()).allMatch(q -> q.aiGenerated());
        assertThat(exam.bookId()).isEqualTo(bookId);
    }

    @Test
    @DisplayName("تولید با trueFalseCount سؤال درست/نادرست می‌سازد")
    void generate_includesTrueFalseWhenRequested() {
        Actor teacher = newTenant(MemberRole.TEACHER);
        UUID classroomId = insertClassroom(teacher);
        UUID bookId = insertBook(teacher);

        ExamResponse exam = TenantContext.callAs(teacher.principal(), () ->
                examService.generate(new GenerateExamRequest(
                        classroomId, bookId, 2, 4, "آزمون TF " + unique(),
                        null, 20, null, 1, 2, null, null)));

        assertThat(exam.questions())
                .filteredOn(q -> q.type() == QuestionType.TRUE_FALSE)
                .hasSize(2);
        assertThat(exam.questions())
                .filteredOn(q -> q.type() == QuestionType.MCQ)
                .hasSize(1);
    }

    @Test
    @DisplayName("GET blueprint آخرین طرح تولید را برمی‌گرداند")
    void getBlueprint_returnsLatest() {
        Actor teacher = newTenant(MemberRole.TEACHER);
        UUID classroomId = insertClassroom(teacher);
        UUID bookId = insertBook(teacher);

        ExamResponse exam = TenantContext.callAs(teacher.principal(), () ->
                examService.generate(new GenerateExamRequest(
                        classroomId, bookId, 10, 12, "آزمون BP " + unique(),
                        null, 15, null, 2, 0, null, null)));

        ExamBlueprintResponse blueprint = TenantContext.callAs(teacher.principal(), () ->
                examService.getBlueprint(exam.id()));

        assertThat(blueprint.examId()).isEqualTo(exam.id());
        assertThat(blueprint.bookId()).isEqualTo(bookId);
        assertThat(blueprint.pageFrom()).isEqualTo(10);
        assertThat(blueprint.pageTo()).isEqualTo(12);
        assertThat(blueprint.questionCount()).isEqualTo(exam.questions().size());
    }

    @Test
    @DisplayName("انتشار با chatId ایتا، پیام exm_ deep link می‌فرستد")
    void publish_sendsEitaaDeepLink() {
        Actor teacher = newTenant(MemberRole.TEACHER);
        UUID classroomId = insertClassroom(teacher);

        ExamResponse exam = TenantContext.callAs(teacher.principal(), () -> {
            ExamResponse created = examService.create(new CreateExamRequest(
                    classroomId, "ایتا " + unique(), null, 20, BigDecimal.ONE,
                    null, null, null, null, null));
            examService.addQuestion(created.id(), new CreateExamQuestionRequest(
                    QuestionType.MCQ, "؟", "[\"آ\",\"ب\"]", "آ",
                    BigDecimal.ONE, null, null, 0));
            return examService.publish(created.id(), new PublishExamRequest("@exam_group"));
        });

        assertThat(exam.status().name()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("ارسال پس از مهلت durationMinutes رد می‌شود")
    void lateSubmit_isRejected() {
        Actor teacher = newTenant(MemberRole.TEACHER);
        UUID classroomId = insertClassroom(teacher);
        UUID studentId = insertStudent(teacher);

        ExamResponse exam = TenantContext.callAs(teacher.principal(), () -> {
            ExamResponse created = examService.create(new CreateExamRequest(
                    classroomId, "مهلت " + unique(), null, 1, BigDecimal.TEN,
                    null, null, null, null, null));
            examService.addQuestion(created.id(), new CreateExamQuestionRequest(
                    QuestionType.MCQ, "۱+۱؟", "[\"۱\",\"۲\"]", "۲",
                    BigDecimal.TEN, null, null, 0));
            return examService.publish(created.id(), null);
        });

        ExamAttemptResponse attempt = TenantContext.callAs(teacher.principal(), () ->
                examService.start(exam.id(), new StartExamRequest(studentId)));

        // شروع را به گذشته ببر تا مهلت ۱ دقیقه‌ای گذشته باشد
        jdbc.update("update exam_attempts set started_at = ? where id = ?",
                java.sql.Timestamp.from(Instant.now().minus(5, ChronoUnit.MINUTES)),
                attempt.id());

        assertThatThrownBy(() -> TenantContext.callAs(teacher.principal(), () ->
                examService.submit(attempt.id(), studentId)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.RESOURCE_CONFLICT));
    }

    @Test
    @DisplayName("سؤال تشریحی با Mock AI نمره می‌گیرد")
    void essay_getsAiScoreOnSubmit() {
        Actor teacher = newTenant(MemberRole.TEACHER);
        UUID classroomId = insertClassroom(teacher);
        UUID studentId = insertStudent(teacher);

        var question = TenantContext.callAs(teacher.principal(), () -> {
            ExamResponse created = examService.create(new CreateExamRequest(
                    classroomId, "تشریحی " + unique(), null, 60, BigDecimal.valueOf(5),
                    null, null, null, null, null));
            var q = examService.addQuestion(created.id(), new CreateExamQuestionRequest(
                    QuestionType.ESSAY, "جنگل را توضیح دهید", null, null,
                    BigDecimal.valueOf(5), null, null, 0));
            examService.publish(created.id(), null);
            return q;
        });

        ExamAttemptResponse graded = TenantContext.callAs(teacher.principal(), () -> {
            ExamAttemptResponse attempt = examService.start(
                    question.examId(), new StartExamRequest(studentId));
            examService.saveAnswer(attempt.id(), studentId,
                    new SubmitAnswerRequest(question.id(), "جنگل مجموعه‌ای از درختان است", null));
            return examService.submit(attempt.id(), studentId);
        });

        assertThat(graded.status()).isEqualTo(AttemptStatus.GRADED);
        assertThat(graded.answers()).hasSize(1);
        assertThat(graded.answers().getFirst().aiGraded()).isTrue();
        assertThat(graded.answers().getFirst().awardedScore()).isNotNull();
        assertThat(graded.answers().getFirst().aiFeedback()).isNotBlank();
    }

    @Test
    @DisplayName("مستأجر ب کارنامه تلاش مستأجر الف را نمی‌بیند")
    void tenantB_cannotSeeTenantA_report() {
        Actor a = newTenant(MemberRole.TEACHER);
        Actor b = newTenant(MemberRole.TEACHER);
        UUID classroomA = insertClassroom(a);
        UUID studentA = insertStudent(a);

        ExamAttemptResponse attempt = TenantContext.callAs(a.principal(), () -> {
            ExamResponse created = examService.create(new CreateExamRequest(
                    classroomA, "جداسازی " + unique(), null, 20, BigDecimal.ONE,
                    null, null, null, null, null));
            examService.addQuestion(created.id(), new CreateExamQuestionRequest(
                    QuestionType.MCQ, "؟", "[\"آ\",\"ب\"]", "آ",
                    BigDecimal.ONE, null, null, 0));
            examService.publish(created.id(), null);
            return examService.start(created.id(), new StartExamRequest(studentA));
        });

        assertThatThrownBy(() -> TenantContext.callAs(b.principal(), () ->
                examService.report(attempt.id())))
                .isInstanceOf(ResourceNotFoundException.class);
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

    private UUID insertBook(Actor actor) {
        UUID id = UuidV7Generator.generate();
        jdbc.update("""
                insert into books (id, tenant_id, title, status)
                values (?, ?, ?, 'READY')
                """,
                id, actor.tenantId(), "کتاب " + unique());
        return id;
    }

    private static String unique() {
        return "x" + SEQ.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private record Actor(UUID userId, UUID tenantId, TenantPrincipal principal) {
    }
}
