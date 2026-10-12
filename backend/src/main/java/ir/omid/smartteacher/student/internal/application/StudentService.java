package ir.omid.smartteacher.student.internal.application;

import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.api.PageableFactory;
import ir.omid.smartteacher.common.api.SearchPattern;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.DuplicateResourceException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.security.TenantGuard;
import ir.omid.smartteacher.student.internal.api.dto.CreateStudentRequest;
import ir.omid.smartteacher.student.internal.api.dto.StudentResponse;
import ir.omid.smartteacher.student.internal.api.dto.UpdateStudentRequest;
import ir.omid.smartteacher.student.internal.api.mapper.StudentMapper;
import ir.omid.smartteacher.student.internal.domain.NationalCode;
import ir.omid.smartteacher.student.internal.domain.Student;
import ir.omid.smartteacher.student.internal.domain.StudentRepository;
import ir.omid.smartteacher.student.internal.domain.StudentStatus;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * منطق کسب‌وکار دانش‌آموزان.
 *
 * <p><b>⚠️ برخلاف کلاس‌ها، اینجا محدودیت «مالکیت معلم» وجود ندارد.</b>
 * دانش‌آموز دارایی <i>مدرسه</i> است، نه یک معلم: پرونده‌اش باید وقتی
 * معلم عوض می‌شود بماند و معلم دوم باید بتواند آن را ببیند. پس هر
 * کارمند مستأجر همه دانش‌آموزان مستأجر خودش را می‌بیند و فیلتر
 * {@code @TenantId} تمام مرز لازم است.
 *
 * <p>⚠️ <b>پیامدش را بشناسید:</b> معلم الف دانش‌آموز معلم ب را
 * <i>می‌بیند</i>. این عمدی است و با قید پذیرش فاز ۳
 * ({@code teacherA_cannotReadTeacherB_students}) در تضاد <b>نیست</b> —
 * آن قید درباره <i>کلاس</i> معلم ب است، و کلاس‌ها همچنان محدودند
 * ({@code ClassRoomService.requireAccessible}). تفکیک این دو، تفکیک
 * «دارایی مدرسه» از «دارایی معلم» است.
 *
 * <p>⚠️ <b>حذف دانش‌آموزِ ثبت‌نام‌شده رد نمی‌شود</b> — برخلاف حذف درس.
 * دلیلش این است که ردیف‌های {@code class_students} مال ماژول
 * {@code classroom} است و این ماژول طبق قاعده ۴ معماری حق ندارد آن‌ها
 * را بشمارد. در عوض، هر خواندن فهرست کلاس از
 * {@code StudentDirectory} می‌گذرد که فقط دانش‌آموزان <i>فعال</i> را
 * برمی‌گرداند؛ پس دانش‌آموز حذف‌شده بی‌صدا از فهرست کلاس می‌رود و
 * ردیف ثبت‌نامش بی‌اثر می‌ماند. همان الگویی که برای درس حذف‌شده و
 * {@code subjectName} برابر {@code null} پذیرفته شد.
 */
@Service
public class StudentService {

    /** فیلدهای مجاز مرتب‌سازی — Allow-List طبق {@code docs/api.md} بخش ۷. */
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("fullName", "nationalCode", "status", "createdAt", "updatedAt");

    private static final String DEFAULT_SORT = "fullName,asc";

    private final StudentRepository studentRepository;
    private final StudentMapper mapper;
    private final TenantGuard tenantGuard;

    public StudentService(StudentRepository studentRepository,
                          StudentMapper mapper,
                          TenantGuard tenantGuard) {
        this.studentRepository = studentRepository;
        this.mapper = mapper;
        this.tenantGuard = tenantGuard;
    }

    /** فهرست صفحه‌بندی‌شده دانش‌آموزان مستأجر جاری. */
    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> list(int page,
                                              int size,
                                              String sort,
                                              String search,
                                              StudentStatus status) {
        Pageable pageable = PageableFactory.of(page, size, sort, SORTABLE_FIELDS, DEFAULT_SORT);
        Page<Student> result =
                studentRepository.findForListing(SearchPattern.like(search), status, pageable);
        return PageResponse.from(result, mapper::toResponse);
    }

    /** یک دانش‌آموز مشخص. */
    @Transactional(readOnly = true)
    public StudentResponse get(UUID id) {
        return mapper.toResponse(require(id));
    }

