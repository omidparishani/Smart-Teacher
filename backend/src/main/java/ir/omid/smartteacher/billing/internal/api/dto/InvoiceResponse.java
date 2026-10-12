package ir.omid.smartteacher.billing.internal.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String invoiceNumber,
        UUID subscriptionId,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        String currency,
        String status,
        Instant issuedAt
) {
}
