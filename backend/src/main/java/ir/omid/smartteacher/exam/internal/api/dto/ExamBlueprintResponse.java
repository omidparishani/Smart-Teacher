package ir.omid.smartteacher.exam.internal.api.dto;

import java.time.Instant;
import java.util.UUID;

/** طرح تولید AI برای یک آزمون. */
public record ExamBlueprintResponse(
        UUID id,
        UUID examId,
        UUID bookId,
        int pageFrom,
        int pageTo,
        String difficultyDistribution,
        String questionTypeDistribution,
        int questionCount,
        UUID aiUsageId,
        Instant createdAt
) {
}