    /** ساخت دانش‌آموز تازه. */
    @Transactional
    public StudentResponse create(CreateStudentRequest request) {
        String nationalCode = trimToNull(request.nationalCode());
        requireValidNationalCode(nationalCode);
        requireNoNationalCodeConflict(nationalCode, null);

        Student student = new Student(request.fullName().trim());
        student.setNationalCode(nationalCode);
        applyContact(student, request.guardianName(), request.guardianMobile());
        student.setBirthDate(request.birthDate());
        student.setGender(request.gender());

        return mapper.toResponse(studentRepository.save(student));
    }

    /** ویرایش کامل دانش‌آموز. */
    @Transactional
    public StudentResponse update(UUID id, UpdateStudentRequest request) {
        Student student = require(id);

        String nationalCode = trimToNull(request.nationalCode());
        requireValidNationalCode(nationalCode);
        requireNoNationalCodeConflict(nationalCode, student.getId());

        student.setFullName(request.fullName().trim());
        student.setNationalCode(nationalCode);
        applyContact(student, request.guardianName(), request.guardianMobile());
        student.setBirthDate(request.birthDate());
        student.setGender(request.gender());
        student.setStatus(request.status());

        return mapper.toResponse(studentRepository.save(student));
    }

    /**
     * حذف نرم دانش‌آموز.
     *
     * <p>⚠️ ثبت‌نام‌هایش حذف نمی‌شوند و بررسی هم نمی‌شوند — دلیلش در
     * Javadoc همین کلاس آمده. ردیف‌های {@code class_students} باقی
     * می‌مانند ولی از آنجا که هر خواندن فهرست فقط دانش‌آموزان فعال را
     * برمی‌گرداند، آن‌ها بی‌اثر می‌شوند.
     */
    @Transactional
    public void delete(UUID id) {
        Student student = require(id);
        student.softDelete();
        studentRepository.save(student);
    }

    /**
     * خواندن دانش‌آموز با بررسی مالکیت.
     *
     * <p>لایه اول فیلتر خودکار {@code @TenantId} است که دانش‌آموز مستأجر
     * دیگر را اصلاً برنمی‌گرداند؛ لایه دوم {@link TenantGuard} همان
     * {@code null} را به ۴۰۴ تبدیل می‌کند.
     */
    private Student require(UUID id) {
        return tenantGuard.requireOwned(
                studentRepository.findByIdAndDeletedAtIsNull(id).orElse(null), "Student");
    }

    /**
     * بررسی رقم کنترلی کد ملی — فقط وقتی کد داده شده باشد.
     *
     * <p>⚠️ چرا در سرویس و نه با {@code @Pattern} روی DTO؟ چون الگوریتم
     * رقم کنترلی است، نه یک الگوی ساده. با regex فقط «ده رقم» قابل
     * بررسی است و همان چیزی که ارزش دارد — درست‌بودن رقم آخر — از دست
     * می‌رود.
     */
    private void requireValidNationalCode(String nationalCode) {
        if (nationalCode != null && !NationalCode.isValid(nationalCode)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "کد ملی نامعتبر است.");
        }
    }

    /**
     * بررسی تعارض کد ملی — آینه‌ی دقیق ایندکس
     * {@code uk_students_national_code}.
     *
     * @param nationalCode کد؛ {@code null} یعنی بررسی لازم نیست
     * @param selfId       شناسه خودِ رکورد در حال ویرایش تا با خودش
     *                     تعارض نگیرد؛ {@code null} در حالت ساخت
     */
    private void requireNoNationalCodeConflict(String nationalCode, UUID selfId) {
        if (nationalCode == null) {
            return;
        }
        boolean conflictsWithOther = studentRepository
                .findConflictingNationalCodeIds(nationalCode)
                .stream()
                .anyMatch(found -> !found.equals(selfId));
        if (conflictsWithOther) {
            // ⚠️ پیام عامدانه کد ملی را تکرار نمی‌کند. کد ملی داده
            // هویتی است و تکرارش در پیام خطا یعنی راه‌یافتنش به لاگ
            // درخواست‌های ناموفق.
            throw DuplicateResourceException.of("دانش‌آموز", "کد ملی");
        }
    }

    private static void applyContact(Student student, String guardianName, String guardianMobile) {
        student.setGuardianName(trimToNull(guardianName));
        student.setGuardianMobile(trimToNull(guardianMobile));
    }

    /** رشته خالی را به {@code null} تبدیل می‌کند تا «خالی» و «ندارد» یکی نشوند. */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
