package ir.omid.smartteacher.classroom.internal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ir.omid.smartteacher.audit.ActorType;
import ir.omid.smartteacher.classroom.internal.api.dto.ClassRoomResponse;
import ir.omid.smartteacher.classroom.internal.api.dto.CreateClassRoomRequest;
import ir.omid.smartteacher.classroom.internal.api.mapper.ClassRoomMapper;
import ir.omid.smartteacher.classroom.internal.api.mapper.ClassRoomMapperImpl;
import ir.omid.smartteacher.classroom.internal.domain.ClassRoom;
import ir.omid.smartteacher.classroom.internal.domain.ClassRoomRepository;
import ir.omid.smartteacher.classroom.internal.domain.ClassRoomStatus;
import ir.omid.smartteacher.classroom.internal.domain.SubjectRepository;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.security.TenantGuard;
import ir.omid.smartteacher.security.audit.SecurityAuditLog;
import ir.omid.smartteacher.security.audit.SecurityEvent;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * آزمون قاعده دسترسی کلاس — <b>مهم‌ترین تست فاز ۳</b>.
 *
 * <p><b>این تست چه چیزی را ثابت می‌کند که تست جداسازی مستأجر ثابت
 * نمی‌کند؟</b> آن تست می‌گوید «داده مستأجر ب برای الف دیده نمی‌شود».
 * این تست چیزی سخت‌تر می‌گوید: «داده <i>معلم همکار</i> در همان مدرسه
 * هم برای معلم دیگر دیده نمی‌شود». اگر فقط فیلتر مستأجر بود، هر معلمی
 * کلاس‌های همه معلمان مدرسه را می‌دید — و آن، هم نشت اطلاعات است و هم
 * یک باگ کسب‌وکار که هیچ ابزار خودکاری نمی‌گیرد.
 *
 * <p><b>⚠️ چرا unit test و نه integration test؟</b> چون این قاعده هیچ
 * ربطی به دیتابیس ندارد: ورودی‌هایش هویت کاربر و صاحب رکورد است.
 * اجرای آن روی Testcontainers، سی ثانیه طول می‌کشد و هیچ چیز بیشتری
 * ثابت نمی‌کند. تست دیتابیسیِ جداسازی مستأجر سرِ جای خودش هست
 * ({@code TenantIsolationTest}) و این تست ادعای دیگری دارد.
 *
 * <p>⚠️ {@code TenantGuard} واقعی است، نه mock. با mock کردنش، همین
 * لایه دوم دفاعی بدون تست می‌ماند — که دقیقاً همان لایه‌ای است که
 * نمی‌شود به آن اعتماد کرد بدون اینکه دید.
 */
class ClassRoomServiceAccessTest {

    private static final UUID TENANT_A = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID TENANT_B = UUID.fromString("22222222-2222-7222-8222-222222222222");

