package ir.omid.smartteacher.classroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.classroom.internal.api.dto.ClassRoomResponse;
import ir.omid.smartteacher.classroom.internal.api.dto.CreateClassRoomRequest;
import ir.omid.smartteacher.classroom.internal.api.dto.CreateSubjectRequest;
import ir.omid.smartteacher.classroom.internal.api.dto.UpdateClassRoomRequest;
import ir.omid.smartteacher.classroom.internal.application.ClassRoomService;
import ir.omid.smartteacher.classroom.internal.application.SubjectService;
import ir.omid.smartteacher.classroom.internal.domain.ClassRoomRepository;
import ir.omid.smartteacher.classroom.internal.domain.ClassRoomStatus;
import ir.omid.smartteacher.classroom.internal.domain.SubjectRepository;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.DuplicateResourceException;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.util.Locale;
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
 * آزمون واقعی کلاس‌ها و درس‌ها روی PostgreSQL — قید پذیرش فاز ۳.
 *
 * <p><b>این تست چه چیزی را ثابت می‌کند که unit test نمی‌تواند؟</b>
 * چهار چیز، و هر چهار تا فقط با دیتابیس واقعی سنجیدنی‌اند:
 * <ol>
 *   <li><b>Migration و نگاشت با هم می‌خوانند.</b> اگر {@code V8} ستونی
 *       را نساخته باشد که موجودیت انتظار دارد (یا برعکس)، بالا آمدن
 *       کانتکست با {@code ddl-auto: validate} می‌شکند — و همین اولین
 *       چیزی است که در این تست لو می‌رود.</li>
 *   <li><b>فیلتر خودکار {@code @TenantId} روی جدول‌های تازه کار
 *       می‌کند.</b> اینکه روی {@code isolation_probe} کار می‌کند، درباره
 *       {@code classrooms} چیزی نمی‌گوید.</li>
 *   <li><b>کلید خارجی ترکیبی واقعاً جلوی ارجاع بین‌مستأجری را
 *       می‌گیرد.</b> این یک قید دیتابیس است؛ هیچ تستی در لایه جاوا
 *       نمی‌تواند وجودش را ثابت کند.</li>
 *   <li><b>ایندکس‌های یکتا رفتار درست دارند</b> — یکتایی کد درس داخل
 *       مستأجر، با حساسیت‌نداشتن به بزرگی حروف.</li>
 * </ol>
 *
 * <p><b>⚠️ چرا هر تست مستأجر تازه خودش را می‌سازد؟</b> چون این کلاس
 * هیچ چیزی را بین تست‌ها پاک نمی‌کند (rollback ممکن نیست — با یک
 * Session واحد، شناسه مستأجر یک بار خوانده می‌شود و تست بی‌معنا
 * می‌شود). اگر تست‌ها مستأجر مشترک داشتند، {@code totalElements} هر
 * تست به ترتیب اجرای بقیه وابسته می‌شد و تست‌ها شکننده می‌گشتند.
 *
 * <p>⚠️ تصویر پیش‌فرض PostgreSQL 17 است — همان نسخه Production. اگر
 * Docker Hub در دسترس نبود:
 * <pre>{@code
 * mvn test -Dtest=ClassRoomTenantIsolationTest -Dtest.postgres.image=postgres:17-alpine
 * }</pre>
 * و آن اجرا باید در {@code docs/verification-log.md} با همان نسخه ثبت
 * شود (توضیح کامل در {@code TenantIsolationTest}).
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ClassRoomTenantIsolationTest {

    static final String POSTGRES_IMAGE =
            System.getProperty("test.postgres.image", "postgres:17-alpine");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));

    /** شمارنده یکتاسازی نام‌ها — ایندکس‌های یکتا نباید بین تست‌ها برخورد کنند. */
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private ClassRoomService classRoomService;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ClassRoomRepository classRoomRepository;

    // ─────────────────────────── کمکی ───────────────────────────

    /** یک کاربر و مستأجر تازه با نقش مالک. */
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

    /** عضو تازه در مستأجر موجود — برای ساختن «معلم همکار». */
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

    private UUID createSubject(Actor actor, String code) {
        return TenantContext.callAs(actor.principal(), () -> subjectService.create(
                new CreateSubjectRequest("ریاضی " + unique(), code, "پنجم"))).id();
    }

    private UUID createClass(Actor actor, UUID subjectId) {
        return TenantContext.callAs(actor.principal(), () -> classRoomService.create(
                new CreateClassRoomRequest(
                        "کلاس " + unique(), "پنجم", "1404-1405", subjectId, 30))).id();
    }

    private PageResponse<ClassRoomResponse> listFor(Actor actor) {
        return TenantContext.callAs(actor.principal(),
                () -> classRoomService.list(0, 100, null, null, null));
    }

    /** یک عامل (کاربر + مستأجر + هویت) — فقط برای خوانا ماندن تست‌ها. */
    private record Actor(UUID userId, UUID tenantId, TenantPrincipal principal) {
    }

    // ────────────────────────── ماندگاری ──────────────────────────

    @Test
    @DisplayName("رکورد کلاس، مُهر مستأجر و معلم جاری را می‌خورد")
    void classRoomRowIsStampedWithTenantAndCurrentTeacher() {
        Actor owner = newTenant(MemberRole.OWNER);

        UUID classId = createClass(owner, null);

        // 🔴 کوئری خام، نه مخزن: اگر از مخزن می‌خواندیم، فیلتر مستأجر
        // مقدار واقعی ستون را پنهان می‌کرد و تست فقط می‌گفت «چیزی
        // برگشت» — نه اینکه ستون چه دارد.
        var row = jdbc.queryForMap(
                "select tenant_id, homeroom_teacher_id, status from classrooms where id = ?",
                classId);

        assertThat(row.get("tenant_id")).isEqualTo(owner.tenantId());
        assertThat(row.get("homeroom_teacher_id"))
                .as("معلم سرکلاس باید همان کاربر جاری باشد")
                .isEqualTo(owner.userId());
        assertThat(row.get("status")).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("کلاس، نام درسش را در پاسخ می‌آورد")
    void classRoomResponseCarriesSubjectName() {
        Actor owner = newTenant(MemberRole.OWNER);
        UUID subjectId = createSubject(owner, "MATH-" + unique());

        UUID classId = createClass(owner, subjectId);

        ClassRoomResponse response = TenantContext.callAs(owner.principal(),
                () -> classRoomService.get(classId));

        assertThat(response.subjectId()).isEqualTo(subjectId);
        assertThat(response.subjectName())
                .as("نام درس باید در یک کوئری جدا خوانده و پر شود")
                .startsWith("ریاضی ");
    }

    // ─────────────────────── جداسازی مستأجر ───────────────────────

    @Test
    @DisplayName("🔴 کلاس مستأجر دیگر نه با مخزن دیده می‌شود و نه با سرویس")
    void classRoomOfAnotherTenantIsInvisible() {
        Actor tenantA = newTenant(MemberRole.OWNER);
        Actor tenantB = newTenant(MemberRole.OWNER);
        UUID classId = createClass(tenantA, null);

        assertThat(TenantContext.callAs(tenantA.principal(),
                () -> classRoomRepository.findByIdAndDeletedAtIsNull(classId)))
                .as("صاحب رکورد باید آن را ببیند")
                .isPresent();

        assertThat(TenantContext.callAs(tenantB.principal(),
                () -> classRoomRepository.findByIdAndDeletedAtIsNull(classId)))
                .as("مستأجر دیگر باید همان شناسه را ناموجود ببیند")
                .isEmpty();

        assertThatThrownBy(() -> TenantContext.callAs(tenantB.principal(),
                () -> classRoomService.get(classId)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("🔴 معلم، کلاس معلم همکار در همان مدرسه را نمی‌بیند")
    void teacherCannotReadColleaguesClass() {
        // ⚠️ این همان قید پذیرش فاز ۳ است، در سطح کلاس: دو معلم در
        // *یک* مستأجر. فیلتر @TenantId اینجا هیچ کمکی نمی‌کند، چون
        // هر دو در یک مستأجرند — تمام کار روی قاعده مالکیت سرویس است.
        Actor schoolOwner = newTenant(MemberRole.OWNER);
        Actor teacherA = newMemberOf(schoolOwner.tenantId(), MemberRole.TEACHER);
        Actor teacherB = newMemberOf(schoolOwner.tenantId(), MemberRole.TEACHER);

        UUID classOfA = createClass(teacherA, null);

        // معلم الف کلاس خودش را می‌بیند
        assertThat(TenantContext.callAs(teacherA.principal(),
                () -> classRoomService.get(classOfA)).id())
                .isEqualTo(classOfA);

        // معلم ب نمی‌بیند — و ۴۰۴ می‌گیرد، نه ۴۰۳
        assertThatThrownBy(() -> TenantContext.callAs(teacherB.principal(),
                () -> classRoomService.get(classOfA)))
                .as("کلاس همکار باید ناموجود دیده شود، نه ممنوع")
                .isInstanceOf(ResourceNotFoundException.class);

        // و مهم‌تر: رد شدن در جدول حسابرسی ثبت شده باشد. یک کنترل
        // دسترسی که رد می‌کند ولی کسی نمی‌بیندش، در حادثه واقعی
        // بی‌ارزش است.
        Long denials = jdbc.queryForObject("""
                select count(*) from audit_logs
                where action = 'SECURITY_ACCESS_DENIED' and entity_id = ?
                """, Long.class, classOfA);

        assertThat(denials)
                .as("رد دسترسی باید در audit_logs ثبت شده باشد")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("فهرست، برای معلم فقط کلاس‌های خودش و برای مالک همه را می‌آورد")
    void listingIsScopedByHomeroomTeacher() {
        Actor schoolOwner = newTenant(MemberRole.OWNER);
        Actor teacherA = newMemberOf(schoolOwner.tenantId(), MemberRole.TEACHER);
        Actor teacherB = newMemberOf(schoolOwner.tenantId(), MemberRole.TEACHER);

        createClass(teacherA, null);
        createClass(teacherB, null);

        PageResponse<ClassRoomResponse> asOwner = listFor(schoolOwner);
        PageResponse<ClassRoomResponse> asTeacherA = listFor(teacherA);
        PageResponse<ClassRoomResponse> asTeacherB = listFor(teacherB);

        assertThat(asOwner.totalElements())
                .as("مالک مدرسه باید هر دو کلاس را ببیند")
                .isEqualTo(2);

        assertThat(asTeacherA.content())
                .as("معلم الف فقط کلاس خودش را می‌بیند")
                .extracting(ClassRoomResponse::homeroomTeacherId)
                .containsExactly(teacherA.userId());

        assertThat(asTeacherB.content())
                .as("معلم ب فقط کلاس خودش را می‌بیند")
                .extracting(ClassRoomResponse::homeroomTeacherId)
                .containsExactly(teacherB.userId());

        assertThat(asTeacherA.totalElements())
                .as("شمارش کل هم باید فیلترشده باشد، نه فقط محتوای صفحه")
                .isEqualTo(1);
    }

    // ───────────────────── کلید خارجی ترکیبی ─────────────────────

    @Test
    @DisplayName("🔴 کلید خارجی ترکیبی، اتصال کلاس به درس مستأجر دیگر را می‌گیرد")
    void compositeForeignKeyBlocksCrossTenantSubjectLink() {
        Actor tenantA = newTenant(MemberRole.OWNER);
        Actor tenantB = newTenant(MemberRole.OWNER);
        UUID subjectInB = createSubject(tenantB, "MATH-" + unique());

        // ⚠️ این INSERT عمداً لایه سرویس را کامل دور می‌زند و همان
        // کاری را می‌کند که یک باگ در سرویس می‌کرد: کلاس مستأجر الف را
        // به درس مستأجر ب وصل می‌کند. اگر قید ترکیبی نبود، این درج
        // موفق می‌شد و هیچ تست سطح جاوا هم نمی‌گرفتش.
        assertThatThrownBy(() -> jdbc.update("""
                insert into classrooms (id, tenant_id, name, status, subject_id)
                values (?, ?, ?, 'ACTIVE', ?)
                """,
                UuidV7Generator.generate(), tenantA.tenantId(),
                "کلاس دست‌ساز " + unique(), subjectInB))
                .as("دیتابیس باید این ارجاع بین‌مستأجری را رد کند")
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ──────────────────── ایندکس جست‌وجو ────────────────────

    @Test
    @DisplayName("🔴 ایندکس جست‌وجوی نام، روی همان عبارتی است که کوئری می‌زند")
    void searchIndexMatchesTheQueryExpression() {
        // ⚠️ V8 این ایندکس را روی `name` خام ساخته بود، ولی کوئری
        // `lower(name) like :pattern` است و پستگرس آن دو را به هم
        // نگاشت نمی‌کند — یعنی ایندکس هرگز انتخاب نمی‌شد و فقط
        // هزینه نوشتن داشت. با EXPLAIN سنجیده شد (با
        // enable_seqscan=off هم پلن Seq Scan ماند). V9 اصلاحش کرد.
        //
        // این تست آن اصلاح را قفل می‌کند: یک ایندکس بی‌مصرف هیچ
        // خطایی نمی‌دهد، پس بدون این، بازگشتش دوباره نامرئی می‌شد.
        String definition = jdbc.queryForObject("""
                select indexdef from pg_indexes
                where tablename = 'classrooms' and indexname = 'idx_classrooms_name_trgm'
                """, String.class);

        assertThat(definition)
                .as("ایندکس باید روی lower(name) باشد، نه name خام")
                .contains("lower(");
    }

    // ─────────────────────── یکتایی درس ───────────────────────

    @Test
    @DisplayName("کد درس داخل مستأجر یکتاست، بدون حساسیت به بزرگی حروف، و بین مستأجرها آزاد")
    void subjectCodeIsUniquePerTenantCaseInsensitively() {
        Actor tenantA = newTenant(MemberRole.OWNER);
        Actor tenantB = newTenant(MemberRole.OWNER);

        String code = "MATH-" + unique();
        createSubject(tenantA, code);

        assertThatThrownBy(() -> createSubject(tenantA, code.toLowerCase(Locale.ROOT)))
                .as("همان کد با حروف کوچک هم باید تعارض بدهد")
                .isInstanceOf(DuplicateResourceException.class);

        assertThat(createSubject(tenantB, code))
                .as("همان کد در مستأجر دیگر باید آزاد باشد")
                .isNotNull();
    }

    // ─────────────────────── حذف نرم ───────────────────────

    @Test
    @DisplayName("درسِ متصل به کلاس حذف نمی‌شود؛ پس از جدا کردن، حذف نرم می‌شود")
    void deletingSubjectWithLinkedClassesIsRejected() {
        Actor owner = newTenant(MemberRole.OWNER);
        UUID subjectId = createSubject(owner, "MATH-" + unique());
        UUID classId = createClass(owner, subjectId);

        assertThatThrownBy(() -> TenantContext.runAs(owner.principal(),
                () -> subjectService.delete(subjectId)))
                .as("تا وقتی کلاسی به درس وصل است، حذف باید رد شود")
                .isInstanceOf(AppException.class);

        assertThat(isSoftDeleted("subjects", subjectId))
                .as("درس نباید حذف شده باشد")
                .isFalse();

        // کلاس را از درس جدا می‌کنیم
        TenantContext.runAs(owner.principal(), () -> classRoomService.update(classId,
                new UpdateClassRoomRequest("کلاس " + unique(), "پنجم", "1404-1405",
                        null, 30, ClassRoomStatus.ACTIVE)));

        TenantContext.runAs(owner.principal(), () -> subjectService.delete(subjectId));

        assertThat(isSoftDeleted("subjects", subjectId))
                .as("پس از جدا شدن، حذف نرم باید انجام شده باشد")
                .isTrue();

        // ⚠️ خواندن از مخزن باید *داخل* زمینه مستأجر باشد. بیرون از آن،
        // شناسه مستأجر به PLATFORM_SENTINEL می‌رود و کوئری همیشه خالی
        // برمی‌گردد — یعنی این سنجش بدون این wrapping بی‌معنا می‌شد و
        // همیشه «سبز» بود، حتی اگر حذف نرم کار نمی‌کرد.
        assertThat(TenantContext.callAs(owner.principal(),
                () -> subjectRepository.findByIdAndDeletedAtIsNull(subjectId)))
                .as("درس حذف‌شده نباید دیگر برگردد")
                .isEmpty();
    }

    @Test
    @DisplayName("کلاس حذف‌شده از فهرست و از خواندن می‌رود، ولی رکوردش می‌ماند")
    void softDeletedClassDisappearsButRowRemains() {
        Actor owner = newTenant(MemberRole.OWNER);
        UUID classId = createClass(owner, null);

        assertThat(listFor(owner).totalElements()).isEqualTo(1);

        TenantContext.runAs(owner.principal(), () -> classRoomService.delete(classId));

        assertThat(listFor(owner).totalElements())
                .as("کلاس حذف‌شده نباید در فهرست باشد")
                .isZero();

        assertThatThrownBy(() -> TenantContext.callAs(owner.principal(),
                () -> classRoomService.get(classId)))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(jdbc.queryForObject(
                "select count(*) from classrooms where id = ?", Long.class, classId))
                .as("رکورد باید در دیتابیس بماند — حذف نرم است، نه واقعی")
                .isEqualTo(1L);
    }

    private boolean isSoftDeleted(String table, UUID id) {
        // نام جدول از ثابت‌های همین تست می‌آید، نه از ورودی کاربر؛
        // ولی باز هم با مقایسه سنجیده می‌شود تا اگر روزی کسی متغیرش
        // کرد، این متد تبدیل به یک درِ تزریق SQL نشود.
        if (!"subjects".equals(table) && !"classrooms".equals(table)) {
            throw new IllegalArgumentException("جدول ناشناخته: " + table);
        }
        Boolean deleted = jdbc.queryForObject(
                "select deleted_at is not null from " + table + " where id = ?",
                Boolean.class, id);
        return Boolean.TRUE.equals(deleted);
    }
}
