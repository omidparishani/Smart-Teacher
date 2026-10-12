package ir.omid.smartteacher.classroom.internal.application;

import ir.omid.smartteacher.audit.ActorType;
import ir.omid.smartteacher.classroom.internal.api.dto.ClassRoomResponse;
import ir.omid.smartteacher.classroom.internal.api.dto.CreateClassRoomRequest;
import ir.omid.smartteacher.classroom.internal.api.dto.UpdateClassRoomRequest;
import ir.omid.smartteacher.classroom.internal.api.mapper.ClassRoomMapper;
import ir.omid.smartteacher.classroom.internal.domain.ClassRoom;
import ir.omid.smartteacher.classroom.internal.domain.ClassRoomRepository;
import ir.omid.smartteacher.classroom.internal.domain.ClassRoomStatus;
import ir.omid.smartteacher.classroom.internal.domain.Subject;
import ir.omid.smartteacher.classroom.internal.domain.SubjectRepository;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.api.PageableFactory;
import ir.omid.smartteacher.common.api.SearchPattern;
import ir.omid.smartteacher.common.error.DuplicateResourceException;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.security.TenantGuard;
import ir.omid.smartteacher.security.audit.SecurityAuditLog;
import ir.omid.smartteacher.security.audit.SecurityEvent;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * منطق کسب‌وکار کلاس‌ها — قلب فاز ۳.
 *
 * <p><b>🔴 قاعده دسترسی، در یک جمله:</b> معلم فقط کلاس‌های خودش را
 * می‌بیند و تغییر می‌دهد؛ مالک و مدیر مدرسه همه کلاس‌های مستأجر را.
 *
 * <p>این قاعده یک تصمیم <i>کسب‌وکار</i> است، نه جداسازی مستأجر.
 * جداسازی مستأجر کار {@code @TenantId} و {@link TenantGuard} است و
 * می‌گوید «داده مستأجر دیگر دیده نشود»؛ اینجا می‌گوییم «داده همکار
 * من هم دیده نشود». هر دو لازم‌اند و هیچ‌کدام جایگزین دیگری نیست: اگر
 * فقط {@code @TenantId} بود، هر معلمی کلاس‌های همه معلمان مدرسه را
 * می‌دید.
 *
 * <p><b>⚠️ چرا این بررسی در سرویس است و نه {@code @PreAuthorize}؟</b>
 * چون {@code @PreAuthorize} پیش از خواندن رکورد اجرا می‌شود و چیزی از
 * رکورد نمی‌داند؛ برای فهمیدن «مالک این کلاس کیست» باید ابتدا کلاس را
 * خواند. پس لایه نقش (<i>آیا اصلاً کارمند این مستأجر است؟</i>) در
 * Controller با {@code @PreAuthorize} است و ۴۰۳ می‌دهد؛ لایه مالکیت
 * (<i>آیا این کلاس مال خودش است؟</i>) اینجا است و ۴۰۴ می‌دهد. تفاوت
 * ۴۰۳ و ۴۰۴ عمدی است: در اولی هیچ رکوردی در میان نیست که وجودش لو
 * برود، در دومی هست.
 */
@Service
public class ClassRoomService {

    /** فیلدهای مجاز مرتب‌سازی — Allow-List طبق {@code docs/api.md} بخش ۷. */
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("name", "gradeLevel", "academicYear", "createdAt", "updatedAt");

    private static final String DEFAULT_SORT = "createdAt,desc";

    private final ClassRoomRepository classRoomRepository;
    private final SubjectRepository subjectRepository;
    private final ClassRoomMapper mapper;
    private final TenantGuard tenantGuard;
    private final SecurityAuditLog securityAuditLog;

    public ClassRoomService(ClassRoomRepository classRoomRepository,
                            SubjectRepository subjectRepository,
                            ClassRoomMapper mapper,
                            TenantGuard tenantGuard,
                            SecurityAuditLog securityAuditLog) {
        this.classRoomRepository = classRoomRepository;
        this.subjectRepository = subjectRepository;
        this.mapper = mapper;
        this.tenantGuard = tenantGuard;
        this.securityAuditLog = securityAuditLog;
    }

    // ─────────────────────────── خواندن ───────────────────────────

