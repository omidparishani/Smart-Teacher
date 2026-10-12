package ir.omid.smartteacher.exam.internal.api.dto;

import ir.omid.smartteacher.exam.internal.domain.ExamStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExamResponse(
        UUID id,
        UUID classroomId,
        UUID bookId,
        String title,
        String description,
        Integer durationMinutes,
        BigDecimal totalScore,
        Instant startAt,
        Instant endAt,
        ExamStatus status,
        boolean shuffleQuestions,
        boolean showResultsImmediately,
        List<ExamQuestionResponse> questions,
        Instant createdAt
) {}
