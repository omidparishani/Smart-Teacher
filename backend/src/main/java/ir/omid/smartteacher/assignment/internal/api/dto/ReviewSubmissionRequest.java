package ir.omid.smartteacher.assignment.internal.api.dto;

import ir.omid.smartteacher.assignment.internal.domain.ReviewAction;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * درخواست بازبینی معلم.
 *
 * @param action   ACCEPT|EDIT_SCORE|EDIT_FEEDBACK|REJECT|RE_EVALUATE
 * @param newScore نمره جدید — برای EDIT_SCORE / ACCEPT اختیاری
 * @param comment  توضیح معلم
 */
public record ReviewSubmissionRequest(
        @NotNull ReviewAction action,
        BigDecimal newScore,
        String comment
) {
}
