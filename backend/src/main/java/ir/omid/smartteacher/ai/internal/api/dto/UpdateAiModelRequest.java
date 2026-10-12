package ir.omid.smartteacher.ai.internal.api.dto;

import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateAiModelRequest(
        UUID providerId,
        @Size(max = 100, message = "کد مدل حداکثر ۱۰۰ کاراکتر است")
        String code,
        @Size(max = 150, message = "نام نمایشی حداکثر ۱۵۰ کاراکتر است")
        String displayName,
        Boolean enabled,
        BigDecimal inputCostPerMillion,
        BigDecimal outputCostPerMillion,
        String currency
) {
}
