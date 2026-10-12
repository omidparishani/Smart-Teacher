package ir.omid.smartteacher.billing.internal.application;

import ir.omid.smartteacher.billing.UsageMetering;
import ir.omid.smartteacher.billing.internal.domain.PlanLimit;
import ir.omid.smartteacher.billing.internal.domain.PlanLimitRepository;
import ir.omid.smartteacher.billing.internal.domain.Subscription;
import ir.omid.smartteacher.billing.internal.domain.SubscriptionRepository;
import ir.omid.smartteacher.billing.internal.domain.UsageCounter;
import ir.omid.smartteacher.billing.internal.domain.UsageCounterRepository;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.tenant.TenantContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * رزرو/مصرف اتمیک روی {@code usage_counters}.
 */
@Service
public class UsageMeteringService implements UsageMetering {

    private final UsageCounterRepository usageCounterRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanLimitRepository planLimitRepository;

    public UsageMeteringService(UsageCounterRepository usageCounterRepository,
                                SubscriptionRepository subscriptionRepository,
                                PlanLimitRepository planLimitRepository) {
        this.usageCounterRepository = usageCounterRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planLimitRepository = planLimitRepository;
    }

    /**
     * یک واحد از متریک را مصرف می‌کند؛ اگر سقف پر باشد پرتاب می‌کند.
     */
    @Override
    @Transactional
    public void consume(String metric, long units) {
        if (units <= 0) {
            return;
        }
        TenantContext.requireTenantId();

        Subscription sub = subscriptionRepository.findCurrentActive()
                .filter(Subscription::isCurrentlyActive)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_EXPIRED));

        PlanLimit limit = planLimitRepository.findByPlanId(sub.getPlanId()).stream()
                .filter(pl -> metric.equals(pl.getMetric()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.USAGE_LIMIT_REACHED,
                        "سقف این متریک در پلن تعریف نشده است."));

        Instant periodStart = periodStart(limit.getPeriod());
        Instant periodEnd = periodEnd(limit.getPeriod(), periodStart);

        UsageCounter counter = loadOrCreateForUpdate(metric, limit.getLimitValue(), periodStart, periodEnd);

        if (counter.getLimitValue() != limit.getLimitValue()) {
            counter.setLimitValue(limit.getLimitValue());
        }

        if (counter.getUsedValue() + counter.getReservedValue() + units > counter.getLimitValue()) {
            throw new AppException(ErrorCode.USAGE_LIMIT_REACHED);
        }
        counter.setUsedValue(counter.getUsedValue() + units);
    }

    private UsageCounter loadOrCreateForUpdate(
            String metric, long limitValue, Instant periodStart, Instant periodEnd) {
        return usageCounterRepository.findTenantMetricForUpdate(metric, periodStart)
                .orElseGet(() -> usageCounterRepository.saveAndFlush(
                        new UsageCounter(metric, limitValue, periodStart, periodEnd)));
    }

    private static Instant periodStart(String period) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return switch (period == null ? "MONTHLY" : period) {
            case "DAILY" -> today.atStartOfDay(ZoneOffset.UTC).toInstant();
            case "YEARLY" -> today.withDayOfYear(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            case "TOTAL" -> Instant.EPOCH;
            default -> today.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        };
    }

    private static Instant periodEnd(String period, Instant start) {
        return switch (period == null ? "MONTHLY" : period) {
            case "DAILY" -> start.plus(1, ChronoUnit.DAYS);
            case "YEARLY" -> start.plus(365, ChronoUnit.DAYS);
            case "TOTAL" -> Instant.parse("9999-12-31T00:00:00Z");
            default -> start.plus(31, ChronoUnit.DAYS);
        };
    }
}
