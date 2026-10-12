package ir.omid.smartteacher.exam.internal.api.dto;

import ir.omid.smartteacher.exam.internal.domain.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateExamQuestionRequest(
        @NotNull QuestionType type,
        @NotBlank String questionText,
        String options,
        String correctAnswer,
        BigDecimal score,
        String difficulty,
        String explanation,
        Integer orderIndex
) {}