    /**
     * فهرست صفحه‌بندی‌شده کلاس‌ها — <b>محدود به آنچه کاربر حق دیدنش را
     * دارد</b>.
     *
     * <p>⚠️ محدودیت معلم اینجا با یک <i>شرط کوئری</i> اعمال می‌شود و نه
     * با فیلترکردن نتیجه پس از خواندن. تفاوتش فقط کارایی نیست: اگر
     * فیلتر بعد از خواندن بود، {@code totalElements} کل مستأجر را
     * می‌گفت و معلم از شمارش کلاس‌ها می‌فهمید چند کلاس دارد که نمی‌بیند.
     */
    @Transactional(readOnly = true)
    public PageResponse<ClassRoomResponse> list(int page,
                                                int size,
                                                String sort,
                                                String search,
                                                ClassRoomStatus status) {

        Pageable pageable = PageableFactory.of(page, size, sort, SORTABLE_FIELDS, DEFAULT_SORT);

        // ⚠️ null یعنی «بدون محدودیت معلم» — و فقط مالک/مدیر مدرسه به آن
        // می‌رسد. یک معلم هرگز null نمی‌فرستد، پس نمی‌تواند کلاس دیگری را
        // ببیند.
        UUID teacherFilter = canManageAllClasses() ? null : TenantContext.requireUserId();

        Page<ClassRoom> result = classRoomRepository.findForListing(
                SearchPattern.like(search), status, teacherFilter, pageable);

        // نام درس‌ها یک‌جا خوانده می‌شود. اگر داخل نگاشت تک‌تک می‌خواندیم،
        // یک صفحه ۲۰تایی ۲۱ کوئری می‌زد.
        Map<UUID, String> subjectNames = subjectNamesOf(result.getContent());

        return PageResponse.from(result,
                room -> mapper.toResponse(room, subjectNameFrom(room, subjectNames)));
    }

    /** یک کلاس مشخص. */
    @Transactional(readOnly = true)
    public ClassRoomResponse get(UUID id) {
        ClassRoom room = requireAccessible(id);
        return mapper.toResponse(room, subjectNameOf(room.getSubjectId()));
    }

    /**
     * خواندن کلاس با بررسی مالکیت — برای استفاده درون همین ماژول.
     *
     * <p>مسیرهای {@code /classes/{id}/students} و
     * {@code /classes/{id}/dashboard} همین متد را صدا می‌زنند تا یک
     * قاعده دسترسی داشته باشیم، نه دو تا که روزی از هم جدا شوند.
     *
     * @param id شناسه کلاس
     * @return همان کلاس، اگر کاربر حق دیدنش را داشته باشد
     * @throws ResourceNotFoundException اگر کلاس نباشد، مال مستأجر
     *                                   دیگری باشد، یا مال معلم دیگری
     *                                   در همین مستأجر باشد (۴۰۴، نه ۴۰۳)
     */
    @Transactional(readOnly = true)
    public ClassRoom requireAccessible(UUID id) {
        ClassRoom room = tenantGuard.requireOwned(
                classRoomRepository.findByIdAndDeletedAtIsNull(id).orElse(null), "ClassRoom");

        if (canManageAllClasses()) {
            return room;
        }

        UUID actorId = TenantContext.requireUserId();
        if (!Objects.equals(room.getHomeroomTeacherId(), actorId)) {
            // این IDOR بین‌مستأجری نیست (TenantGuard آن را می‌گیرد)، پس
            // نام رویداد IDOR_ATTEMPT نمی‌گذاریم: برچسب عوض‌کردن یک رویداد،
            // گزارش‌های امنیتی را بی‌اعتبار می‌کند. ACCESS_DENIED درست است.
            securityAuditLog.record(SecurityEvent.ACCESS_DENIED,
                    room.getTenantId(),
                    actorId,
                    ActorType.USER,
                    "ClassRoom",
                    room.getId(),
                    Map.of("reason", "notHomeroomTeacher"));
            throw ResourceNotFoundException.generic();
        }
        return room;
    }

    // ─────────────────────────── نوشتن ───────────────────────────

    /**
     * ساخت کلاس تازه.
     *
     * <p><b>⚠️ معلم سرکلاس همیشه کاربر جاری است و از درخواست خوانده
     * نمی‌شود.</b> دلیل امنیتی‌اش در Javadoc {@code CreateClassRoomRequest}
     * آمده است.
     */
    @Transactional
    public ClassRoomResponse create(CreateClassRoomRequest request) {
        String name = request.name().trim();
        String academicYear = trimToNull(request.academicYear());
        requireNoNameConflict(name, academicYear, null);

        ClassRoom room = new ClassRoom(name);
        room.setGradeLevel(trimToNull(request.gradeLevel()));
        room.setAcademicYear(academicYear);
        room.setCapacity(request.capacity());
        room.setHomeroomTeacherId(TenantContext.requireUserId());
        applySubject(room, request.subjectId());

        ClassRoom saved = classRoomRepository.save(room);
        return mapper.toResponse(saved, subjectNameOf(saved.getSubjectId()));
    }

    /** ویرایش کامل کلاس. */
    @Transactional
    public ClassRoomResponse update(UUID id, UpdateClassRoomRequest request) {
        ClassRoom room = requireAccessible(id);

        String name = request.name().trim();
        String academicYear = trimToNull(request.academicYear());
        requireNoNameConflict(name, academicYear, room.getId());

        room.setName(name);
        room.setGradeLevel(trimToNull(request.gradeLevel()));
        room.setAcademicYear(academicYear);
        room.setCapacity(request.capacity());
        room.setStatus(request.status());
        applySubject(room, request.subjectId());

        ClassRoom saved = classRoomRepository.save(room);
        return mapper.toResponse(saved, subjectNameOf(saved.getSubjectId()));
    }

