package ir.omid.smartteacher.exam.internal.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamAnswerResponse(
        UUID id,
        UUID questionId,
        String answerText,
        String selectedOptions,
        BigDecimal awardedScore,
        Boolean correct,
        boolean aiGraded,
        String aiFeedback
) {}
