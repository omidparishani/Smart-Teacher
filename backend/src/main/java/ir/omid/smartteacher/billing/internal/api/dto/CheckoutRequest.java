package ir.omid.smartteacher.billing.internal.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * درخواست خرید اشتراک.
 *
 * @param planId     شناسه پلن
 * @param couponCode کد تخفیف اختیاری
 */
public record CheckoutRequest(
        @NotNull UUID planId,
        String couponCode
) {
    public CheckoutRequest(UUID planId) {
        this(planId, null);
    }
}
