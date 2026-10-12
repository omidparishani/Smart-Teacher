package ir.omid.smartteacher.billing.internal.api.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UpdateCouponRequest(
        @DecimalMin(value = "0.01", message = "مقدار تخفیف باید مثبت باشد")
        BigDecimal discountValue,
        Instant validFrom,
        Instant validUntil,
        BigDecimal maxDiscountAmount,
        BigDecimal minPurchaseAmount,
        Integer usageLimit,
        Integer usageLimitPerTenant,
        UUID applicablePlanId,
        Boolean active
) {
}
