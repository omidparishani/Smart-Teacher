package ir.omid.smartteacher.billing.internal.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CouponResponse(
        UUID id,
        String code,
        String discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minPurchaseAmount,
        Integer usageLimit,
        Integer usageLimitPerTenant,
        int usedCount,
        Instant validFrom,
        Instant validUntil,
        UUID applicablePlanId,
        boolean active
) {
}
