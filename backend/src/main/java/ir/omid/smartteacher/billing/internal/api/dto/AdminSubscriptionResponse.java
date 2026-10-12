package ir.omid.smartteacher.billing.internal.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminSubscriptionResponse(
        UUID id,
        UUID tenantId,
        UUID planId,
        String planCode,
        String planName,
        String status,
        Instant startDate,
        Instant endDate,
        BigDecimal pricePaid,
        Instant createdAt
) {
}
