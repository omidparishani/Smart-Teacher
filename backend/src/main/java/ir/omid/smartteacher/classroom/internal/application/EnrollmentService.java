package ir.omid.smartteacher.classroom.internal.application;

import ir.omid.smartteacher.classroom.internal.api.dto.ClassDashboardResponse;
import ir.omid.smartteacher.classroom.internal.api.dto.ClassStudentResponse;
import ir.omid.smartteacher.classroom.internal.api.dto.EnrollStudentRequest;
import ir.omid.smartteacher.classroom.internal.domain.ClassRoom;
import ir.omid.smartteacher.classroom.internal.domain.ClassStudent;
import ir.omid.smartteacher.classroom.internal.domain.ClassStudentRepository;
import ir.omid.smartteacher.classroom.internal.domain.Subject;
import ir.omid.smartteacher.classroom.internal.domain.SubjectRepository;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.DuplicateResourceException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.student.StudentDirectory;
import ir.omid.smartteacher.student.StudentDirectory.StudentSummary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ثبت‌نام دانش‌آموز در کلاس و داشبورد کلاس.
 *
 * <p><b>🔴 قاعده دسترسی:</b> همان {@link ClassRoomService#requireAccessible}
 * — معلم فقط کلاس خودش، مالک/مدیر همه کلاس‌ها. این سرویس قاعده تازه‌ای
 * نمی‌سازد؛ فقط همان را صدا می‌زند. دو قاعده یعنی روزی یکی عوض می‌شود
 * و دیگری فراموش.
 *
 * <p><b>⚠️ وابستگی به {@link StudentDirectory}:</b> این ماژول جدول
 * {@code students} را نمی‌خواند. نام و اعتبار دانش‌آموز از Facade ماژول
 * {@code student} می‌آید — قاعده ۴ معماری.
 *
 * <p><b>⚠️ صفحه‌بندی فهرست دانش‌آموزان در حافظه است</b> — نه در دیتابیس.
 * دلیلش این است که دانش‌آموز حذف‌نرم‌شده باید از فهرست حذف شود، ولی ردیف
 * ثبت‌نامش در {@code class_students} می‌ماند؛ فیلتر روی جدول دیگر فقط با
 * join بین‌ماژولی ممکن است که ممنوع است. اندازه کلاس‌های واقعی ایران
 * معمولاً زیر ۵۰ است، پس خواندن همه ثبت‌نام‌ها و صفحه‌بندی پس از فیلتر
 * قابل قبول است. اگر روزی کلاس‌های چندصدنفری آمد، نقطه تغییر همین متد
 * {@link #listStudents} است.
 */
@Service
public class EnrollmentService {

    private final ClassRoomService classRoomService;
    private final ClassStudentRepository classStudentRepository;
    private final SubjectRepository subjectRepository;
    private final StudentDirectory studentDirectory;

    public EnrollmentService(ClassRoomService classRoomService,
                             ClassStudentRepository classStudentRepository,
                             SubjectRepository subjectRepository,
                             StudentDirectory studentDirectory) {
        this.classRoomService = classRoomService;
        this.classStudentRepository = classStudentRepository;
        this.subjectRepository = subjectRepository;
        this.studentDirectory = studentDirectory;
    }

    /**
     * فهرست دانش‌آموزان فعال یک کلاس — صفحه‌بندی‌شده.
     *
     * <p>⚠️ {@code totalElements} تعداد دانش‌آموزان <i>قابل‌نمایش</i>
     * است، نه تعداد ردیف‌های خام {@code class_students}. دانش‌آموز
     * حذف‌نرم‌شده شمرده نمی‌شود.
     */
    @Transactional(readOnly = true)
    public PageResponse<ClassStudentResponse> listStudents(UUID classId, int page, int size) {
        classRoomService.requireAccessible(classId);

        List<ClassStudentResponse> visible = visibleEnrollments(classId);

        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, 100);
        int from = Math.min(safePage * safeSize, visible.size());
        int to = Math.min(from + safeSize, visible.size());

        List<ClassStudentResponse> content = visible.subList(from, to);
        long total = visible.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);

        return new PageResponse<>(
                content,
                safePage,
                safeSize,
                total,
                totalPages,
                safePage == 0,
                totalPages == 0 || safePage >= totalPages - 1);
    }

    /**
     * ثبت‌نام دانش‌آموز در کلاس.
     *
     * <p>سه ردّ ممکن، هر کدام با کد متفاوت:
     * <ul>
     *   <li>دانش‌آموز وجود ندارد / مال مستأجر دیگر / حذف‌شده → ۴۰۴</li>
     *   <li>قبلاً در همین کلاس ثبت‌نام شده → ۴۰۹ تکراری</li>
     *   <li>ظرفیت کلاس پر است → ۴۰۹ تعارض</li>
     * </ul>
     */
    @Transactional
    public ClassStudentResponse enroll(UUID classId, EnrollStudentRequest request) {
        ClassRoom room = classRoomService.requireAccessible(classId);

        UUID studentId = request.studentId();
        Set<UUID> active = studentDirectory.activeIdsAmong(List.of(studentId));
        if (!active.contains(studentId)) {
            // ⚠️ پیام عامدانه «دانش‌آموز» می‌گوید و نه «یافت نشد عمومی»:
            // اینجا شناسه از بدنه آمده و مهاجم خودش فرستاده؛ لو رفتن
            // «وجود ندارد» اطلاعات تازه‌ای به او نمی‌دهد. ولی همچنان
            // نمی‌گوییم «مال مستأجر دیگر است» تا تفاوت دو حالت لو نرود.
            throw ResourceNotFoundException.of("دانش‌آموز");
        }

        if (classStudentRepository.existsByClassroomIdAndStudentId(classId, studentId)) {
            throw DuplicateResourceException.of("ثبت‌نام", "دانش‌آموز در این کلاس");
        }

        requireCapacityAvailable(room, classId);

        ClassStudent enrollment = new ClassStudent(classId, studentId);
        enrollment.setRollNumber(trimToNull(request.rollNumber()));
        ClassStudent saved = classStudentRepository.save(enrollment);

        StudentSummary summary = studentDirectory.summariesOf(List.of(studentId)).get(studentId);
        // summary نباید null باشد — همین الان activeIdsAmong تأیید کرد.
        // اگر بود، یعنی بین دو فراخوانی حذف شده؛ ۴۰۴ امن‌تر از NPE است.
        if (summary == null) {
            throw ResourceNotFoundException.of("دانش‌آموز");
        }

        return toResponse(saved, summary);
    }

    /**
     * لغو ثبت‌نام — حذف واقعی ردیف، نه نرم.
     *
     * <p>اگر ثبت‌نامی نباشد، ۴۰۴. عمداً idempotent نیست: دو بار حذف
     * یعنی بار دوم «چیزی برای حذف نبود» و باید گفته شود، وگرنه کلاینت
     * فکر می‌کند هنوز عضویت بوده.
     */
    @Transactional
    public void unenroll(UUID classId, UUID studentId) {
        classRoomService.requireAccessible(classId);

        ClassStudent enrollment = classStudentRepository
                .findByClassroomIdAndStudentId(classId, studentId)
                .orElseThrow(() -> ResourceNotFoundException.of("ثبت‌نام"));

        classStudentRepository.delete(enrollment);
    }

    /**
     * داشبورد کلاس — خلاصه وضعیت فعلی بدون داده فازهای بعدی.
     */
    @Transactional(readOnly = true)
    public ClassDashboardResponse dashboard(UUID classId) {
        ClassRoom room = classRoomService.requireAccessible(classId);
        long enrolled = visibleEnrollments(classId).size();

        Integer seatsRemaining = null;
        if (room.getCapacity() != null) {
            seatsRemaining = Math.max(0, room.getCapacity() - (int) enrolled);
        }

        return new ClassDashboardResponse(
                room.getId(),
                room.getName(),
                room.getGradeLevel(),
                room.getAcademicYear(),
                room.getSubjectId(),
                subjectNameOf(room.getSubjectId()),
                room.getStatus(),
                room.getCapacity(),
                enrolled,
                seatsRemaining,
                room.getCreatedAt(),
                room.getUpdatedAt());
    }

    // ─────────────────────────── کمکی ───────────────────────────

    /**
     * ثبت‌نام‌های قابل‌نمایش یک کلاس — دانش‌آموزان حذف‌نرم‌شده غایب‌اند.
     */
    private List<ClassStudentResponse> visibleEnrollments(UUID classId) {
        List<ClassStudent> enrollments =
                classStudentRepository.findByClassroomIdOrderByEnrolledAtAsc(classId);

        if (enrollments.isEmpty()) {
            return List.of();
        }

        List<UUID> studentIds = enrollments.stream().map(ClassStudent::getStudentId).toList();
        Map<UUID, StudentSummary> summaries = studentDirectory.summariesOf(studentIds);

        return enrollments.stream()
                .filter(e -> summaries.containsKey(e.getStudentId()))
                .map(e -> toResponse(e, summaries.get(e.getStudentId())))
                .toList();
    }

    /**
     * ردّ ثبت‌نام وقتی ظرفیت پر است.
     *
     * <p>⚠️ ظرفیت بر اساس دانش‌آموزان <i>قابل‌نمایش</i> سنجیده می‌شود،
     * نه ردیف‌های خام. دانش‌آموز حذف‌نرم‌شده صندلی را اشغال نمی‌کند —
     * وگرنه معلم بعد از حذف اشتباهی، برای همیشه به سقف می‌خورد بدون
     * اینکه بداند چرا.
     */
    private void requireCapacityAvailable(ClassRoom room, UUID classId) {
        if (room.getCapacity() == null) {
            return;
        }
        long enrolled = visibleEnrollments(classId).size();
        if (enrolled >= room.getCapacity()) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT,
                    "ظرفیت این کلاس (" + room.getCapacity() + " نفر) تکمیل شده است.");
        }
    }

    private String subjectNameOf(UUID subjectId) {
        if (subjectId == null) {
            return null;
        }
        return subjectRepository.findByIdAndDeletedAtIsNull(subjectId)
                .map(Subject::getName)
                .orElse(null);
    }

    private static ClassStudentResponse toResponse(ClassStudent enrollment, StudentSummary summary) {
        return new ClassStudentResponse(
                summary.id(),
                summary.fullName(),
                summary.nationalCode(),
                enrollment.getRollNumber(),
                enrollment.getEnrolledAt());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
