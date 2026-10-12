package ir.omid.smartteacher.exam.internal.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** خلاصه نتایج یک آزمون برای معلم. */
public record ExamResultsResponse(
        UUID examId,
        String title,
        int attemptCount,
        BigDecimal averageScore,
        List<ExamAttemptResponse> attempts
) {
}
