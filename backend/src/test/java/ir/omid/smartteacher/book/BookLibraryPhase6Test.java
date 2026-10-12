package ir.omid.smartteacher.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.book.internal.api.dto.BookPageResponse;
import ir.omid.smartteacher.book.internal.api.dto.BookResponse;
import ir.omid.smartteacher.book.internal.api.dto.ChapterResponse;
import ir.omid.smartteacher.book.internal.api.dto.ContentAnalysisResponse;
import ir.omid.smartteacher.book.internal.api.dto.CreateBookRequest;
import ir.omid.smartteacher.book.internal.api.dto.CreateChapterRequest;
import ir.omid.smartteacher.book.internal.application.BookService;
import ir.omid.smartteacher.book.internal.domain.AnalysisType;
import ir.omid.smartteacher.book.internal.domain.OcrStatus;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.file.FileStorage;
import ir.omid.smartteacher.file.FileStorageProperties;
import ir.omid.smartteacher.file.internal.LocalFileStorage;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * فاز ۶ گام ۲ — PDF، تحلیل، فصل؛ path-traversal همچنان مسدود.
 *
 * <pre>{@code
 * mvn test -Dtest=BookLibraryPhase6Test -Dtest.postgres.image=postgres:17-alpine
 * }</pre>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BookLibraryPhase6Test {

    static final String POSTGRES_IMAGE =
            System.getProperty("test.postgres.image", "postgres:17-alpine");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private BookService bookService;

    @TempDir
    Path tempDir;

    private Actor newTenant(MemberRole role) {
        UUID userId = UuidV7Generator.generate();
        jdbc.update("insert into users (id, full_name) values (?, ?)",
                userId, "کاربر " + unique());

        UUID tenantId = UuidV7Generator.generate();
        jdbc.update("""
                insert into tenants (id, type, name, slug, owner_user_id)
                values (?, 'TEACHER', ?, ?, ?)
                """,
                tenantId, "مدرسه " + unique(), "school-" + unique(), userId);

        return new Actor(userId, tenantId, TenantPrincipal.ofTenantUser(
                userId, tenantId, null, Set.of(role.name()), role, "fa"));
    }

    private static String unique() {
        return "x" + SEQ.incrementAndGet() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private UUID createBook(Actor actor, String title) {
        return TenantContext.callAs(actor.principal(), () -> bookService.create(
                new CreateBookRequest(title, "ناشر", "پنجم", null))).id();
    }

    private static byte[] tinyPdf(String pageText) throws Exception {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(pageText);
                cs.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("استخراج PDF صفحات placeholder می‌سازد")
    void pdfExtract_createsPages() throws Exception {
        Actor a = newTenant(MemberRole.TEACHER);
        UUID bookId = createBook(a, "PDF " + unique());
        byte[] pdf = tinyPdf("Hello Smart Teacher");

        List<BookPageResponse> pages = TenantContext.callAs(a.principal(), () ->
                bookService.importPdf(bookId, new MockMultipartFile(
                        "file", "tiny.pdf", "application/pdf", pdf)));

        assertThat(pages).hasSize(1);
        assertThat(pages.getFirst().extractedText()).contains("Hello Smart Teacher");
        assertThat(pages.getFirst().ocrStatus()).isEqualTo(OcrStatus.DONE);

        BookResponse book = TenantContext.callAs(a.principal(), () -> bookService.get(bookId));
        assertThat(book.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("تحلیل AI در content_analyses ذخیره می‌شود")
    void analyze_storesContentAnalysis() throws Exception {
        Actor a = newTenant(MemberRole.TEACHER);
        UUID bookId = createBook(a, "تحلیل " + unique());
        byte[] pdf = tinyPdf("Page content for analysis");

        TenantContext.callAs(a.principal(), () -> {
            bookService.importPdf(bookId, new MockMultipartFile(
                    "file", "a.pdf", "application/pdf", pdf));
            ContentAnalysisResponse analysis =
                    bookService.analyzePage(bookId, 1, AnalysisType.SUMMARY);
            assertThat(analysis.analysisType()).isEqualTo(AnalysisType.SUMMARY);
            assertThat(analysis.resultText()).isNotBlank();

            List<ContentAnalysisResponse> all = bookService.listAnalyses(bookId);
            assertThat(all).anyMatch(x -> x.id().equals(analysis.id()));
            return null;
        });
    }

    @Test
    @DisplayName("فصل GET|POST کار می‌کند")
    void chapters_api() {
        Actor a = newTenant(MemberRole.TEACHER);
        UUID bookId = createBook(a, "فصل " + unique());

        ChapterResponse chapter = TenantContext.callAs(a.principal(), () ->
                bookService.createChapter(bookId,
                        new CreateChapterRequest("فصل یک", 0, 1, 5)));

        assertThat(chapter.title()).isEqualTo("فصل یک");

        List<ChapterResponse> list = TenantContext.callAs(a.principal(),
                () -> bookService.listChapters(bookId));
        assertThat(list).extracting(ChapterResponse::id).contains(chapter.id());
    }

    @Test
    @DisplayName("OCR صریح متن جایگزین می‌گذارد")
    void ocrEndpoint_fillsExtractedText() {
        Actor a = newTenant(MemberRole.TEACHER);
        UUID bookId = createBook(a, "OCR " + unique());

        TenantContext.callAs(a.principal(), () -> bookService.addPage(
                bookId, 1,
                new MockMultipartFile("file", "p.png", "image/png",
                        "img".getBytes(StandardCharsets.UTF_8)),
                null));

        BookPageResponse page = TenantContext.callAs(a.principal(),
                () -> bookService.runOcr(bookId, 1));
        assertThat(page.extractedText()).contains("OCR");
        assertThat(page.ocrStatus()).isIn(OcrStatus.SKIPPED, OcrStatus.DONE);
    }

    @Test
    @DisplayName("path traversal همچنان مسدود است")
    void pathTraversal_stillBlocked() {
        FileStorage storage = new LocalFileStorage(new FileStorageProperties(tempDir));
        byte[] bytes = "x".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> storage.store(
                "../outside.txt", new ByteArrayInputStream(bytes), bytes.length))
                .isInstanceOf(ir.omid.smartteacher.common.error.AppException.class)
                .satisfies(ex -> assertThat(
                        ((ir.omid.smartteacher.common.error.AppException) ex).getErrorCode())
                        .isEqualTo(ir.omid.smartteacher.common.error.ErrorCode.INVALID_REQUEST));
        assertThat(Files.exists(tempDir.resolveSibling("outside.txt"))).isFalse();
    }

    private record Actor(UUID userId, UUID tenantId, TenantPrincipal principal) {
    }
}
