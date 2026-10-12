package ir.omid.smartteacher.book.internal.api.dto;

import ir.omid.smartteacher.book.internal.domain.BookStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * نمای کتاب در پاسخ API.
 *
 * @param id          شناسه
 * @param title       عنوان
 * @param publisher   ناشر؛ ممکن است {@code null}
 * @param gradeLevel  پایه؛ ممکن است {@code null}
 * @param subjectId   شناسه درس؛ ممکن است {@code null}
 * @param totalPages  شمار صفحات ثبت‌شده؛ ممکن است {@code null}
 * @param status      وضعیت
 * @param coverFileId شناسه فایل جلد؛ ممکن است {@code null}
 * @param createdAt   زمان ساخت
 * @param updatedAt   زمان آخرین تغییر
 */
public record BookResponse(
        UUID id,
        String title,
        String publisher,
        String gradeLevel,
        UUID subjectId,
        Integer totalPages,
        BookStatus status,
        UUID coverFileId,
        Instant createdAt,
        Instant updatedAt
) {
}
