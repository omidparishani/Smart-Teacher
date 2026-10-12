package ir.omid.smartteacher.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * رویداد دامنه — اشتراک به‌زودی منقضی می‌شود (تریگر خودکارسازی فاز ۹).
 */
public record SubscriptionExpiringEvent(
        UUID tenantId,
        UUID subscriptionId,
        Instant endDate,
        int daysRemaining
) {
}
