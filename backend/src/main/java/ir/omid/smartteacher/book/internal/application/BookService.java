package ir.omid.smartteacher.book.internal.application;

import ir.omid.smartteacher.ai.ContentAnalysisAi;
import ir.omid.smartteacher.billing.EntitlementService;
import ir.omid.smartteacher.book.OcrService;
import ir.omid.smartteacher.book.internal.api.dto.BookPageResponse;
import ir.omid.smartteacher.book.internal.api.dto.BookResponse;
import ir.omid.smartteacher.book.internal.api.dto.ChapterResponse;
import ir.omid.smartteacher.book.internal.api.dto.ContentAnalysisResponse;
import ir.omid.smartteacher.book.internal.api.dto.CreateBookRequest;
import ir.omid.smartteacher.book.internal.api.dto.CreateChapterRequest;
import ir.omid.smartteacher.book.internal.api.dto.UpdateBookRequest;
import ir.omid.smartteacher.book.internal.api.mapper.BookMapper;
import ir.omid.smartteacher.book.internal.domain.AnalysisTargetType;
import ir.omid.smartteacher.book.internal.domain.AnalysisType;
import ir.omid.smartteacher.book.internal.domain.Book;
import ir.omid.smartteacher.book.internal.domain.BookChapter;
import ir.omid.smartteacher.book.internal.domain.BookChapterRepository;
import ir.omid.smartteacher.book.internal.domain.BookPage;
import ir.omid.smartteacher.book.internal.domain.BookPageRepository;
import ir.omid.smartteacher.book.internal.domain.BookRepository;
import ir.omid.smartteacher.book.internal.domain.BookStatus;
import ir.omid.smartteacher.book.internal.domain.ContentAnalysis;
import ir.omid.smartteacher.book.internal.domain.ContentAnalysisRepository;
import ir.omid.smartteacher.book.internal.domain.OcrStatus;
import ir.omid.smartteacher.book.internal.pdf.PdfPageExtractor;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.api.PageableFactory;
import ir.omid.smartteacher.common.api.SearchPattern;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.DuplicateResourceException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.file.FileCatalog;
import ir.omid.smartteacher.file.FileCatalog.StoredFileRef;
import ir.omid.smartteacher.security.TenantGuard;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * منطق کسب‌وکار کتاب، صفحات، فصل، OCR و تحلیل AI — فاز ۶.
 *
 * <p><b>⚠️ برخلاف کلاس‌ها، محدودیت «مالکیت معلم» اینجا نیست.</b>
 * کتاب دارایی مدرسه است؛ همه کارمندان مستأجر آن را می‌بینند. جداسازی
 * فقط بین مستأجرهاست ({@code @TenantId} + {@link TenantGuard}).
 */
