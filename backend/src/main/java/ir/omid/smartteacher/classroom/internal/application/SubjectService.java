package ir.omid.smartteacher.classroom.internal.application;

import ir.omid.smartteacher.classroom.internal.api.dto.CreateSubjectRequest;
import ir.omid.smartteacher.classroom.internal.api.dto.SubjectResponse;
import ir.omid.smartteacher.classroom.internal.api.dto.UpdateSubjectRequest;
import ir.omid.smartteacher.classroom.internal.api.mapper.ClassRoomMapper;
import ir.omid.smartteacher.classroom.internal.domain.ClassRoomRepository;
import ir.omid.smartteacher.classroom.internal.domain.Subject;
import ir.omid.smartteacher.classroom.internal.domain.SubjectRepository;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.api.PageableFactory;
import ir.omid.smartteacher.common.api.SearchPattern;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.DuplicateResourceException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.security.TenantGuard;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * منطق کسب‌وکار درس‌ها.
 *
 * <p><b>⚠️ چرا درس‌ها API جدا دارند در حالی که {@code docs/api.md} بخش ۴
 * هیچ مسیری برایشان فهرست نکرده؟</b> چون ERD ({@code docs/database.md}
 * بخش ۳) و نقشه راه ({@code docs/roadmap.md} فاز ۳، مورد ۳) جدول درس را
 * الزام کرده‌اند و {@code classrooms.subject_id} بدون راهی برای ساختن
 * درس عملاً قابل استفاده نیست. این یک انحراف آگاهانه از فهرست endpoint
 * هاست و در {@code docs/verification-log.md} و {@code docs/api.md} ثبت
 * شده — نه چیزی که کسی باید بعداً کشف کند.
 *
 * <p><b>⚠️ چرا {@code @Transactional} روی خواندن‌ها هم هست؟</b> با
 * {@code readOnly = true}: به Hibernate اجازه می‌دهد flush را رد کند و
 * با {@code open-in-view: false} که در این پروژه فعال است، تراکنش
 * خواندن همان چیزی است که از {@code LazyInitializationException} جلوگیری
 * می‌کند. گرچه این مخزن‌ها رابطه تنبل ندارند (هیچ ارتباط JPA در پروژه
 * نیست)، الگو یکدست می‌ماند تا روزی که یکی اضافه شد، کسی مجبور نباشد
 * یادش بیاید.
 */
@Service
public class SubjectService {

    /**
     * فیلدهای مجاز مرتب‌سازی — Allow-List طبق {@code docs/api.md} بخش ۷.
     *
     * <p>⚠️ این نام‌ها <b>نام پراپرتی موجودیت</b> هستند، نه نام ستون
     * دیتابیس و نه نام فیلد DTO. Spring Data آن‌ها را به مسیر JPQL
     * ترجمه می‌کند؛ اگر نام ستون بدهیم، کوئری با «could not resolve
     * property» می‌شکند.
     */
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("name", "code", "gradeLevel", "createdAt", "updatedAt");

    private static final String DEFAULT_SORT = "name,asc";

    private final SubjectRepository subjectRepository;
    private final ClassRoomRepository classRoomRepository;
    private final ClassRoomMapper mapper;
    private final TenantGuard tenantGuard;

    public SubjectService(SubjectRepository subjectRepository,
                          ClassRoomRepository classRoomRepository,
                          ClassRoomMapper mapper,
                          TenantGuard tenantGuard) {
        this.subjectRepository = subjectRepository;
        this.classRoomRepository = classRoomRepository;
        this.mapper = mapper;
        this.tenantGuard = tenantGuard;
    }

    /** فهرست صفحه‌بندی‌شده درس‌های مستأجر جاری. */
    @Transactional(readOnly = true)
    public PageResponse<SubjectResponse> list(int page, int size, String sort, String search) {
        Pageable pageable = PageableFactory.of(page, size, sort, SORTABLE_FIELDS, DEFAULT_SORT);
        Page<Subject> result =
                subjectRepository.searchByPattern(SearchPattern.like(search), pageable);
        return PageResponse.from(result, mapper::toResponse);
    }

