package ir.omid.smartteacher.billing.internal.application;

import ir.omid.smartteacher.billing.internal.platform.BillingPlatformQueries;
import ir.omid.smartteacher.billing.internal.platform.BillingPlatformQueries.ExpiringSubscriptionRow;
import ir.omid.smartteacher.common.event.SubscriptionExpiringEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * هشدار انقضای اشتراک — ۷، ۳ و ۱ روز قبل از {@code end_date}.
 *
 * <p>رویداد {@link SubscriptionExpiringEvent} برای خودکارسازی فاز ۹ منتشر می‌شود.
 * dedup در حافظه بر اساس (subscription, days, UTC day) تا بدون migration تکرار نشود.
 */
@Component
public class SubscriptionExpiryWarningJob {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiryWarningJob.class);

    private static final Set<Integer> WARNING_DAYS = Set.of(7, 3, 1);

    private final BillingPlatformQueries billingPlatformQueries;
    private final ApplicationEventPublisher events;
    private final ConcurrentHashMap<String, Boolean> warnedToday = new ConcurrentHashMap<>();

    public SubscriptionExpiryWarningJob(BillingPlatformQueries billingPlatformQueries,
                                        ApplicationEventPublisher events) {
        this.billingPlatformQueries = billingPlatformQueries;
        this.events = events;
    }

    @Scheduled(fixedDelayString = "${app.billing.expiry-warning-job-delay:3600000}")
    @Transactional(readOnly = true)
    public void warnExpiringSubscriptions() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        pruneOldDedupKeys(todayUtc);

        for (int days : WARNING_DAYS) {
            LocalDate expiryDay = todayUtc.plusDays(days);
            List<ExpiringSubscriptionRow> rows =
                    billingPlatformQueries.findActiveExpiringOn(expiryDay);
            for (ExpiringSubscriptionRow row : rows) {
                String dedupKey = row.subscriptionId() + ":" + days + ":" + todayUtc;
                if (warnedToday.putIfAbsent(dedupKey, Boolean.TRUE) != null) {
                    continue;
                }
                log.info("اشتراک {} در {} روز منقضی می‌شود (tenant={})",
                        row.subscriptionId(), days, row.tenantId());
                events.publishEvent(new SubscriptionExpiringEvent(
                        row.tenantId(),
                        row.subscriptionId(),
                        row.endDate(),
                        days));
            }
        }
    }

    private void pruneOldDedupKeys(LocalDate todayUtc) {
        String todaySuffix = ":" + todayUtc;
        warnedToday.keySet().removeIf(key -> !key.endsWith(todaySuffix));
    }

    /** فقط برای تست. */
    void clearDedupCache() {
        warnedToday.clear();
    }
}
