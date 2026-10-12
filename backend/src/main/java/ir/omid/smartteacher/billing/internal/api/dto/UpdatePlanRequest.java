package ir.omid.smartteacher.billing.internal.api.dto;

import java.math.BigDecimal;

public record UpdatePlanRequest(
        String name,
        BigDecimal price
) {
}
