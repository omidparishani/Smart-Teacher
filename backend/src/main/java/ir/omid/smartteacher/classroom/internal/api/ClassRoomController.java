package ir.omid.smartteacher.classroom.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.classroom.internal.api.dto.ClassDashboardResponse;
import ir.omid.smartteacher.classroom.internal.api.dto.ClassRoomResponse;
import ir.omid.smartteacher.classroom.internal.api.dto.ClassStudentResponse;
import ir.omid.smartteacher.classroom.internal.api.dto.CreateClassRoomRequest;
import ir.omid.smartteacher.classroom.internal.api.dto.EnrollStudentRequest;
import ir.omid.smartteacher.classroom.internal.api.dto.UpdateClassRoomRequest;
import ir.omid.smartteacher.classroom.internal.application.ClassRoomService;
import ir.omid.smartteacher.classroom.internal.application.EnrollmentService;
import ir.omid.smartteacher.classroom.internal.domain.ClassRoomStatus;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.api.PageableFactory;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * مسیرهای کلاس‌ها — {@code /api/v1/classes}، مطابق {@code docs/api.md}
 * بخش ۴.۲ (شامل ثبت‌نام و داشبورد — گام ۲ فاز ۳).
 *
 * <p><b>🔴 تفاوت مهم این کلاس با {@link SubjectController}:</b> اینجا
 * {@code @PreAuthorize} تنها <i>نیمی</i> از کنترل دسترسی است. او فقط
 * می‌گوید «آیا این کاربر کارمند مستأجر است؟». اینکه «آیا این کلاس مال
 * خودش است؟» را {@link ClassRoomService#requireAccessible} می‌گوید، چون
 * برای پاسخ‌دادنش باید ابتدا رکورد خوانده شود.
 *
 * <p>پیامدش برای مصرف‌کننده API این است که در این مسیرها <b>هر دو</b>
 * کد ۴۰۳ و ۴۰۴ ممکن است برگردد، و تفاوتشان معنادار است:
 * ۴۰۳ یعنی «شما کارمند این مدرسه نیستید»، ۴۰۴ یعنی «این کلاس یا وجود
 * ندارد یا مال شما نیست». دومی عمداً یکسان است تا کسی با شمردن
 * پاسخ‌ها نفهمد کدام شناسه واقعی است.
 */
@RestController
@RequestMapping("/api/v1/classes")
@Tag(name = "کلاس‌ها", description = "مدیریت کلاس‌های مستأجر — محدود به کلاس‌های خود معلم")
public class ClassRoomController {

    /**
     * نقش‌های کارمندی که به کلاس‌ها دسترسی دارند.
     *
     * <p>همان رشته {@link SubjectController} — یک تعریف، دو مصرف. اگر
     * روزی سطوح دسترسی این دو از هم جدا شوند، اینجا باید به دو رشته
     * تبدیل شود؛ تا آن روز، یکی نگه‌داشتنشان از واگرایی بی‌صدا جلوگیری
     * می‌کند.
     */
    static final String STAFF_ONLY = SubjectController.STAFF_ONLY;

    private final ClassRoomService classRoomService;
    private final EnrollmentService enrollmentService;

    public ClassRoomController(ClassRoomService classRoomService,
                               EnrollmentService enrollmentService) {
        this.classRoomService = classRoomService;
        this.enrollmentService = enrollmentService;
    }

    /**
     * فهرست صفحه‌بندی‌شده کلاس‌ها.
     *
     * <p><b>⚠️ نتیجه برای معلم فیلتر می‌شود، ولی هیچ پارامتری برای
     * انتخاب معلم وجود ندارد.</b> این عمدی است: اگر
     * {@code ?teacherId=...} می‌پذیرفتیم، هر معلمی می‌توانست کلاس‌های
     * همکارش را بخواند و محدودیت سرویس را خودش دور بزند. فیلتر از
     * هویت توکن می‌آید، نه از درخواست.
     */
    @GetMapping
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "فهرست کلاس‌ها",
            description = "مالک و مدیر مدرسه همه کلاس‌های مستأجر را می‌بینند؛ "
                    + "معلم فقط کلاس‌هایی را که سرکلاسشان است.")
    public PageResponse<ClassRoomResponse> list(
            @Parameter(description = "شماره صفحه، از صفر")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "اندازه صفحه؛ بالای ۱۰۰ کلمپ می‌شود")
            @RequestParam(defaultValue = "" + PageableFactory.DEFAULT_SIZE) int size,

            @Parameter(description = "مرتب‌سازی به شکل field,direction")
            @RequestParam(required = false) String sort,

            @Parameter(description = "جست‌وجو در نام کلاس")
            @RequestParam(required = false) String search,

            @Parameter(description = "فیلتر وضعیت: ACTIVE یا ARCHIVED")
            @RequestParam(required = false) ClassRoomStatus status) {

        return classRoomService.list(page, size, sort, search, status);
    }

    /** یک کلاس مشخص. */
    @GetMapping("/{id}")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "دریافت کلاس",
            description = "کلاسی که کاربر حق دیدنش را دارد؛ در غیر این صورت ۴۰۴.")
    public ClassRoomResponse get(@PathVariable UUID id) {
        return classRoomService.get(id);
    }

    /**
     * ساخت کلاس تازه.
     *
     * <p>⚠️ معلم سرکلاس از بدنه درخواست خوانده <b>نمی‌شود</b>؛ همان
     * کاربر جاری است. دلیلش در Javadoc {@link CreateClassRoomRequest}.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ساخت کلاس",
            description = "کلاس تازه می‌سازد و معلم سرکلاس را همان کاربر جاری می‌گذارد.")
    public ClassRoomResponse create(@Valid @RequestBody CreateClassRoomRequest request) {
        return classRoomService.create(request);
    }

    /** ویرایش کامل کلاس، شامل وضعیت. */
    @PutMapping("/{id}")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ویرایش کلاس", description = "جایگزینی کامل فیلدهای کلاس.")
    public ClassRoomResponse update(@PathVariable UUID id,
                                    @Valid @RequestBody UpdateClassRoomRequest request) {
        return classRoomService.update(id, request);
    }

    /** حذف نرم کلاس. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "حذف کلاس",
            description = "حذف نرم — داده‌های وابسته در فازهای بعدی حفظ می‌شوند.")
    public void delete(@PathVariable UUID id) {
        classRoomService.delete(id);
    }

    // ─────────────────── ثبت‌نام و داشبورد (گام ۲) ───────────────────

    /** فهرست دانش‌آموزان ثبت‌نام‌شده در کلاس. */
    @GetMapping("/{id}/students")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "فهرست دانش‌آموزان کلاس",
            description = "دانش‌آموزان فعال ثبت‌نام‌شده؛ دانش‌آموز حذف‌نرم‌شده غایب است.")
    public PageResponse<ClassStudentResponse> listStudents(
            @PathVariable UUID id,
            @Parameter(description = "شماره صفحه، از صفر")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "اندازه صفحه؛ بالای ۱۰۰ کلمپ می‌شود")
            @RequestParam(defaultValue = "" + PageableFactory.DEFAULT_SIZE) int size) {
        return enrollmentService.listStudents(id, page, size);
    }

    /** ثبت‌نام دانش‌آموز در کلاس. */
    @PostMapping("/{id}/students")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ثبت‌نام دانش‌آموز در کلاس",
            description = "دانش‌آموز باید از قبل در مدرسه ساخته شده باشد. ظرفیت پر → ۴۰۹.")
    public ClassStudentResponse enroll(@PathVariable UUID id,
                                       @Valid @RequestBody EnrollStudentRequest request) {
        return enrollmentService.enroll(id, request);
    }

    /** لغو ثبت‌نام دانش‌آموز از کلاس. */
    @DeleteMapping("/{id}/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "لغو ثبت‌نام",
            description = "حذف واقعی ردیف ثبت‌نام — نه حذف نرم.")
    public void unenroll(@PathVariable UUID id, @PathVariable UUID studentId) {
        enrollmentService.unenroll(id, studentId);
    }

    /** داشبورد خلاصه کلاس. */
    @GetMapping("/{id}/dashboard")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "داشبورد کلاس",
            description = "خلاصه کلاس و شمار دانش‌آموزان. تکالیف و نمرات در فازهای بعد می‌آیند.")
    public ClassDashboardResponse dashboard(@PathVariable UUID id) {
        return enrollmentService.dashboard(id);
    }
}
