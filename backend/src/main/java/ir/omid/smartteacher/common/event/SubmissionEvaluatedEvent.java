package ir.omid.smartteacher.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * رویداد دامنه — ارسال تکلیف تصحیح شد (تریگر خودکارسازی فاز ۹).
 */
public record SubmissionEvaluatedEvent(
        UUID tenantId,
        UUID assignmentId,
        UUID submissionId,
        String assignmentTitle,
        BigDecimal suggestedScore
) {
}
