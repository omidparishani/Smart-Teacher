package ir.omid.smartteacher.student.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.api.PageableFactory;
import ir.omid.smartteacher.student.internal.api.dto.CreateStudentRequest;
import ir.omid.smartteacher.student.internal.api.dto.StudentResponse;
import ir.omid.smartteacher.student.internal.api.dto.UpdateStudentRequest;
import ir.omid.smartteacher.student.internal.application.StudentService;
import ir.omid.smartteacher.student.internal.domain.StudentStatus;
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
 * مسیرهای دانش‌آموزان — {@code /api/v1/students}، مطابق
 * {@code docs/api.md} بخش ۴.۳.
 *
 * <p><b>⚠️ تفاوت با {@code ClassRoomController}:</b> اینجا تنها لایه
 * کنترل دسترسی همان {@code @PreAuthorize} است. هیچ قاعده مالکیت
 * معلمی وجود ندارد، چون دانش‌آموز دارایی مدرسه است نه معلم. دلیل کامل
 * در Javadoc {@code StudentService}.
 *
 * <p>⚠️ <b>مسیرهای {@code /students/{id}/performance} و
 * {@code /students/{id}/submissions}}</b> در {@code docs/api.md} فهرست
 * شده‌اند ولی اینجا نیستند: آن‌ها به نمره و تکلیف نیاز دارند که فازهای
 * ۷ و ۸ می‌آورند. ساختنشان حالا یعنی برگرداندن داده خالی — و
 * «پیاده‌شده» علامت‌خوردن یک Endpoint توخالی بدتر از باز بودنش است.
 *
 * <p>⚠️ {@code STUDENT(خود)} در ستون دسترسی {@code GET /students/{id}}
 * هنوز اعمال نشده: ورود دانش‌آموز و اتصال حسابش به رکورد دانش‌آموز در
 * فاز بعد می‌آید. تا آن روز این مسیر فقط برای کارمندان است.
 */
@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "دانش‌آموزان", description = "مدیریت دانش‌آموزان مستأجر")
public class StudentController {

    /**
     * نقش‌های کارمندی که به دانش‌آموزان دسترسی دارند.
     *
     * <p>⚠️ عیناً همان رشته {@code SubjectController.STAFF_ONLY} است.
     * تکرار شده و نه import: آن کلاس در ماژول {@code classroom} است و
     * {@code classroom} به {@code student} وابسته نیست؛ اگر اینجا از
     * آن استفاده می‌شد، جهت وابستگی برعکس می‌شد و یک وابستگی
     * دومسیره می‌ساخت که مدولیت ردش می‌کند.
     *
     * <p>⚠️ {@code ASSISTANT} عمداً غایب است — {@code docs/api.md} بخش
     * ۴.۳ ستون دسترسی را {@code TEACHER} گذاشته.
     */
    static final String STAFF_ONLY =
            "hasAnyAuthority('TENANT_OWNER','TENANT_SCHOOL_ADMIN','TENANT_TEACHER')";

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    /** فهرست صفحه‌بندی‌شده دانش‌آموزان. */
    @GetMapping
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "فهرست دانش‌آموزان",
            description = "دانش‌آموزان مستأجر جاری را صفحه‌بندی‌شده برمی‌گرداند.")
    public PageResponse<StudentResponse> list(
            @Parameter(description = "شماره صفحه، از صفر")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "اندازه صفحه؛ بالای ۱۰۰ کلمپ می‌شود")
            @RequestParam(defaultValue = "" + PageableFactory.DEFAULT_SIZE) int size,

            @Parameter(description = "مرتب‌سازی به شکل field,direction")
            @RequestParam(required = false) String sort,

            @Parameter(description = "جست‌وجو در نام دانش‌آموز")
            @RequestParam(required = false) String search,

            @Parameter(description = "فیلتر وضعیت: ACTIVE، INACTIVE یا GRADUATED")
            @RequestParam(required = false) StudentStatus status) {

        return studentService.list(page, size, sort, search, status);
    }

    /** یک دانش‌آموز مشخص. */
    @GetMapping("/{id}")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "دریافت دانش‌آموز", description = "یک دانش‌آموز با شناسه.")
    public StudentResponse get(@PathVariable UUID id) {
        return studentService.get(id);
    }

    /** ساخت دانش‌آموز تازه. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ساخت دانش‌آموز",
            description = "کد ملی، اگر داده شود، باید معتبر و در مستأجر یکتا باشد.")
    public StudentResponse create(@Valid @RequestBody CreateStudentRequest request) {
        return studentService.create(request);
    }

    /** ویرایش کامل دانش‌آموز. */
    @PutMapping("/{id}")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ویرایش دانش‌آموز", description = "جایگزینی کامل فیلدهای دانش‌آموز.")
    public StudentResponse update(@PathVariable UUID id,
                                  @Valid @RequestBody UpdateStudentRequest request) {
        return studentService.update(id, request);
    }

    /** حذف نرم دانش‌آموز. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "حذف دانش‌آموز",
            description = "حذف نرم — سوابق و ثبت‌نام‌ها باقی می‌مانند.")
    public void delete(@PathVariable UUID id) {
        studentService.delete(id);
    }
}
