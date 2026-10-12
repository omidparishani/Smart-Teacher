package ir.omid.smartteacher.billing.internal.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WalletAmountRequest(
        @NotNull @DecimalMin("0.0001") BigDecimal amount,
        String description
) {
}
