package ir.omid.smartteacher.billing.internal.application;

import ir.omid.smartteacher.billing.internal.domain.Subscription;
import ir.omid.smartteacher.billing.internal.domain.SubscriptionRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * جاب انقضای اشتراک‌های ACTIVE که end_date گذشته‌اند.
 */
@Component
public class SubscriptionExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiryJob.class);

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionExpiryJob(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Scheduled(fixedDelayString = "${app.billing.expiry-job-delay:60000}")
    @Transactional
    public void expireSubscriptions() {
        Instant now = Instant.now();
        List<Subscription> expired = subscriptionRepository.findActivePastEndDate(now);
        for (Subscription sub : expired) {
            sub.setStatus("EXPIRED");
            log.info("اشتراک {} منقضی شد (endDate={})", sub.getId(), sub.getEndDate());
        }
    }
}
