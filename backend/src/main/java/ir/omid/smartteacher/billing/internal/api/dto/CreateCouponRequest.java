package ir.omid.smartteacher.billing.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateCouponRequest(
        @NotBlank String code,
        @NotBlank String discountType,
        @NotNull BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minPurchaseAmount,
        Integer usageLimit,
        Integer usageLimitPerTenant,
        @NotNull Instant validFrom,
        @NotNull Instant validUntil,
        UUID applicablePlanId
) {
}