    /** ساخت درس تازه. */
    @Transactional
    public SubjectResponse create(CreateSubjectRequest request) {
        String code = trimToNull(request.code());
        requireNoCodeConflict(code, null);

        Subject subject = new Subject(request.name().trim());
        subject.setCode(code);
        subject.setGradeLevel(trimToNull(request.gradeLevel()));

        return mapper.toResponse(subjectRepository.save(subject));
    }

    /** یک درس مشخص. */
    @Transactional(readOnly = true)
    public SubjectResponse get(UUID id) {
        return mapper.toResponse(require(id));
    }

    /** ویرایش کامل درس. */
    @Transactional
    public SubjectResponse update(UUID id, UpdateSubjectRequest request) {
        Subject subject = require(id);

        String code = trimToNull(request.code());
        requireNoCodeConflict(code, subject.getId());

        subject.setName(request.name().trim());
        subject.setCode(code);
        subject.setGradeLevel(trimToNull(request.gradeLevel()));

        return mapper.toResponse(subjectRepository.save(subject));
    }

    /**
     * حذف نرم درس.
     *
     * <p><b>⚠️ چرا اگر کلاسی به درس وصل باشد حذف رد می‌شود؟</b> چون
     * کلید خارجی {@code ON DELETE SET NULL} است، ولی ما حذف <i>نرم</i>
     * می‌کنیم و آن قید هرگز فعال نمی‌شود. اگر اجازه می‌دادیم، کلاس‌ها
     * به درسی اشاره می‌کردند که دیگر خوانده نمی‌شود و نامش در پاسخ‌ها
     * {@code null} می‌شد — یعنی داده‌ای که بی‌صدا از دید کاربر محو
     * می‌شود و هیچ خطایی هم نمی‌دهد. رد کردن صریح، این را به تصمیمی
     * آگاهانه تبدیل می‌کند.
     */
    @Transactional
    public void delete(UUID id) {
        Subject subject = require(id);

        long connectedClasses =
                classRoomRepository.countBySubjectIdAndDeletedAtIsNull(subject.getId());
        if (connectedClasses > 0) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT,
                    "این درس به " + connectedClasses + " کلاس متصل است. "
                            + "ابتدا کلاس‌ها را از این درس جدا کنید.");
        }

        subject.softDelete();
        subjectRepository.save(subject);
    }

    /**
     * خواندن درس با بررسی مالکیت.
     *
     * <p>ترتیب دو لایه دفاعی اینجاست: فیلتر خودکار Hibernate درس مستأجر
     * دیگر را اصلاً برنمی‌گرداند (پس {@code orElse(null)})، و
     * {@link TenantGuard} همان {@code null} را به ۴۰۴ تبدیل می‌کند.
     */
    private Subject require(UUID id) {
        return tenantGuard.requireOwned(
                subjectRepository.findByIdAndDeletedAtIsNull(id).orElse(null), "Subject");
    }

    /**
     * بررسی تعارض کد — آینه‌ی دقیق ایندکس {@code uk_subjects_code}.
     *
     * @param code   کد تازه؛ {@code null} یعنی بررسی لازم نیست
     * @param selfId شناسه خودِ رکورد در حال ویرایش، تا با خودش تعارض
     *               نگیرد؛ {@code null} در حالت ساخت
     */
    private void requireNoCodeConflict(String code, UUID selfId) {
        if (code == null) {
            return;
        }
        List<UUID> conflicts = subjectRepository.findConflictingCodeIds(code);
        boolean conflictsWithOther = conflicts.stream().anyMatch(found -> !found.equals(selfId));
        if (conflictsWithOther) {
            throw DuplicateResourceException.of("درس", "کد");
        }
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
