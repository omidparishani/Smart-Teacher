package ir.omid.smartteacher.assignment.internal.api.dto;

import ir.omid.smartteacher.assignment.internal.domain.ReviewAction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * یک ردیف لاگ بازبینی معلم.
 */
public record TeacherReviewResponse(
        UUID id,
        UUID submissionId,
        UUID reviewerUserId,
        ReviewAction action,
        BigDecimal previousScore,
        BigDecimal newScore,
        String comment,
        Instant reviewedAt
) {
}
