package ir.omid.smartteacher.billing.internal.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubscriptionResponse(
        UUID id,
        UUID planId,
        String planCode,
        String planName,
        String status,
        Instant startDate,
        Instant endDate,
        BigDecimal pricePaid,
        String invoiceNumber,
        String paymentAuthority,
        String redirectUrl
) {}