    /**
     * حذف نرم کلاس.
     *
     * <p>⚠️ حذف نرم و نه واقعی: نمره‌ها، تکالیف و آزمون‌های این کلاس در
     * فازهای بعدی به همین شناسه گره می‌خورند. حذف واقعی یعنی از دست دادن
     * آن‌ها — و معلمی که «کلاس را حذف می‌کند» انتظار ندارد کارنامه
     * سال قبل هم برود.
     */
    @Transactional
    public void delete(UUID id) {
        ClassRoom room = requireAccessible(id);
        room.softDelete();
        classRoomRepository.save(room);
    }

    // ─────────────────────────── کمکی ───────────────────────────

    /**
     * آیا کاربر جاری همه کلاس‌های مستأجر را می‌بیند؟
     *
     * <p>مالک و مدیر مدرسه: بله. معلم و دستیار: نه — فقط کلاس‌های خودشان.
     *
     * <p>⚠️ این متد نقش‌های <i>سطح پلتفرم</i> را نمی‌خواند. یک
     * {@code SUPER_ADMIN} که عضو هیچ مستأجری نیست، از
     * {@code @PreAuthorize} رد می‌شود و به اینجا نمی‌رسد. عبور او از مرز
     * مستأجر باید در کد صریح و قابل‌شمردن باشد، نه پنهان در یک شرط اینجا.
     */
    private boolean canManageAllClasses() {
        TenantPrincipal principal = TenantContext.requirePrincipal();
        return principal.hasTenantRole(MemberRole.OWNER)
                || principal.hasTenantRole(MemberRole.SCHOOL_ADMIN);
    }

    /**
     * اتصال کلاس به درس، با بررسی تعلق درس به همین مستأجر.
     *
     * <p>⚠️ بررسی مالکیت درس صریح نیست چون لازم نیست: فیلتر خودکار
     * {@code @TenantId} باعث می‌شود {@code findByIdAndDeletedAtIsNull}
     * برای درس مستأجر دیگر <b>هیچ</b> نتیجه‌ای ندهد. لایه سوم هم در
     * دیتابیس است: کلید خارجی ترکیبی
     * {@code (tenant_id, subject_id) → subjects(tenant_id, id)}.
     *
     * <p>⚠️ پیام «درس یافت نشد» هم برای «وجود ندارد» و هم برای «مال
     * مستأجر دیگر است» یکسان است — همان قاعده‌ای که نمی‌گذارد مهاجم با
     * شمردن پاسخ‌ها فهرست شناسه‌های معتبر بسازد.
     */
    private void applySubject(ClassRoom room, UUID subjectId) {
        if (subjectId == null) {
            room.setSubjectId(null);
            return;
        }

        Subject subject = subjectRepository.findByIdAndDeletedAtIsNull(subjectId).orElse(null);
        if (subject == null) {
            throw ResourceNotFoundException.of("درس");
        }
        room.setSubjectId(subject.getId());
    }

    /**
     * بررسی تعارض نام — آینه‌ی دقیق ایندکس {@code uk_classrooms_name_year}.
     *
     * <p>این بررسی پیام فارسی می‌دهد؛ ایندکس دیتابیس تضمین می‌کند در
     * مسابقه همزمانی دو درخواست، داده خراب نشود. هر دو لازم‌اند: بررسی
     * سرویس بدون ایندکس، در مسابقه می‌بازد؛ ایندکس بدون بررسی، کاربر را
     * با پیام مبهم «تعارض داده» تنها می‌گذارد.
     *
     * @param selfId شناسه خود رکورد در حال ویرایش؛ {@code null} در ساخت
     */
    private void requireNoNameConflict(String name, String academicYear, UUID selfId) {
        boolean conflict = classRoomRepository.findConflictingIds(name, academicYear)
                .stream()
                .anyMatch(found -> !found.equals(selfId));
        if (conflict) {
            throw DuplicateResourceException.of("کلاس", "نام در این سال تحصیلی");
        }
    }

    /** نام درس یک کلاس، با یک کوئری تک. */
    private String subjectNameOf(UUID subjectId) {
        if (subjectId == null) {
            return null;
        }
        return subjectRepository.findByIdAndDeletedAtIsNull(subjectId)
                .map(Subject::getName)
                .orElse(null);
    }

    /** نام درس‌های یک صفحه، با یک کوئری دسته‌ای — ضد N+1. */
    private Map<UUID, String> subjectNamesOf(List<ClassRoom> rooms) {
        List<UUID> subjectIds = rooms.stream()
                .map(ClassRoom::getSubjectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (subjectIds.isEmpty()) {
            return Map.of();
        }

        return subjectRepository.findByIdInAndDeletedAtIsNull(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));
    }

    private static String subjectNameFrom(ClassRoom room, Map<UUID, String> subjectNames) {
        return (room.getSubjectId() == null) ? null : subjectNames.get(room.getSubjectId());
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
