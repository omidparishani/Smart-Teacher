package ir.omid.smartteacher.exam.internal.application;

import java.math.BigDecimal;

/**
 * خروجی ساختاریافته تصحیح AI برای سؤال تشریحی/کوتاه‌پاسخ.
 *
 * <p>Mock با {@code StructuredOutputFixtures} نمونه می‌سازد
 * (نمره ۱، بازخورد «نمونه»).
 */
public record AiEssayGradeResult(
        BigDecimal awardedScore,
        String feedback,
        boolean correct
) {
}
