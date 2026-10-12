package ir.omid.smartteacher.assignment.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.assignment.internal.api.dto.AssignmentResponse;
import ir.omid.smartteacher.assignment.internal.api.dto.CreateAssignmentRequest;
import ir.omid.smartteacher.assignment.internal.api.dto.CreateSubmissionRequest;
import ir.omid.smartteacher.assignment.internal.api.dto.EvaluationResponse;
import ir.omid.smartteacher.assignment.internal.api.dto.PublishAssignmentRequest;
import ir.omid.smartteacher.assignment.internal.api.dto.ReviewSubmissionRequest;
import ir.omid.smartteacher.assignment.internal.api.dto.SubmissionResponse;
import ir.omid.smartteacher.assignment.internal.api.dto.TeacherReviewResponse;
import ir.omid.smartteacher.assignment.internal.api.dto.UpdateAssignmentRequest;
import ir.omid.smartteacher.assignment.internal.application.AssignmentService;
import ir.omid.smartteacher.assignment.internal.domain.AssignmentStatus;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.api.PageableFactory;
import jakarta.validation.Valid;
import java.util.List;
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
 * مسیرهای تکلیف — {@code /api/v1/assignments} مطابق {@code docs/api.md} بخش ۴.۵.
 *
 * <p>گام ۱ فاز ۷: CRUD + انتشار + ثبت پاسخ متنی از Mini App.
 * گام ۲: تصحیح AI، بازبینی معلم، reevaluate-all، اعلان Mock ایتا.
 *
 * <p><b>⚠️ مسیر ارسال پاسخ</b> {@code POST .../submissions} است (نه
 * {@code /submit} در نسخه قدیمی API) — پاسخ فقط از Mini App می‌آید، نه
 * از چت ایتا.
 */
@RestController
@RequestMapping("/api/v1/assignments")
@Tag(name = "تکالیف", description = "تکلیف و ارسال پاسخ — متعلق به کلاس مستأجر")
public class AssignmentController {

    static final String STAFF_ONLY =
            "hasAnyAuthority('TENANT_OWNER','TENANT_SCHOOL_ADMIN','TENANT_TEACHER','TENANT_ASSISTANT')";

    static final String STAFF_OR_STUDENT =
            "hasAnyAuthority('TENANT_OWNER','TENANT_SCHOOL_ADMIN','TENANT_TEACHER',"
                    + "'TENANT_ASSISTANT','TENANT_STUDENT')";

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "فهرست تکالیف")
    public PageResponse<AssignmentResponse> list(
            @Parameter(description = "شماره صفحه، از صفر")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "اندازه صفحه")
            @RequestParam(defaultValue = "" + PageableFactory.DEFAULT_SIZE) int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(required = false) UUID classroomId) {
        return assignmentService.list(page, size, sort, search, status, classroomId);
    }

    @GetMapping("/{id}")
    @PreAuthorize(STAFF_OR_STUDENT)
    @Operation(summary = "دریافت تکلیف")
    public AssignmentResponse get(@PathVariable UUID id) {
        return assignmentService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ساخت تکلیف")
    public AssignmentResponse create(@Valid @RequestBody CreateAssignmentRequest request) {
        return assignmentService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ویرایش تکلیف")
    public AssignmentResponse update(@PathVariable UUID id,
                                     @Valid @RequestBody UpdateAssignmentRequest request) {
        return assignmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "حذف نرم تکلیف")
    public void delete(@PathVariable UUID id) {
        assignmentService.delete(id);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "انتشار تکلیف",
            description = "وضعیت را PUBLISHED می‌کند و در صورت وجود chatId به ایتا می‌فرستد. "
                    + "شکست ارسال، انتشار را باطل نمی‌کند.")
    public AssignmentResponse publish(
            @PathVariable UUID id,
            @RequestBody(required = false) @Valid PublishAssignmentRequest request) {
        return assignmentService.publish(id,
                request != null ? request : new PublishAssignmentRequest(null));
    }

    @GetMapping("/{id}/submissions")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "فهرست ارسال‌های یک تکلیف")
    public List<SubmissionResponse> listSubmissions(@PathVariable UUID id) {
        return assignmentService.listSubmissions(id);
    }

    @PostMapping("/{id}/submissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_OR_STUDENT)
    @Operation(summary = "ثبت پاسخ متنی",
            description = "مسیر Mini App — نه دریافت از چت ایتا.")
    public SubmissionResponse submit(@PathVariable UUID id,
                                     @Valid @RequestBody CreateSubmissionRequest request) {
        return assignmentService.submit(id, request);
    }

    @GetMapping("/submissions/{submissionId}")
    @PreAuthorize(STAFF_OR_STUDENT)
    @Operation(summary = "یک ارسال",
            description = "معلم: همه ارسال‌های کلاس خودش. دانش‌آموز: فقط مال خودش.")
    public SubmissionResponse getSubmission(@PathVariable UUID submissionId) {
        return assignmentService.getSubmission(submissionId);
    }

    @PostMapping("/{id}/submissions/{submissionId}/evaluate")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "تصحیح AI یک ارسال",
            description = "با Mock AI نمره ساختاریافته می‌سازد و در ai_evaluations ذخیره می‌کند.")
    public EvaluationResponse evaluate(@PathVariable UUID id,
                                       @PathVariable UUID submissionId) {
        return assignmentService.evaluate(id, submissionId);
    }

    @GetMapping("/{id}/submissions/{submissionId}/evaluation")
    @PreAuthorize(STAFF_OR_STUDENT)
    @Operation(summary = "مشاهده ارزیابی AI")
    public EvaluationResponse getEvaluation(@PathVariable UUID id,
                                            @PathVariable UUID submissionId) {
        return assignmentService.getEvaluation(id, submissionId);
    }

    @PostMapping("/{id}/submissions/{submissionId}/review")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "بازبینی معلم",
            description = "لاگ Append-Only در teacher_reviews + به‌روزرسانی نمره نهایی.")
    public TeacherReviewResponse review(
            @PathVariable UUID id,
            @PathVariable UUID submissionId,
            @Valid @RequestBody ReviewSubmissionRequest request) {
        return assignmentService.review(id, submissionId, request);
    }

    @GetMapping("/{id}/submissions/{submissionId}/reviews")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "تاریخچه بازبینی‌ها")
    public List<TeacherReviewResponse> listReviews(@PathVariable UUID id,
                                                   @PathVariable UUID submissionId) {
        return assignmentService.listReviews(id, submissionId);
    }

    @PostMapping("/{id}/reevaluate-all")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "تصحیح مجدد همه ارسال‌ها")
    public List<EvaluationResponse> reevaluateAll(@PathVariable UUID id) {
        return assignmentService.reevaluateAll(id);
    }
}