@Service
public class BookService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("title", "gradeLevel", "status", "createdAt", "updatedAt");

    private static final String DEFAULT_SORT = "createdAt,desc";

    private static final String ENTITY_BOOK_PAGE = "BOOK_PAGE";
    private static final String ENTITY_BOOK_PDF = "BOOK_PDF";
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final BookRepository bookRepository;
    private final BookPageRepository bookPageRepository;
    private final BookChapterRepository bookChapterRepository;
    private final ContentAnalysisRepository contentAnalysisRepository;
    private final BookMapper mapper;
    private final TenantGuard tenantGuard;
    private final FileCatalog fileCatalog;
    private final OcrService ocrService;
    private final PdfPageExtractor pdfPageExtractor;
    private final ContentAnalysisAi contentAnalysisAi;
    private final ObjectProvider<EntitlementService> entitlementService;

    public BookService(BookRepository bookRepository,
                       BookPageRepository bookPageRepository,
                       BookChapterRepository bookChapterRepository,
                       ContentAnalysisRepository contentAnalysisRepository,
                       BookMapper mapper,
                       TenantGuard tenantGuard,
                       FileCatalog fileCatalog,
                       OcrService ocrService,
                       PdfPageExtractor pdfPageExtractor,
                       ContentAnalysisAi contentAnalysisAi,
                       ObjectProvider<EntitlementService> entitlementService) {
        this.bookRepository = bookRepository;
        this.bookPageRepository = bookPageRepository;
        this.bookChapterRepository = bookChapterRepository;
        this.contentAnalysisRepository = contentAnalysisRepository;
        this.mapper = mapper;
        this.tenantGuard = tenantGuard;
        this.fileCatalog = fileCatalog;
        this.ocrService = ocrService;
        this.pdfPageExtractor = pdfPageExtractor;
        this.contentAnalysisAi = contentAnalysisAi;
        this.entitlementService = entitlementService;
    }

    // ─────────────────────────── کتاب ───────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<BookResponse> list(int page, int size, String sort,
                                           String search, BookStatus status) {
        Pageable pageable = PageableFactory.of(page, size, sort, SORTABLE_FIELDS, DEFAULT_SORT);
        Page<Book> result = bookRepository.findForListing(
                SearchPattern.like(search), status, pageable);
        return PageResponse.from(result, mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BookResponse get(UUID id) {
        return mapper.toResponse(requireBook(id));
    }

    @Transactional
    public BookResponse create(CreateBookRequest request) {
        Book book = new Book(request.title().trim());
        book.setPublisher(trimToNull(request.publisher()));
        book.setGradeLevel(trimToNull(request.gradeLevel()));
        book.setSubjectId(request.subjectId());
        book.setStatus(BookStatus.DRAFT);
        book.setTotalPages(0);

        try {
            return mapper.toResponse(bookRepository.save(book));
        } catch (DataIntegrityViolationException ex) {
            throw subjectOrFkFailure(ex);
        }
    }

    /**
     * ساخت کتاب با PDF اختیاری — صفحات به‌صورت placeholder متنی ساخته می‌شوند.
     */
    @Transactional
    public BookResponse createWithOptionalPdf(CreateBookRequest request, MultipartFile pdf) {
        BookResponse created = create(request);
        if (pdf != null && !pdf.isEmpty()) {
            importPdf(created.id(), pdf);
            return get(created.id());
        }
        return created;
    }

    @Transactional
    public BookResponse update(UUID id, UpdateBookRequest request) {
        Book book = requireBook(id);
        book.setTitle(request.title().trim());
        book.setPublisher(trimToNull(request.publisher()));
        book.setGradeLevel(trimToNull(request.gradeLevel()));
        book.setSubjectId(request.subjectId());
        book.setStatus(request.status());

        if (request.coverFileId() != null) {
            fileCatalog.findActive(request.coverFileId()).orElseThrow(
                    () -> new AppException(ErrorCode.INVALID_REQUEST,
                            "فایل جلد یافت نشد یا متعلق به این مستأجر نیست."));
        }
        book.setCoverFileId(request.coverFileId());

        try {
            return mapper.toResponse(bookRepository.save(book));
        } catch (DataIntegrityViolationException ex) {
            throw subjectOrFkFailure(ex);
        }
    }

    @Transactional
    public void delete(UUID id) {
        Book book = requireBook(id);
        book.softDelete();
        bookRepository.save(book);
    }

    // ─────────────────────────── صفحات ───────────────────────────

    @Transactional(readOnly = true)
    public List<BookPageResponse> listPages(UUID bookId) {
        requireBook(bookId);
        return bookPageRepository.findByBookIdOrderByPageNumberAsc(bookId).stream()
                .map(mapper::toPageResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookPageResponse getPage(UUID bookId, int pageNumber) {
        requireBook(bookId);
        BookPage page = bookPageRepository.findByBookIdAndPageNumber(bookId, pageNumber)
                .orElseThrow(() -> ResourceNotFoundException.of("صفحه"));
        return mapper.toPageResponse(page);
    }

    /**
     * افزودن صفحه — تصویر، PDF چندصفحه‌ای، یا {@code fileId} موجود.
     */
    @Transactional
    public BookPageResponse addPage(UUID bookId,
                                    Integer pageNumber,
                                    MultipartFile file,
                                    UUID fileId) {
        Book book = requireBook(bookId);

        if (pageNumber == null || pageNumber < 1) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "شماره صفحه باید حداقل ۱ باشد.");
        }

        if (file != null && !file.isEmpty() && isPdf(file)) {
            List<BookPageResponse> imported = importPdfFrom(book, file, pageNumber);
            return imported.getFirst();
        }

        if (bookPageRepository.existsByBookIdAndPageNumber(bookId, pageNumber)) {
            throw DuplicateResourceException.of("صفحه", "شماره");
        }

        UUID resolvedFileId = resolveFileId(bookId, file, fileId);

        BookPage page = new BookPage(bookId, pageNumber, resolvedFileId);
        applyOcrPlaceholder(page);

        try {
            BookPage saved = bookPageRepository.save(page);
            fileCatalog.bindEntity(resolvedFileId, ENTITY_BOOK_PAGE, saved.getId());
            refreshTotalPages(book);
            return mapper.toPageResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "فایل صفحه یافت نشد یا متعلق به این مستأجر نیست.", null, ex);
        }
    }

    /**
     * بارگذاری PDF و ساخت صفحه برای هر برگ.
     */
    @Transactional
    public List<BookPageResponse> importPdf(UUID bookId, MultipartFile pdf) {
        Book book = requireBook(bookId);
        if (pdf == null || pdf.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "فایل PDF الزامی است.");
        }
        if (!isPdf(pdf)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "فقط application/pdf پذیرفته می‌شود.");
        }
        int start = (int) bookPageRepository.countByBookId(bookId) + 1;
        return importPdfFrom(book, pdf, start);
    }

    private List<BookPageResponse> importPdfFrom(Book book, MultipartFile pdf, int startPage) {
        book.setStatus(BookStatus.PROCESSING);
        bookRepository.save(book);

        List<String> pageTexts;
        byte[] pdfBytes;
        try {
            pdfBytes = pdf.getBytes();
            // ذخیره خود PDF برای ردیابی مصرف فضا
            enforceStorageQuota(pdfBytes.length);
            try (InputStream in = new ByteArrayInputStream(pdfBytes)) {
                fileCatalog.store(
                        pdf.getOriginalFilename(),
                        PDF_CONTENT_TYPE,
                        in,
                        pdfBytes.length,
                        ENTITY_BOOK_PDF,
                        book.getId());
            }
            pageTexts = pdfPageExtractor.extractPageTexts(new ByteArrayInputStream(pdfBytes));
        } catch (IOException ex) {
            book.setStatus(BookStatus.FAILED);
            bookRepository.save(book);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "خواندن PDF ناموفق بود.", null, ex);
        }

        List<BookPageResponse> created = new ArrayList<>();
        int pageNumber = startPage;
        for (String text : pageTexts) {
            if (bookPageRepository.existsByBookIdAndPageNumber(book.getId(), pageNumber)) {
                throw DuplicateResourceException.of("صفحه", "شماره");
            }
            byte[] placeholder = text.getBytes(StandardCharsets.UTF_8);
            enforceStorageQuota(placeholder.length);
            StoredFileRef stored = fileCatalog.store(
                    "page-" + pageNumber + ".txt",
                    "text/plain; charset=utf-8",
                    new ByteArrayInputStream(placeholder),
                    placeholder.length,
                    ENTITY_BOOK_PAGE,
                    book.getId());

            BookPage page = new BookPage(book.getId(), pageNumber, stored.id());
            page.setExtractedText(text);
            page.setOcrStatus(OcrStatus.DONE);
            page.setOcrConfidence(BigDecimal.ONE);
            BookPage saved = bookPageRepository.save(page);
            fileCatalog.bindEntity(stored.id(), ENTITY_BOOK_PAGE, saved.getId());
            created.add(mapper.toPageResponse(saved));
            pageNumber++;
        }

        refreshTotalPages(book);
        book.setStatus(BookStatus.READY);
        bookRepository.save(book);
        return created;
    }

    // ─────────────────────────── فصل‌ها ───────────────────────────

    @Transactional(readOnly = true)
    public List<ChapterResponse> listChapters(UUID bookId) {
        requireBook(bookId);
        return bookChapterRepository.findByBookIdOrderByOrderIndexAsc(bookId).stream()
                .map(mapper::toChapterResponse)
                .toList();
    }

    @Transactional
    public ChapterResponse createChapter(UUID bookId, CreateChapterRequest request) {
        requireBook(bookId);
        int order = request.orderIndex() != null
                ? request.orderIndex()
                : (int) bookChapterRepository.countByBookId(bookId);
        if (request.startPage() != null && request.endPage() != null
                && request.startPage() > request.endPage()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "بازه صفحه فصل نامعتبر است.");
        }
        BookChapter chapter = new BookChapter(bookId, request.title().trim(), order);
        chapter.setStartPage(request.startPage());
        chapter.setEndPage(request.endPage());
        return mapper.toChapterResponse(bookChapterRepository.save(chapter));
    }

    // ─────────────────────────── OCR ───────────────────────────

    @Transactional
    public BookPageResponse runOcr(UUID bookId, int pageNumber) {
        requireBook(bookId);
        BookPage page = bookPageRepository.findByBookIdAndPageNumber(bookId, pageNumber)
                .orElseThrow(() -> ResourceNotFoundException.of("صفحه"));

        try (InputStream in = fileCatalog.open(page.getFileId())) {
            OcrService.OcrResult result = ocrService.extractText(in, null);
            page.setExtractedText(result.text());
            if (result.confidence() != null) {
                page.setOcrConfidence(BigDecimal.valueOf(result.confidence()));
            }
            page.setOcrStatus(result.skipped() ? OcrStatus.SKIPPED : OcrStatus.DONE);
        } catch (IOException ex) {
            page.setOcrStatus(OcrStatus.FAILED);
            bookPageRepository.save(page);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "اجرای OCR ناموفق بود.", null, ex);
        }

        return mapper.toPageResponse(bookPageRepository.save(page));
    }

    // ─────────────────────────── تحلیل AI ───────────────────────────

    @Transactional
    public ContentAnalysisResponse analyzePage(UUID bookId, int pageNumber, AnalysisType type) {
        requireBook(bookId);
        BookPage page = bookPageRepository.findByBookIdAndPageNumber(bookId, pageNumber)
                .orElseThrow(() -> ResourceNotFoundException.of("صفحه"));

        String source = page.getExtractedText();
        if (source == null || source.isBlank()) {
            // اگر OCR قبلاً نزده، یک بار Mock OCR بزن
            runOcr(bookId, pageNumber);
            page = bookPageRepository.findByBookIdAndPageNumber(bookId, pageNumber)
                    .orElseThrow(() -> ResourceNotFoundException.of("صفحه"));
            source = page.getExtractedText();
        }

        String result = contentAnalysisAi.analyze(type.name(), source);
        ContentAnalysis analysis = new ContentAnalysis(
                AnalysisTargetType.PAGE, page.getId(), type, result);
        return mapper.toAnalysisResponse(contentAnalysisRepository.save(analysis));
    }

    @Transactional(readOnly = true)
    public List<ContentAnalysisResponse> listAnalyses(UUID bookId) {
        requireBook(bookId);
        List<UUID> pageIds = bookPageRepository.findByBookIdOrderByPageNumberAsc(bookId).stream()
                .map(BookPage::getId)
                .toList();
        List<UUID> targets = new ArrayList<>();
        targets.add(bookId);
        targets.addAll(pageIds);
        return contentAnalysisRepository
                .findByTargetIdInAndDeletedAtIsNullOrderByCreatedAtDesc(targets)
                .stream()
                .map(mapper::toAnalysisResponse)
                .toList();
    }

    // ─────────────────────────── کمکی ───────────────────────────

    private UUID resolveFileId(UUID bookId, MultipartFile file, UUID fileId) {
        boolean hasUpload = file != null && !file.isEmpty();
        boolean hasFileId = fileId != null;

        if (hasUpload == hasFileId) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "یکی از «فایل تصویری» یا «شناسه فایل» باید فرستاده شود.");
        }

        if (hasFileId) {
            fileCatalog.findActive(fileId).orElseThrow(
                    () -> new AppException(ErrorCode.INVALID_REQUEST,
                            "فایل یافت نشد یا متعلق به این مستأجر نیست."));
            return fileId;
        }

        enforceStorageQuota(file.getSize());
        try (InputStream in = file.getInputStream()) {
            StoredFileRef stored = fileCatalog.store(
                    file.getOriginalFilename(),
                    file.getContentType(),
                    in,
                    file.getSize(),
                    ENTITY_BOOK_PAGE,
                    bookId);
            return stored.id();
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "خواندن فایل آپلود ناموفق بود.", null, ex);
        }
    }

    private void applyOcrPlaceholder(BookPage page) {
        OcrService.OcrResult result = ocrService.extractText(InputStream.nullInputStream(), null);
        page.setExtractedText(result.text());
        if (result.confidence() != null) {
            page.setOcrConfidence(BigDecimal.valueOf(result.confidence()));
        }
        page.setOcrStatus(result.skipped() ? OcrStatus.SKIPPED : OcrStatus.DONE);
    }

    private void refreshTotalPages(Book book) {
        long count = bookPageRepository.countByBookId(book.getId());
        book.setTotalPages((int) count);
        if (count > 0 && book.getStatus() == BookStatus.DRAFT) {
            book.setStatus(BookStatus.READY);
        }
        bookRepository.save(book);
    }

    private void enforceStorageQuota(long additionalBytes) {
        EntitlementService entitlement = entitlementService.getIfAvailable();
        if (entitlement != null) {
            entitlement.requireStorageCapacity(fileCatalog.totalSizeBytes(), additionalBytes);
        }
    }

    private static boolean isPdf(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("pdf")) {
            return true;
        }
        String name = file.getOriginalFilename();
        return name != null && name.toLowerCase().endsWith(".pdf");
    }

    Book requireBook(UUID id) {
        return tenantGuard.requireOwned(
                bookRepository.findByIdAndDeletedAtIsNull(id).orElse(null), "Book");
    }

    private static AppException subjectOrFkFailure(DataIntegrityViolationException ex) {
        return new AppException(ErrorCode.INVALID_REQUEST,
                "درس یا فایل ارجاع‌شده یافت نشد یا متعلق به این مستأجر نیست.", null, ex);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
