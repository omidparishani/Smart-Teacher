package ir.omid.smartteacher.billing.internal.application;

import ir.omid.smartteacher.billing.EntitlementService;
import ir.omid.smartteacher.billing.FeatureNotEntitledException;
import ir.omid.smartteacher.billing.internal.domain.PlanFeature;
import ir.omid.smartteacher.billing.internal.domain.PlanFeatureRepository;
import ir.omid.smartteacher.billing.internal.domain.PlanLimit;
import ir.omid.smartteacher.billing.internal.domain.PlanLimitRepository;
import ir.omid.smartteacher.billing.internal.domain.Subscription;
import ir.omid.smartteacher.billing.internal.domain.SubscriptionRepository;
import ir.omid.smartteacher.common.cache.CacheBackend;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.tenant.TenantContext;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entitlement بر اساس اشتراک ACTIVE + {@code plan_features}.
 */
@Service
public class EntitlementServiceImpl implements EntitlementService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanLimitRepository planLimitRepository;
    private final PlanFeatureRepository planFeatureRepository;
    private final CacheBackend cache;
    private final Duration cacheTtl;

    public EntitlementServiceImpl(SubscriptionRepository subscriptionRepository,
                                   PlanLimitRepository planLimitRepository,
                                   PlanFeatureRepository planFeatureRepository,
                                   CacheBackend cache,
                                   @Value("${app.cache.entitlement-ttl:30s}") Duration cacheTtl) {
        this.subscriptionRepository = subscriptionRepository;
        this.planLimitRepository = planLimitRepository;
        this.planFeatureRepository = planFeatureRepository;
        this.cache = cache;
        this.cacheTtl = cacheTtl;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEnabled(String featureCode) {
        Optional<UUID> tenantId = TenantContext.currentTenantId();
        if (tenantId.isEmpty()) {
            return false;
        }
        String key = "ent:" + tenantId.get() + ":" + (featureCode == null ? "" : featureCode.trim().toUpperCase());
        return cache.getOrLoad(key, Boolean.class, cacheTtl,
                () -> isEnabledUncached(featureCode));
    }

    private boolean isEnabledUncached(String featureCode) {
        Optional<Subscription> active = subscriptionRepository.findCurrentActive()
                .filter(Subscription::isCurrentlyActive);
        if (active.isEmpty()) {
            return false;
        }
        if (featureCode == null || featureCode.isBlank()) {
            return true;
        }
        return planFeatureRepository
                .findByPlanIdAndFeatureCode(active.get().getPlanId(), featureCode.trim().toUpperCase())
                .map(PlanFeature::isEnabled)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireFeature(String featureCode) {
        Optional<Subscription> current = subscriptionRepository.findCurrentActive();
        boolean expired = current.isEmpty()
                || current.map(s -> !s.isCurrentlyActive()).orElse(true);
        if (expired) {
            throw FeatureNotEntitledException.expired();
        }
        if (!isEnabled(featureCode)) {
            throw new FeatureNotEntitledException(featureCode);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void requireStorageCapacity(long currentUsedBytes, long additionalBytes) {
        if (additionalBytes < 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "اندازه فایل نامعتبر است.");
        }
        Optional<Long> limit = findStorageBytesLimit();
        if (limit.isEmpty()) {
            return;
        }
        long projected = currentUsedBytes + additionalBytes;
        if (projected > limit.get()) {
            throw new AppException(ErrorCode.USAGE_LIMIT_REACHED,
                    "سقف فضای ذخیره‌سازی این پلن پر شده است.");
        }
    }

    private Optional<Long> findStorageBytesLimit() {
        return subscriptionRepository.findCurrentActive()
                .filter(Subscription::isCurrentlyActive)
                .flatMap(sub -> planLimitRepository.findByPlanId(sub.getPlanId()).stream()
                        .filter(pl -> METRIC_STORAGE_BYTES.equals(pl.getMetric()))
                        .map(PlanLimit::getLimitValue)
                        .findFirst());
    }
}
