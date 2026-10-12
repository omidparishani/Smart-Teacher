package ir.omid.smartteacher.assignment.internal.api.dto;

import ir.omid.smartteacher.assignment.internal.domain.EvaluationStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * پاسخ تصحیح AI.
 */
public record EvaluationResponse(
        UUID id,
        UUID submissionId,
        EvaluationStatus status,
        BigDecimal aiScore,
        Boolean correct,
        List<String> mistakes,
        List<String> strengths,
        List<String> weaknesses,
        String feedback,
        String teacherNote,
        BigDecimal confidence,
        boolean requiresTeacherReview,
        BigDecimal finalScore
) {
}
