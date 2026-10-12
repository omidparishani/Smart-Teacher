package ir.omid.smartteacher.exam.internal.api.dto;

import ir.omid.smartteacher.exam.internal.domain.QuestionType;
import java.math.BigDecimal;
import java.util.UUID;

public record ExamQuestionResponse(
        UUID id,
        UUID examId,
        QuestionType type,
        String questionText,
        String options,
        String correctAnswer,
        BigDecimal score,
        String difficulty,
        String explanation,
        int orderIndex,
        boolean aiGenerated
) {}
