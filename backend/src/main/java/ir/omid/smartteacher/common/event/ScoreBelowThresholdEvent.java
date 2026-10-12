package ir.omid.smartteacher.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * رویداد دامنه — نمره زیر آستانه (تریگر خودکارسازی فاز ۹).
 */
public record ScoreBelowThresholdEvent(
        UUID tenantId,
        UUID examId,
        UUID attemptId,
        UUID studentId,
        BigDecimal score,
        BigDecimal totalScore,
        BigDecimal percent
) {
}
