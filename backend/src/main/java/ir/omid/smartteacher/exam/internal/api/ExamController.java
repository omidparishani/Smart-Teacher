package ir.omid.smartteacher.exam.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.api.PageableFactory;
import ir.omid.smartteacher.exam.internal.api.dto.CreateExamQuestionRequest;
import ir.omid.smartteacher.exam.internal.api.dto.CreateExamRequest;
import ir.omid.smartteacher.exam.internal.api.dto.ExamAnswerResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamAttemptReportResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamAttemptResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamBlueprintResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamQuestionResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamResponse;
import ir.omid.smartteacher.exam.internal.api.dto.ExamResultsResponse;
import ir.omid.smartteacher.exam.internal.api.dto.GenerateExamRequest;
import ir.omid.smartteacher.exam.internal.api.dto.GradeAttemptRequest;
import ir.omid.smartteacher.exam.internal.api.dto.PublishExamRequest;
import ir.omid.smartteacher.exam.internal.api.dto.StartExamRequest;
import ir.omid.smartteacher.exam.internal.api.dto.SubmitAnswerRequest;
import ir.omid.smartteacher.exam.internal.api.dto.UpdateExamRequest;
import ir.omid.smartteacher.exam.internal.application.ExamService;
import ir.omid.smartteacher.exam.internal.domain.ExamStatus;
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
 * مسیرهای آزمون — {@code /api/v1/exams} مطابق {@code docs/api.md} بخش ۴.۶.
 */
@RestController
@RequestMapping("/api/v1/exams")
@Tag(name = "آزمون‌ها", description = "ساخت، برگزاری و تصحیح آزمون")
public class ExamController {

    static final String STAFF_ONLY =
            "hasAnyAuthority('TENANT_OWNER','TENANT_SCHOOL_ADMIN','TENANT_TEACHER')";
    static final String STUDENT_OR_STAFF =
            "hasAnyAuthority('TENANT_OWNER','TENANT_SCHOOL_ADMIN','TENANT_TEACHER','TENANT_STUDENT')";

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "فهرست آزمون‌ها")
    public PageResponse<ExamResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + PageableFactory.DEFAULT_SIZE) int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) UUID classroomId,
            @RequestParam(required = false) ExamStatus status) {
        return examService.list(page, size, sort, classroomId, status);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "تولید آزمون با AI از کتاب و بازه صفحه")
    public ExamResponse generate(@Valid @RequestBody GenerateExamRequest request) {
        return examService.generate(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "دریافت آزمون با سؤال‌ها")
    public ExamResponse get(@PathVariable UUID id) {
        return examService.get(id);
    }

    @GetMapping("/{id}/blueprint")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "طرح تولید AI آزمون")
    public ExamBlueprintResponse blueprint(@PathVariable UUID id) {
        return examService.getBlueprint(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ساخت آزمون")
    public ExamResponse create(@Valid @RequestBody CreateExamRequest request) {
        return examService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ویرایش آزمون")
    public ExamResponse update(@PathVariable UUID id,
                               @Valid @RequestBody UpdateExamRequest request) {
        return examService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "حذف نرم آزمون")
    public void delete(@PathVariable UUID id) {
        examService.delete(id);
    }

    @PostMapping("/{id}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "افزودن سؤال")
    public ExamQuestionResponse addQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody CreateExamQuestionRequest request) {
        return examService.addQuestion(id, request);
    }

    @PutMapping("/{id}/questions/{qid}")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ویرایش سؤال")
    public ExamQuestionResponse updateQuestion(
            @PathVariable UUID id,
            @PathVariable UUID qid,
            @Valid @RequestBody CreateExamQuestionRequest request) {
        return examService.updateQuestion(id, qid, request);
    }

    @DeleteMapping("/{id}/questions/{qid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "حذف سؤال")
    public void deleteQuestion(@PathVariable UUID id, @PathVariable UUID qid) {
        examService.deleteQuestion(id, qid);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "انتشار آزمون")
    public ExamResponse publish(@PathVariable UUID id,
                                 @RequestBody(required = false) PublishExamRequest request) {
        return examService.publish(id, request);
    }

    @GetMapping("/{id}/attempts")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "فهرست تلاش‌ها")
    public List<ExamAttemptResponse> listAttempts(@PathVariable UUID id) {
        return examService.listAttempts(id);
    }

    @GetMapping("/{id}/results")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "نتایج آزمون")
    public ExamResultsResponse results(@PathVariable UUID id) {
        return examService.results(id);
    }

    @PostMapping("/{id}/attempts/{aid}/grade")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "بازنویسی نمره توسط معلم")
    public ExamAttemptResponse gradeAttempt(
            @PathVariable UUID id,
            @PathVariable UUID aid,
            @Valid @RequestBody GradeAttemptRequest request) {
        return examService.gradeAttempt(id, aid, request);
    }

    @PostMapping("/{id}/start")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STUDENT_OR_STAFF)
    @Operation(summary = "شروع تلاش دانش‌آموز")
    public ExamAttemptResponse start(@PathVariable UUID id,
                                     @Valid @RequestBody StartExamRequest request) {
        return examService.start(id, request);
    }

    @PostMapping("/attempts/{aid}/answers")
    @PreAuthorize(STUDENT_OR_STAFF)
    @Operation(summary = "ثبت/به‌روزرسانی پاسخ")
    public ExamAnswerResponse saveAnswer(
            @PathVariable UUID aid,
            @RequestParam UUID studentId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        return examService.saveAnswer(aid, studentId, request);
    }

    @PostMapping("/attempts/{aid}/submit")
    @PreAuthorize(STUDENT_OR_STAFF)
    @Operation(summary = "ارسال نهایی، تصحیح MCQ و AI برای تشریحی")
    public ExamAttemptResponse submit(@PathVariable UUID aid,
                                      @RequestParam UUID studentId) {
        return examService.submit(aid, studentId);
    }

    @GetMapping("/attempts/{aid}")
    @PreAuthorize(STUDENT_OR_STAFF)
    @Operation(summary = "مشاهده تلاش خود")
    public ExamAttemptResponse getAttempt(@PathVariable UUID aid,
                                          @RequestParam UUID studentId) {
        return examService.getAttemptForStudent(aid, studentId);
    }

    @GetMapping("/attempts/{aid}/report")
    @PreAuthorize(STUDENT_OR_STAFF)
    @Operation(summary = "کارنامه تلاش")
    public ExamAttemptReportResponse report(@PathVariable UUID aid) {
        return examService.report(aid);
    }
}
