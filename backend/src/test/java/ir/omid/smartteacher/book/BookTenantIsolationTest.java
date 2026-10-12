package ir.omid.smartteacher.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.book.internal.api.dto.BookResponse;
import ir.omid.smartteacher.book.internal.api.dto.CreateBookRequest;
import ir.omid.smartteacher.book.internal.application.BookService;
import ir.omid.smartteacher.book.internal.domain.BookStatus;
import ir.omid.smartteacher.book.internal.domain.OcrStatus;
import ir.omid.smartteacher.common.api.PageResponse;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * جداسازی مستأجر روی کتاب‌ها + بارگذاری صفحه با OCR جایگزین.
 *
 * <pre>{@code
 * mvn test -Dtest=BookTenantIsolationTest -Dtest.postgres.image=postgres:17-alpine
 * }</pre>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BookTenantIsolationTest {

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

    @Test
    @DisplayName("معلم مستأجر ب کتاب مستأجر الف را نمی‌بیند")
    void tenantB_cannotSeeTenantA_books() {
        Actor a = newTenant(MemberRole.TEACHER);
        Actor b = newTenant(MemberRole.TEACHER);

        UUID bookId = createBook(a, "ریاضی " + unique());

        PageResponse<BookResponse> listB = TenantContext.callAs(b.principal(),
                () -> bookService.list(0, 50, null, null, null));
        assertThat(listB.content()).noneMatch(book -> book.id().equals(bookId));

        assertThatThrownBy(() -> TenantContext.callAs(b.principal(),
                () -> bookService.get(bookId)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("حذف نرم کتاب را از فهرست و get حذف می‌کند")
    void softDelete_hidesBook() {
        Actor a = newTenant(MemberRole.TEACHER);
        UUID bookId = createBook(a, "علوم " + unique());

        TenantContext.callAs(a.principal(), () -> {
            bookService.delete(bookId);
            return null;
        });

        assertThatThrownBy(() -> TenantContext.callAs(a.principal(),
                () -> bookService.get(bookId)))
                .isInstanceOf(ResourceNotFoundException.class);

        PageResponse<BookResponse> list = TenantContext.callAs(a.principal(),
                () -> bookService.list(0, 50, null, null, null));
        assertThat(list.content()).noneMatch(book -> book.id().equals(bookId));
    }

    @Test
    @DisplayName("آپلود صفحه با Mock OCR متن جایگزین می‌گذارد")
    void addPage_runsMockOcr() {
        Actor a = newTenant(MemberRole.TEACHER);
        UUID bookId = createBook(a, "فارسی " + unique());

        var page = TenantContext.callAs(a.principal(), () -> bookService.addPage(
                bookId,
                1,
                new MockMultipartFile(
                        "file",
                        "page1.png",
                        "image/png",
                        "fake-image".getBytes(StandardCharsets.UTF_8)),
                null));

        assertThat(page.pageNumber()).isEqualTo(1);
        assertThat(page.fileId()).isNotNull();
        assertThat(page.ocrStatus()).isEqualTo(OcrStatus.SKIPPED);
        assertThat(page.extractedText()).contains("OCR");

        BookResponse book = TenantContext.callAs(a.principal(), () -> bookService.get(bookId));
        assertThat(book.totalPages()).isEqualTo(1);
        assertThat(book.status()).isEqualTo(BookStatus.READY);
    }

    @Test
    @DisplayName("صفحه تکراری با همان شماره رد می‌شود")
    void duplicatePageNumber_rejected() {
        Actor a = newTenant(MemberRole.TEACHER);
        UUID bookId = createBook(a, "تاریخ " + unique());

        TenantContext.callAs(a.principal(), () -> bookService.addPage(
                bookId, 1,
                new MockMultipartFile("file", "p.png", "image/png", new byte[]{1, 2, 3}),
                null));

        assertThatThrownBy(() -> TenantContext.callAs(a.principal(), () -> bookService.addPage(
                bookId, 1,
                new MockMultipartFile("file", "p2.png", "image/png", new byte[]{4, 5, 6}),
                null)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_RESOURCE));
    }

    private record Actor(UUID userId, UUID tenantId, TenantPrincipal principal) {
    }
}
