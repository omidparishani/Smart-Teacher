package ir.omid.smartteacher.book.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.book.internal.api.dto.BookPageResponse;
import ir.omid.smartteacher.book.internal.api.dto.BookResponse;
import ir.omid.smartteacher.book.internal.api.dto.ChapterResponse;
import ir.omid.smartteacher.book.internal.api.dto.ContentAnalysisResponse;
import ir.omid.smartteacher.book.internal.api.dto.CreateBookRequest;
import ir.omid.smartteacher.book.internal.api.dto.CreateChapterRequest;
import ir.omid.smartteacher.book.internal.api.dto.UpdateBookRequest;
import ir.omid.smartteacher.book.internal.application.BookService;
import ir.omid.smartteacher.book.internal.domain.AnalysisType;
import ir.omid.smartteacher.book.internal.domain.BookStatus;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.api.PageableFactory;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * مسیرهای کتابخانه — {@code /api/v1/books} مطابق {@code docs/api.md} بخش ۴.۴.
 *
 * <p>فاز ۶ گام ۲: PDF، فصل، OCR صریح، تحلیل AI و {@code content_analyses}.
 */
@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "کتابخانه", description = "کتاب‌ها و صفحات — متعلق به مستأجر")
public class BookController {

    static final String STAFF_ONLY =
            "hasAnyAuthority('TENANT_OWNER','TENANT_SCHOOL_ADMIN','TENANT_TEACHER')";

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "فهرست کتاب‌ها")
    public PageResponse<BookResponse> list(
            @Parameter(description = "شماره صفحه، از صفر")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "اندازه صفحه")
            @RequestParam(defaultValue = "" + PageableFactory.DEFAULT_SIZE) int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BookStatus status) {
        return bookService.list(page, size, sort, search, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "دریافت کتاب")
    public BookResponse get(@PathVariable UUID id) {
        return bookService.get(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ساخت کتاب")
    public BookResponse create(@Valid @RequestBody CreateBookRequest request) {
        return bookService.create(request);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ساخت کتاب با PDF اختیاری",
            description = "فیلدهای title/publisher/gradeLevel + فایل pdf اختیاری.")
    public BookResponse createWithPdf(
            @RequestParam("title") String title,
            @RequestParam(value = "publisher", required = false) String publisher,
            @RequestParam(value = "gradeLevel", required = false) String gradeLevel,
            @RequestParam(value = "subjectId", required = false) UUID subjectId,
            @RequestPart(value = "pdf", required = false) MultipartFile pdf) {
        return bookService.createWithOptionalPdf(
                new CreateBookRequest(title, publisher, gradeLevel, subjectId), pdf);
    }

    @PutMapping("/{id}")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "ویرایش کتاب")
    public BookResponse update(@PathVariable UUID id,
                               @Valid @RequestBody UpdateBookRequest request) {
        return bookService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "حذف نرم کتاب")
    public void delete(@PathVariable UUID id) {
        bookService.delete(id);
    }

    @GetMapping("/{id}/pages")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "فهرست صفحات کتاب")
    public List<BookPageResponse> listPages(@PathVariable UUID id) {
        return bookService.listPages(id);
    }

    @GetMapping("/{id}/pages/{pageNumber}")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "یک صفحه")
    public BookPageResponse getPage(@PathVariable UUID id,
                                    @PathVariable int pageNumber) {
        return bookService.getPage(id, pageNumber);
    }

    /**
     * بارگذاری صفحه — چندبخشی.
     *
     * <p>یکی از {@code file} یا {@code fileId} الزامی است (نه هر دو، نه هیچ‌کدام).
     * اگر {@code file} از نوع PDF باشد، همه صفحات استخراج می‌شوند.
     */
    @PostMapping(value = "/{id}/pages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "افزودن صفحه",
            description = "آپلود تصویر/PDF یا ارجاع به fileId موجود + شماره صفحه.")
    public BookPageResponse addPage(
            @PathVariable UUID id,
            @RequestParam("pageNumber") Integer pageNumber,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "fileId", required = false) UUID fileId) {
        return bookService.addPage(id, pageNumber, file, fileId);
    }

    @PostMapping(value = "/{id}/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "بارگذاری PDF و استخراج صفحات")
    public List<BookPageResponse> uploadPdf(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file) {
        return bookService.importPdf(id, file);
    }

    @GetMapping("/{id}/chapters")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "فهرست فصل‌ها")
    public List<ChapterResponse> listChapters(@PathVariable UUID id) {
        return bookService.listChapters(id);
    }

    @PostMapping("/{id}/chapters")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "افزودن فصل")
    public ChapterResponse createChapter(@PathVariable UUID id,
                                         @Valid @RequestBody CreateChapterRequest request) {
        return bookService.createChapter(id, request);
    }

    @PostMapping("/{id}/pages/{pageNumber}/ocr")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "اجرای OCR روی صفحه")
    public BookPageResponse runOcr(@PathVariable UUID id,
                                   @PathVariable int pageNumber) {
        return bookService.runOcr(id, pageNumber);
    }

    @PostMapping("/{id}/pages/{pageNumber}/analyze")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "تحلیل AI (خلاصه)")
    public ContentAnalysisResponse analyze(@PathVariable UUID id,
                                           @PathVariable int pageNumber) {
        return bookService.analyzePage(id, pageNumber, AnalysisType.SUMMARY);
    }

    @PostMapping("/{id}/pages/{pageNumber}/questions")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "تولید سؤال با AI")
    public ContentAnalysisResponse questions(@PathVariable UUID id,
                                             @PathVariable int pageNumber) {
        return bookService.analyzePage(id, pageNumber, AnalysisType.QUESTIONS);
    }

    @PostMapping("/{id}/pages/{pageNumber}/teaching-plan")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "طرح درس با AI")
    public ContentAnalysisResponse teachingPlan(@PathVariable UUID id,
                                                @PathVariable int pageNumber) {
        return bookService.analyzePage(id, pageNumber, AnalysisType.TEACHING_PLAN);
    }

    @GetMapping("/{id}/analyses")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "تحلیل‌های ذخیره‌شده کتاب")
    public List<ContentAnalysisResponse> listAnalyses(@PathVariable UUID id) {
        return bookService.listAnalyses(id);
    }
}
