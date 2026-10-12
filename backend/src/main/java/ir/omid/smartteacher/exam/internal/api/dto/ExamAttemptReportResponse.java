package ir.omid.smartteacher.exam.internal.api.dto;

import ir.omid.smartteacher.exam.internal.domain.AttemptStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** کارنامه یک تلاش. */
public record ExamAttemptReportResponse(
        UUID attemptId,
        UUID examId,
        String examTitle,
        UUID studentId,
        Instant startedAt,
        Instant submittedAt,
        BigDecimal score,
        BigDecimal totalScore,
        AttemptStatus status,
        Integer durationMinutes,
        List<ExamAnswerResponse> answers,
        List<ExamQuestionResponse> questions
) {
}
