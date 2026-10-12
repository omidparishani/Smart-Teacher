package ir.omid.smartteacher.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * محاسبه ساده هزینه توکن ({@code docs/ai-providers.md} §۸).
 *
 * <p>در گام ۱ فقط stub است: اگر نرخ مدل مشخص نباشد، صفر برمی‌گرداند
 * تا ثبت مصرف بدون قیمت‌گذاری شکست نخورد. محاسبه کامل با Model Catalog
 * در گام‌های بعدی فاز ۵ تکمیل می‌شود.
 */
@Component
public class AiCostCalculator {

    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);
    private static final int SCALE = 10;

    /**
     * هزینه واقعی بر اساس توکن‌های گزارش‌شده.
     *
     * @param inputTokens          توکن ورودی؛ {@code null} = ۰
     * @param outputTokens         توکن خروجی؛ {@code null} = ۰
     * @param cachedTokens         توکن کش‌شده؛ {@code null} = ۰
     * @param inputCostPerMillion  نرخ ورودی به ازای یک میلیون؛ nullable
     * @param outputCostPerMillion نرخ خروجی؛ nullable
     * @param cachedCostPerMillion نرخ کش؛ nullable
     */
    public BigDecimal calculate(
            Integer inputTokens,
            Integer outputTokens,
            Integer cachedTokens,
            BigDecimal inputCostPerMillion,
            BigDecimal outputCostPerMillion,
            BigDecimal cachedCostPerMillion) {

        BigDecimal input = costOf(inputTokens, inputCostPerMillion);
        BigDecimal cached = costOf(cachedTokens, cachedCostPerMillion);
        BigDecimal output = costOf(outputTokens, outputCostPerMillion);
        return input.add(cached).add(output);
    }

    /**
     * نسخه ساده‌شده برای Mock — بدون نرخ مدل، هزینه صفر.
     */
    public BigDecimal calculateOrZero(AiProvider.AiResponse response) {
        Objects.requireNonNull(response, "response");
        return calculate(
                response.inputTokens(),
                response.outputTokens(),
                response.cachedTokens(),
                null, null, null);
    }

    private static BigDecimal costOf(Integer tokens, BigDecimal ratePerMillion) {
        if (tokens == null || tokens <= 0 || ratePerMillion == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(tokens)
                .divide(MILLION, SCALE, RoundingMode.HALF_UP)
                .multiply(ratePerMillion);
    }
}
