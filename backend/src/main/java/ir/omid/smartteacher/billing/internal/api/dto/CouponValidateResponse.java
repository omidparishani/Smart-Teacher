package ir.omid.smartteacher.billing.internal.api.dto;

import java.math.BigDecimal;

public record CouponValidateResponse(
        boolean valid,
        String message,
        BigDecimal discount
) {
}
