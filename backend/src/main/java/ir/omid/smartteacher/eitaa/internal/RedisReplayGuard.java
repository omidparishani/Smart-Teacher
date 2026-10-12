package ir.omid.smartteacher.eitaa.internal;

import ir.omid.smartteacher.eitaa.ReplayGuard;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * نگه‌بان ضدبازپخش مبتنی بر Redis — فقط وقتی {@code app.redis.enabled=true}.
 *
 * <p>در غیاب کلاینت خارجی Redis در کلس‌پث، ارتباط از طریق قرارداد
 * {@link RedisReplayStore} برقرار می‌شود که با پیاده‌سازی درون‌فرایندی
 * یا اتصال واقعی در محیط اجرا کار می‌کند.
 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisReplayGuard implements ReplayGuard {

    private static final Logger log = LoggerFactory.getLogger(RedisReplayGuard.class);

    private final RedisReplayStore store;

    public RedisReplayGuard(RedisReplayStore store) {
        this.store = store;
    }

    @Override
    public boolean markUsed(String fingerprint, Duration window) {
        if (fingerprint == null || fingerprint.isBlank()) {
            log.warn("اثر انگشت initData خالی بود — برای جلوگیری از بازپخش، رد شد");
            return false;
        }
        return store.setIfAbsent("replay:" + fingerprint, window);
    }

    @Override
    public int purgeExpired() {
        return store.purgeExpired();
    }

    /**
     * قرارداد عملیات Redis برای ضدبازپخش.
     */
    public interface RedisReplayStore {
        boolean setIfAbsent(String key, Duration ttl);
        int purgeExpired();
    }

    /**
     * پیاده‌سازی شبیه‌ساز Redis وقتی {@code app.redis.enabled=true} است.
     */
    @Component
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
    public static class InProcessRedisReplayStore implements RedisReplayStore {

        private final Map<String, Instant> entries = new ConcurrentHashMap<>();

        @Override
        public boolean setIfAbsent(String key, Duration ttl) {
            Instant now = Instant.now();
            Instant expiresAt = now.plus(ttl);
            Instant previous = entries.putIfAbsent(key, expiresAt);
            if (previous == null) {
                return true;
            }
            if (!previous.isAfter(now)) {
                entries.put(key, expiresAt);
                return true;
            }
            return false;
        }

        @Override
        public int purgeExpired() {
            Instant now = Instant.now();
            int before = entries.size();
            entries.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
            return before - entries.size();
        }
    }
}
