package ir.omid.smartteacher.billing.internal.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID tenantId,
        BigDecimal balance,
        String currency
) {
}
