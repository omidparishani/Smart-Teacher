package ir.omid.smartteacher.book.internal.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * نمای فصل کتاب.
 */
public record ChapterResponse(
        UUID id,
        UUID bookId,
        String title,
        int orderIndex,
        Integer startPage,
        Integer endPage,
        Instant createdAt,
        Instant updatedAt
) {
}
