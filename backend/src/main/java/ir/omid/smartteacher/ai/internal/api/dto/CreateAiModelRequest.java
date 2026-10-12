package ir.omid.smartteacher.ai.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateAiModelRequest(
        @NotNull(message = "شناسه Provider الزامی است")
        UUID providerId,
        @NotBlank(message = "کد مدل الزامی است")
        @Size(max = 100, message = "کد مدل حداکثر ۱۰۰ کاراکتر است")
        String code,
        @NotBlank(message = "نام نمایشی مدل الزامی است")
        @Size(max = 150, message = "نام نمایشی حداکثر ۱۵۰ کاراکتر است")
        String displayName,
        Boolean enabled,
        BigDecimal inputCostPerMillion,
        BigDecimal outputCostPerMillion,
        String currency
) {
}
