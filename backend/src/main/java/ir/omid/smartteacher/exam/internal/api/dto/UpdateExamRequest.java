package ir.omid.smartteacher.exam.internal.api.dto;

import ir.omid.smartteacher.exam.internal.domain.ExamStatus;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UpdateExamRequest(
        @NotBlank String title,
        String description,
        Integer durationMinutes,
        BigDecimal totalScore,
        Instant startAt,
        Instant endAt,
        ExamStatus status,
        UUID bookId,
        Boolean shuffleQuestions,
        Boolean showResultsImmediately
) {}
