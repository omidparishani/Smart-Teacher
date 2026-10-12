package ir.omid.smartteacher.exam.internal.api.dto;

import ir.omid.smartteacher.exam.internal.domain.AttemptStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExamAttemptResponse(
        UUID id,
        UUID examId,
        UUID studentId,
        Instant startedAt,
        Instant submittedAt,
        BigDecimal score,
        AttemptStatus status,
        int attemptNumber,
        List<ExamAnswerResponse> answers
) {}
