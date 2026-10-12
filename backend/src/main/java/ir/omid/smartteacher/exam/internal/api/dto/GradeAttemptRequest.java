package ir.omid.smartteacher.exam.internal.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** بازنویسی نمره توسط معلم. */
public record GradeAttemptRequest(
        @NotEmpty @Valid List<AnswerGradeOverride> overrides
) {
    public record AnswerGradeOverride(
            @NotNull UUID questionId,
            @NotNull BigDecimal awardedScore,
            String feedback,
            Boolean correct
    ) {
    }
}
