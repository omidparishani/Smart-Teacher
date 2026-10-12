package ir.omid.smartteacher.classroom.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.classroom.internal.api.dto.CreateSubjectRequest;
import ir.omid.smartteacher.classroom.internal.api.dto.SubjectResponse;
import ir.omid.smartteacher.classroom.internal.api.dto.UpdateSubjectRequest;
import ir.omid.smartteacher.classroom.internal.application.SubjectService;
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
 * مسیرهای درس‌ها — {@code /api/v1/subjects}.
 *
 * <p><b>⚠️ این مسیرها در {@code docs/api.md} بخش ۴ فهرست نشده‌اند.</b>
 * انحراف آگاهانه است، نه فراموشی: ERD ({@code docs/database.md} بخش ۳)
 * جدول {@code subjects} را الزام کرده و {@code classrooms.subject_id}
 * بدون راهی برای ساختن درس، عملاً قابل استفاده نیست. این افزوده در
 * {@code docs/api.md} بخش ۴.۲.۱ و در {@code docs/verification-log.md}
 * ثبت شده است.
 *
 * <p><b>⚠️ چرا مسئولیت نوشتن درس به معلم هم داده شده؟</b> چون در این
 * محصول، اکثر مستأجرها یک معلم تنها هستند: نخستین ورود هر کاربر یک
 * مستأجر شخصی می‌سازد که خودش {@code OWNER} آن است
 * ({@code EitaaAccountProvisioner}). اگر ساخت درس را به مدیر مدرسه
 * محدود می‌کردیم، معلمِ تنها هیچ راهی برای ساختن درس نداشت و کل فاز ۳
 * برای او بی‌استفاده می‌شد.
 *
 * <p><b>⚠️ این کلاس هیچ منطقی ندارد و نباید داشته باشد</b> — همان قاعده
 * {@code EitaaAuthController}.
 */
@RestController
@RequestMapping("/api/v1/subjects")
@Tag(name = "درس‌ها", description = "فهرست دروس مستأجر — پیش‌نیاز اتصال کلاس به درس")
public class SubjectController {

    /**
     * نقش‌های کارمندی که به درس‌ها دسترسی دارند.
     *
     * <p><b>⚠️ نقش‌های سطح پلتفرم اینجا نیستند و عمدی است.</b> یک
     * {@code SUPER_ADMIN} که عضو هیچ مستأجری نیست، مستأجری هم در زمینه
     * ندارد؛ اگر او را راه می‌دادیم، کوئری‌اش روی دامنه پلتفرم اجرا
     * می‌شد و به‌جای خطا، فهرست خالی می‌گرفت. عبور سوپر‌ادمین از مرز
     * مستأجر باید در کد صریح و قابل‌شمردن باشد، نه با یک مجوز کلی.
     *
     * <p>⚠️ {@code ASSISTANT} عمداً غایب است: {@code docs/api.md} بخش
     * ۴.۲ ستون دسترسی را {@code TEACHER} گذاشته و دستیار در آن نیست.
     * اگر روزی خواستید اضافه کنید، جایش همین رشته است — ولی آن تغییر
     * باید آگاهانه باشد، نه نتیجه یک wildcard.
     */
    static final String STAFF_ONLY =
            "hasAnyAuthority('TENANT_OWNER','TENANT_SCHOOL_ADMIN','TENANT_TEACHER')";

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    /**
     * فهرست صفحه‌بندی‌شده درس‌ها.
     *
     * <p>⚠️ اندازه پیش‌فرض از {@link PageableFactory#DEFAULT_SIZE} خوانده
     * می‌شود و نه یک عدد تازه. مقدار پیش‌فرض در annotation باید ثابت
     * زمان کامپایل باشد، و این الحاق رشته هم ثابت است — پس یک منبع
     * حقیقت می‌ماند.
     */
    @GetMapping
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "فهرست درس‌ها",
            description = "دروس مستأجر جاری را صفحه‌بندی‌شده برمی‌گرداند.")
    public PageResponse<SubjectResponse> list(
            @Parameter(description = "شماره صفحه، از صفر")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "اندازه صفحه؛ بالای ۱۰۰ کلمپ می‌شود")
            @RequestParam(defaultValue = "" + PageableFactory.DEFAULT_SIZE) int size,

            @Parameter(description = "مرتب‌سازی به شکل field,direction")
            @RequestParam(required = false) String sort,

            @Parameter(description = "جست‌وجو در نام درس")
            @RequestParam(required = false) String search) {

        return subjectService.list(page, size, sort, search);
    }

    /** ساخت درس تازه. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ساخت درس",
            description = "درس تازه در مستأجر جاری می‌سازد. کد درس، اگر داده شود، "
                    + "نباید تکراری باشد.")
    public SubjectResponse create(@Valid @RequestBody CreateSubjectRequest request) {
        return subjectService.create(request);
    }

    /** یک درس مشخص. */
    @GetMapping("/{id}")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "دریافت درس", description = "یک درس با شناسه.")
    public SubjectResponse get(@PathVariable UUID id) {
        return subjectService.get(id);
    }

    /** ویرایش کامل درس. */
    @PutMapping("/{id}")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ویرایش درس", description = "جایگزینی کامل فیلدهای درس.")
    public SubjectResponse update(@PathVariable UUID id,
                                  @Valid @RequestBody UpdateSubjectRequest request) {
        return subjectService.update(id, request);
    }

    /**
     * حذف نرم درس.
     *
     * <p>⚠️ اگر کلاسی به این درس وصل باشد، {@code 409} برمی‌گردد؛ دلیلش
     * در {@code SubjectService.delete} آمده است.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "حذف درس",
            description = "اگر کلاسی به این درس متصل باشد، پاسخ ۴۰۹ است.")
    public void delete(@PathVariable UUID id) {
        subjectService.delete(id);
    }
}
