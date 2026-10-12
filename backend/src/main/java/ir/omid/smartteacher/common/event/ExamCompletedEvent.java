package ir.omid.smartteacher.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * رویداد دامنه — تلاش آزمون ارسال و تصحیح شد (تریگر خودکارسازی فاز ۹).
 */
public record ExamCompletedEvent(
        UUID tenantId,
        UUID examId,
        UUID attemptId,
        UUID studentId,
        BigDecimal score,
        BigDecimal totalScore
) {
}
