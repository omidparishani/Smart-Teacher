package ir.omid.smartteacher.exam.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateExamRequest(
        @NotNull UUID classroomId,
        @NotBlank String title,
        String description,
        Integer durationMinutes,
        BigDecimal totalScore,
        Instant startAt,
        Instant endAt,
        UUID bookId,
        Boolean shuffleQuestions,
        Boolean showResultsImmediately
) {}
