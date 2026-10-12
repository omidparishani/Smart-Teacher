package ir.omid.smartteacher.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.classroom.internal.api.dto.ClassDashboardResponse;
import ir.omid.smartteacher.classroom.internal.api.dto.ClassStudentResponse;
import ir.omid.smartteacher.classroom.internal.api.dto.CreateClassRoomRequest;
import ir.omid.smartteacher.classroom.internal.api.dto.EnrollStudentRequest;
import ir.omid.smartteacher.classroom.internal.application.ClassRoomService;
import ir.omid.smartteacher.classroom.internal.application.EnrollmentService;
import ir.omid.smartteacher.classroom.internal.domain.ClassStudentRepository;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.DuplicateResourceException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.student.internal.api.dto.CreateStudentRequest;
import ir.omid.smartteacher.student.internal.api.dto.StudentResponse;
import ir.omid.smartteacher.student.internal.application.StudentService;
import ir.omid.smartteacher.student.internal.domain.StudentRepository;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * آزمون واقعی دانش‌آموزان و ثبت‌نام روی PostgreSQL — دروازه گام ۲ فاز ۳.
 *
 * <p>چه چیزی اینجا ثابت می‌شود که unit test نمی‌تواند:
 * <ol>
 *   <li>Migration {@code V10} با نگاشت JPA جور است</li>
 *   <li>فیلتر {@code @TenantId} روی {@code students} و {@code class_students}</li>
 *   <li>کلید خارجی ترکیبی جلوی ارجاع بین‌مستأجری را می‌گیرد</li>
 *   <li>ایندکس یکتای کد ملی و ایندکس جست‌وجوی {@code lower(full_name)}</li>
 *   <li>معلم همکار نمی‌تواند دانش‌آموز را در کلاس دیگری ثبت کند</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class StudentEnrollmentIsolationTest {

    static final String POSTGRES_IMAGE =
            System.getProperty("test.postgres.image", "postgres:17-alpine");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));

    private static final AtomicInteger SEQ = new AtomicInteger();

    /** کد ملی معتبر برای تست‌ها — رقم کنترلی‌اش درست است. */
    private static final String VALID_NATIONAL_CODE = "0123456789";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClassRoomService classRoomService;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private ClassStudentRepository classStudentRepository;

    private record Actor(UUID userId, UUID tenantId, TenantPrincipal principal) {
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

    private Actor newMemberOf(UUID tenantId, MemberRole role) {
        UUID userId = UuidV7Generator.generate();
        jdbc.update("insert into users (id, full_name) values (?, ?)",
                userId, "کاربر " + unique());
        return new Actor(userId, tenantId, TenantPrincipal.ofTenantUser(
                userId, tenantId, null, Set.of(role.name()), role, "fa"));
    }

    private static String unique() {
        return "x" + SEQ.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private UUID createStudent(Actor actor, String name, String nationalCode) {
        return TenantContext.callAs(actor.principal(), () -> studentService.create(
                new CreateStudentRequest(name, nationalCode, null, null, null, null))).id();
    }

    private UUID createClass(Actor actor, Integer capacity) {
        return TenantContext.callAs(actor.principal(), () -> classRoomService.create(
                new CreateClassRoomRequest(
                        "کلاس " + unique(), "پنجم", "1404-1405", null, capacity))).id();
    }

    // ────────────────────────── ماندگاری ──────────────────────────

    @Test
    @DisplayName("رکورد دانش‌آموز مُهر مستأجر می‌خورد")
    void studentRowIsStampedWithTenant() {
        Actor owner = newTenant(MemberRole.OWNER);
        UUID studentId = createStudent(owner, "علی رضایی", null);

        var row = jdbc.queryForMap(
                "select tenant_id, full_name, status from students where id = ?",
                studentId);

        assertThat(row.get("tenant_id")).isEqualTo(owner.tenantId());
        assertThat(row.get("full_name")).isEqualTo("علی رضایی");
        assertThat(row.get("status")).isEqualTo("ACTIVE");
    }

    // ─────────────────────── جداسازی مستأجر ───────────────────────

    @Test
    @DisplayName("🔴 دانش‌آموز مستأجر دیگر نه با مخزن دیده می‌شود و نه با سرویس")
    void studentOfAnotherTenantIsInvisible() {
        Actor tenantA = newTenant(MemberRole.OWNER);
        Actor tenantB = newTenant(MemberRole.OWNER);
        UUID studentId = createStudent(tenantA, "سارا", null);

        assertThat(TenantContext.callAs(tenantA.principal(),
                () -> studentRepository.findByIdAndDeletedAtIsNull(studentId)))
                .isPresent();

        assertThat(TenantContext.callAs(tenantB.principal(),
                () -> studentRepository.findByIdAndDeletedAtIsNull(studentId)))
                .isEmpty();

        assertThatThrownBy(() -> TenantContext.callAs(tenantB.principal(),
                () -> studentService.get(studentId)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("دانش‌آموز دارایی مدرسه است — معلم همکار می‌تواند ببیندش")
    void colleagueCanReadStudentInSameTenant() {
        Actor schoolOwner = newTenant(MemberRole.OWNER);
        Actor teacherA = newMemberOf(schoolOwner.tenantId(), MemberRole.TEACHER);
        Actor teacherB = newMemberOf(schoolOwner.tenantId(), MemberRole.TEACHER);

        UUID studentId = createStudent(teacherA, "مینا", null);

        StudentResponse seenByB = TenantContext.callAs(teacherB.principal(),
                () -> studentService.get(studentId));

        assertThat(seenByB.id()).isEqualTo(studentId);
        assertThat(seenByB.fullName()).isEqualTo("مینا");
    }

    // ───────────────────── کلید خارجی ترکیبی ─────────────────────

    @Test
    @DisplayName("🔴 کلید خارجی ترکیبی، ثبت‌نام بین‌مستأجری را می‌گیرد")
    void compositeForeignKeyBlocksCrossTenantEnrollment() {
        Actor tenantA = newTenant(MemberRole.OWNER);
        Actor tenantB = newTenant(MemberRole.OWNER);
        UUID classInA = createClass(tenantA, null);
        UUID studentInB = createStudent(tenantB, "رضا", null);

        assertThatThrownBy(() -> jdbc.update("""
                insert into class_students (id, tenant_id, classroom_id, student_id)
                values (?, ?, ?, ?)
                """,
                UuidV7Generator.generate(), tenantA.tenantId(), classInA, studentInB))
                .as("دیتابیس باید ارجاع دانش‌آموز مستأجر دیگر را رد کند")
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ──────────────────── ثبت‌نام و دسترسی ────────────────────

    @Test
    @DisplayName("🔴 معلم همکار نمی‌تواند در کلاس دیگری ثبت‌نام کند")
    void teacherCannotEnrollIntoColleaguesClass() {
        Actor schoolOwner = newTenant(MemberRole.OWNER);
        Actor teacherA = newMemberOf(schoolOwner.tenantId(), MemberRole.TEACHER);
        Actor teacherB = newMemberOf(schoolOwner.tenantId(), MemberRole.TEACHER);

        UUID classOfA = createClass(teacherA, null);
        UUID studentId = createStudent(teacherA, "نیما", null);

        assertThatThrownBy(() -> TenantContext.callAs(teacherB.principal(),
                () -> enrollmentService.enroll(classOfA,
                        new EnrollStudentRequest(studentId, null))))
                .as("کلاس همکار باید ناموجود دیده شود")
                .isInstanceOf(ResourceNotFoundException.class);

        Long denials = jdbc.queryForObject("""
                select count(*) from audit_logs
                where action = 'SECURITY_ACCESS_DENIED' and entity_id = ?
                """, Long.class, classOfA);

        assertThat(denials).isEqualTo(1L);
    }

    @Test
    @DisplayName("ثبت‌نام موفق و داشبورد شمار درست می‌دهد")
    void enrollAndDashboardReflectCount() {
        Actor owner = newTenant(MemberRole.OWNER);
        UUID classId = createClass(owner, 30);
        UUID studentId = createStudent(owner, "زهرا", VALID_NATIONAL_CODE);

        ClassStudentResponse enrolled = TenantContext.callAs(owner.principal(),
                () -> enrollmentService.enroll(classId,
                        new EnrollStudentRequest(studentId, "۷")));

        assertThat(enrolled.fullName()).isEqualTo("زهرا");
        assertThat(enrolled.rollNumber()).isEqualTo("۷");
        assertThat(enrolled.nationalCode()).isEqualTo(VALID_NATIONAL_CODE);

        ClassDashboardResponse dashboard = TenantContext.callAs(owner.principal(),
                () -> enrollmentService.dashboard(classId));

        assertThat(dashboard.enrolledStudentCount()).isEqualTo(1);
        assertThat(dashboard.capacity()).isEqualTo(30);
        assertThat(dashboard.seatsRemaining()).isEqualTo(29);

        PageResponse<ClassStudentResponse> list = TenantContext.callAs(owner.principal(),
                () -> enrollmentService.listStudents(classId, 0, 20));
        assertThat(list.totalElements()).isEqualTo(1);
        assertThat(list.content()).extracting(ClassStudentResponse::studentId)
                .containsExactly(studentId);
    }

    @Test
    @DisplayName("ظرفیت پر روی دیتابیس واقعی رد می‌شود")
    void capacityIsEnforced() {
        Actor owner = newTenant(MemberRole.OWNER);
        UUID classId = createClass(owner, 1);
        UUID s1 = createStudent(owner, "یک", null);
        UUID s2 = createStudent(owner, "دو", null);

        TenantContext.callAs(owner.principal(),
                () -> enrollmentService.enroll(classId, new EnrollStudentRequest(s1, null)));

        assertThatThrownBy(() -> TenantContext.callAs(owner.principal(),
                () -> enrollmentService.enroll(classId, new EnrollStudentRequest(s2, null))))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_CONFLICT);
    }

    @Test
    @DisplayName("ثبت‌نام تکراری رد می‌شود")
    void duplicateEnrollmentIsRejected() {
        Actor owner = newTenant(MemberRole.OWNER);
        UUID classId = createClass(owner, null);
        UUID studentId = createStudent(owner, "تکراری", null);

        TenantContext.callAs(owner.principal(),
                () -> enrollmentService.enroll(classId, new EnrollStudentRequest(studentId, null)));

        assertThatThrownBy(() -> TenantContext.callAs(owner.principal(),
                () -> enrollmentService.enroll(classId, new EnrollStudentRequest(studentId, null))))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("دانش‌آموز حذف‌نرم‌شده از فهرست کلاس غایب می‌شود و صندلی آزاد می‌کند")
    void softDeletedStudentDisappearsFromClassListAndFreesCapacity() {
        Actor owner = newTenant(MemberRole.OWNER);
        UUID classId = createClass(owner, 1);
        UUID studentId = createStudent(owner, "حذف‌شونده", null);

        TenantContext.callAs(owner.principal(),
                () -> enrollmentService.enroll(classId, new EnrollStudentRequest(studentId, null)));

        TenantContext.runAs(owner.principal(), () -> studentService.delete(studentId));

        // ردیف ثبت‌نام هنوز هست
        assertThat(TenantContext.callAs(owner.principal(),
                () -> classStudentRepository.findByClassroomIdAndStudentId(classId, studentId)))
                .isPresent();

        // ولی در فهرست و داشبورد نیست
        PageResponse<ClassStudentResponse> list = TenantContext.callAs(owner.principal(),
                () -> enrollmentService.listStudents(classId, 0, 20));
        assertThat(list.totalElements()).isZero();

        ClassDashboardResponse dashboard = TenantContext.callAs(owner.principal(),
                () -> enrollmentService.dashboard(classId));
        assertThat(dashboard.enrolledStudentCount()).isZero();
        assertThat(dashboard.seatsRemaining()).isEqualTo(1);

        // و ظرفیت آزاد شده — دانش‌آموز تازه پذیرفته می‌شود
        UUID replacement = createStudent(owner, "جایگزین", null);
        TenantContext.callAs(owner.principal(),
                () -> enrollmentService.enroll(classId, new EnrollStudentRequest(replacement, null)));

        assertThat(TenantContext.callAs(owner.principal(),
                () -> enrollmentService.dashboard(classId)).enrolledStudentCount())
                .isEqualTo(1);
    }

    // ──────────────────── ایندکس و یکتایی ────────────────────

    @Test
    @DisplayName("کد ملی داخل مستأجر یکتاست و بین مستأجرها آزاد")
    void nationalCodeIsUniquePerTenant() {
        Actor tenantA = newTenant(MemberRole.OWNER);
        Actor tenantB = newTenant(MemberRole.OWNER);

        createStudent(tenantA, "الف", VALID_NATIONAL_CODE);

        assertThatThrownBy(() -> createStudent(tenantA, "ب", VALID_NATIONAL_CODE))
                .isInstanceOf(DuplicateResourceException.class);

        // مستأجر دیگر همان کد را می‌تواند داشته باشد
        assertThat(createStudent(tenantB, "ج", VALID_NATIONAL_CODE)).isNotNull();
    }

    @Test
    @DisplayName("ایندکس جست‌وجوی نام روی lower(full_name) است")
    void studentNameSearchIndexUsesLower() {
        String definition = jdbc.queryForObject("""
                select indexdef from pg_indexes
                where tablename = 'students' and indexname = 'idx_students_name_trgm'
                """, String.class);

        assertThat(definition)
                .as("درس V9: ایندکس باید روی lower(full_name) باشد")
                .contains("lower(");
    }

    @Test
    @DisplayName("قید uk_classrooms_tenant_id برای کلید ترکیبی ثبت‌نام وجود دارد")
    void classroomsTenantIdUniqueExistsForCompositeFk() {
        Long count = jdbc.queryForObject("""
                select count(*) from pg_constraint
                where conname = 'uk_classrooms_tenant_id'
                """, Long.class);

        assertThat(count)
                .as("V10 باید uk_classrooms_tenant_id را روی classrooms اضافه کرده باشد")
                .isEqualTo(1L);
    }
}