    private static final UUID TEACHER_A = UUID.fromString("aaaaaaaa-aaaa-7aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID TEACHER_B = UUID.fromString("bbbbbbbb-bbbb-7bbb-8bbb-bbbbbbbbbbbb");
    private static final UUID OWNER = UUID.fromString("cccccccc-cccc-7ccc-8ccc-cccccccccccc");
    private static final UUID SCHOOL_ADMIN =
            UUID.fromString("dddddddd-dddd-7ddd-8ddd-dddddddddddd");
    private static final UUID ASSISTANT =
            UUID.fromString("eeeeeeee-eeee-7eee-8eee-eeeeeeeeeeee");

    private static final UUID CLASS_ID = UUID.fromString("99999999-9999-7999-8999-999999999999");

    private ClassRoomRepository classRoomRepository;
    private SubjectRepository subjectRepository;
    private SecurityAuditLog securityAuditLog;
    private ClassRoomService service;

    @BeforeEach
    void setUp() {
        classRoomRepository = mock(ClassRoomRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        securityAuditLog = mock(SecurityAuditLog.class);

        ClassRoomMapper mapper = new ClassRoomMapperImpl();

        service = new ClassRoomService(
                classRoomRepository,
                subjectRepository,
                mapper,
                new TenantGuard(securityAuditLog),
                securityAuditLog);
    }

    // ─────────────────────────── کمکی ───────────────────────────

    /** کلاسی در مستأجر مشخص با معلم سرکلاس مشخص. */
    private static ClassRoom classRoom(UUID tenantId, UUID homeroomTeacherId) {
        ClassRoom room = new ClassRoom("پنجم الف");
        room.setHomeroomTeacherId(homeroomTeacherId);
        // ⚠️ tenantId فقط از راه @PrePersist پر می‌شود (که در unit test
        // اجرا نمی‌شود)، پس صریح می‌گذاریمش. همین می‌گذارد مستأجر رکورد
        // را از مستأجر کاربر جدا کنیم و مسیر رد شدن را بسنجیم.
        ReflectionTestUtils.setField(room, "tenantId", tenantId);
        ReflectionTestUtils.setField(room, "id", CLASS_ID);
        return room;
    }

    private static TenantPrincipal teacher(UUID userId) {
        return TenantPrincipal.ofTenantUser(
                userId, TENANT_A, null, Set.of("TEACHER"), MemberRole.TEACHER, "fa");
    }

    private static TenantPrincipal member(UUID userId, MemberRole role) {
        return TenantPrincipal.ofTenantUser(
                userId, TENANT_A, null, Set.of(role.name()), role, "fa");
    }

    // ─────────────────────── خواندن کلاس ───────────────────────

    @Test
    @DisplayName("معلم کلاس خودش را می‌بیند")
    void teacherReadsOwnClass() {
        ClassRoom room = classRoom(TENANT_A, TEACHER_A);
        when(classRoomRepository.findByIdAndDeletedAtIsNull(CLASS_ID))
                .thenReturn(Optional.of(room));

        ClassRoomResponse response = TenantContext.callAs(teacher(TEACHER_A),
                () -> service.get(CLASS_ID));

        assertThat(response.id()).isEqualTo(CLASS_ID);
        assertThat(response.name()).isEqualTo("پنجم الف");
        verify(securityAuditLog, never()).record(any(), any(), any(), any(), any(), any(), anyMap());
    }

    @Test
    @DisplayName("🔴 معلم کلاس همکارش را نمی‌بیند — ۴۰۴، نه ۴۰۳")
    void teacherCannotReadColleaguesClass() {
        ClassRoom room = classRoom(TENANT_A, TEACHER_B);
        when(classRoomRepository.findByIdAndDeletedAtIsNull(CLASS_ID))
                .thenReturn(Optional.of(room));

        assertThatThrownBy(() -> TenantContext.callAs(teacher(TEACHER_A),
                () -> service.get(CLASS_ID)))
                .as("کلاس همکار در همان مستأجر باید ناموجود دیده شود")
                .isInstanceOf(ResourceNotFoundException.class);

        // ⚠️ بخش مهم‌تر از پرتاب استثنا: اینکه رد شدن ثبت شده باشد.
        // کنترل دسترسی‌ای که اتفاق می‌افتد ولی کسی نمی‌بیندش، در
        // حادثه واقعی هیچ ارزشی ندارد.
        verify(securityAuditLog).record(
                eq(SecurityEvent.ACCESS_DENIED),
                eq(TENANT_A),
                eq(TEACHER_A),
                eq(ActorType.USER),
                eq("ClassRoom"),
                eq(CLASS_ID),
                anyMap());
    }

    @Test
    @DisplayName("مالک مستأجر همه کلاس‌ها را می‌بیند")
    void ownerReadsAnyClassInTenant() {
        ClassRoom room = classRoom(TENANT_A, TEACHER_B);
        when(classRoomRepository.findByIdAndDeletedAtIsNull(CLASS_ID))
                .thenReturn(Optional.of(room));

        ClassRoomResponse response = TenantContext.callAs(member(OWNER, MemberRole.OWNER),
                () -> service.get(CLASS_ID));

        assertThat(response.id()).isEqualTo(CLASS_ID);
        verify(securityAuditLog, never()).record(any(), any(), any(), any(), any(), any(), anyMap());
    }

    @Test
    @DisplayName("مدیر مدرسه همه کلاس‌ها را می‌بیند")
    void schoolAdminReadsAnyClassInTenant() {
        ClassRoom room = classRoom(TENANT_A, TEACHER_B);
        when(classRoomRepository.findByIdAndDeletedAtIsNull(CLASS_ID))
                .thenReturn(Optional.of(room));

        ClassRoomResponse response = TenantContext.callAs(
                member(SCHOOL_ADMIN, MemberRole.SCHOOL_ADMIN), () -> service.get(CLASS_ID));

        assertThat(response.id()).isEqualTo(CLASS_ID);
    }

    @Test
    @DisplayName("دستیار مستأجر، کلاس همکار را نمی‌بیند")
    void assistantIsTreatedAsOrdinaryStaff() {
        // ⚠️ تصمیم آگاهانه: docs/api.md بخش ۴.۲ ستون دسترسی این مسیرها
        // را TEACHER گذاشته و دستیار مدیر نیست. اگر روزی این را عوض
        // کردید، این تست باید عوض شود — و همین باعث می‌شود تغییر
        // آگاهانه باشد.
        ClassRoom room = classRoom(TENANT_A, TEACHER_B);
        when(classRoomRepository.findByIdAndDeletedAtIsNull(CLASS_ID))
                .thenReturn(Optional.of(room));

        assertThatThrownBy(() -> TenantContext.callAs(member(ASSISTANT, MemberRole.ASSISTANT),
                () -> service.get(CLASS_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("کلاس ناموجود ۴۰۴ می‌دهد")
    void missingClassIsNotFound() {
        when(classRoomRepository.findByIdAndDeletedAtIsNull(CLASS_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> TenantContext.callAs(member(OWNER, MemberRole.OWNER),
                () -> service.get(CLASS_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("🔴 کلاس مستأجر دیگر از این مستأجر دیده نمی‌شود و رویداد IDOR ثبت می‌شود")
    void classOfAnotherTenantIsNotFoundAndAudited() {
        // در واقعیت فیلتر خودکار @TenantId این رکورد را برنمی‌گرداند و
        // سرویس Optional.empty() می‌گیرد. اینجا عمداً رکورد را برمی‌گردانیم
        // تا لایه *دوم* (TenantGuard) مستقل از لایه اول سنجیده شود —
        // وگرنه هیچ‌وقت نمی‌فهمیم لایه دوم کار می‌کند یا فقط سوار
        // لایه اول است.
        ClassRoom room = classRoom(TENANT_B, TEACHER_A);
        when(classRoomRepository.findByIdAndDeletedAtIsNull(CLASS_ID))
                .thenReturn(Optional.of(room));

        assertThatThrownBy(() -> TenantContext.callAs(member(OWNER, MemberRole.OWNER),
                () -> service.get(CLASS_ID)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(securityAuditLog).idorAttempt("ClassRoom", CLASS_ID, TENANT_B);
    }

    // ─────────────────────── ساخت و حذف ───────────────────────

    @Test
    @DisplayName("کلاس تازه به نام کاربر جاری ثبت می‌شود، نه به نامی از درخواست")
    void createdClassIsAssignedToCurrentUser() {
        when(classRoomRepository.findConflictingIds(any(), any())).thenReturn(List.of());
        when(classRoomRepository.save(any(ClassRoom.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateClassRoomRequest request = new CreateClassRoomRequest(
                "پنجم الف", "پنجم", "1404-1405", null, 30);

        ClassRoomResponse response = TenantContext.callAs(teacher(TEACHER_A),
                () -> service.create(request));

        assertThat(response.homeroomTeacherId())
                .as("معلم سرکلاس باید همان کاربر جاری باشد")
                .isEqualTo(TEACHER_A);
        assertThat(response.name()).isEqualTo("پنجم الف");
        assertThat(response.status())
                .as("کلاس تازه فعال است")
                .isEqualTo(ClassRoomStatus.ACTIVE);
    }

    @Test
    @DisplayName("حذف کلاس، حذف نرم است — رکورد می‌ماند")
    void deleteIsSoft() {
        ClassRoom room = classRoom(TENANT_A, TEACHER_A);
        when(classRoomRepository.findByIdAndDeletedAtIsNull(CLASS_ID))
                .thenReturn(Optional.of(room));
        when(classRoomRepository.save(any(ClassRoom.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TenantContext.runAs(teacher(TEACHER_A), () -> service.delete(CLASS_ID));

        assertThat(room.isDeleted())
                .as("رکورد باید حذف نرم شده باشد، نه پاک")
                .isTrue();
        verify(classRoomRepository).save(room);
    }
}
