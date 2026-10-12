package ir.omid.smartteacher.book.internal.api.dto;

import ir.omid.smartteacher.book.internal.domain.AnalysisTargetType;
import ir.omid.smartteacher.book.internal.domain.AnalysisType;
import java.time.Instant;
import java.util.UUID;

/**
 * نمای تحلیل ذخیره‌شده.
 */
public record ContentAnalysisResponse(
        UUID id,
        AnalysisTargetType targetType,
        UUID targetId,
        AnalysisType analysisType,
        String resultText,
        Instant createdAt
) {
}
