package ir.omid.smartteacher.ai.internal.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * پاسخ مدل کاتالوگ برای ادمین.
 */
public record AiModelResponse(
        UUID id,
        UUID providerId,
        String code,
        String displayName,
        boolean enabled,
        BigDecimal inputCostPerMillion,
        BigDecimal outputCostPerMillion,
        String currency
) {
}
