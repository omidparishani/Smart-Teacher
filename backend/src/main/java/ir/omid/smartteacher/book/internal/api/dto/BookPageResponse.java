package ir.omid.smartteacher.book.internal.api.dto;

import ir.omid.smartteacher.book.internal.domain.OcrStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * نمای صفحه کتاب.
 *
 * @param id             شناسه صفحه
 * @param bookId         شناسه کتاب
 * @param pageNumber     شماره صفحه
 * @param fileId         شناسه فایل تصویر
 * @param extractedText  متن OCR؛ ممکن است {@code null}
 * @param ocrStatus      وضعیت OCR
 * @param ocrConfidence  اطمینان؛ ممکن است {@code null}
 * @param createdAt      زمان ساخت
 * @param updatedAt      زمان آخرین تغییر
 */
public record BookPageResponse(
        UUID id,
        UUID bookId,
        int pageNumber,
        UUID fileId,
        String extractedText,
        OcrStatus ocrStatus,
        BigDecimal ocrConfidence,
        Instant createdAt,
        Instant updatedAt
) {
}
