package ir.omid.smartteacher.security.ratelimit;

import java.time.Duration;
import java.util.function.BiFunction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * محدودیت نرخ مبتنی بر Redis — فقط وقتی {@code app.redis.enabled=true}.
 *
 * <p>بدون وابستگی اجباری به Redis در classpath تست‌ها: شمارنده از طریق
 * {@link RedisCounter} تزریق می‌شود؛ پیاده‌سازی واقعی می‌تواند Lettuce باشد
 * و تست واحد با mock کار می‌کند.
 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisRateLimiter implements RateLimiter {

    private final RedisCounter counter;
    private final int maxRequests;
    private final Duration window;

    public RedisRateLimiter(RedisCounter counter, LoginRateLimitProperties properties) {
        this.counter = counter;
        this.maxRequests = Math.max(1, properties.maxRequests());
        this.window = properties.window() == null ? Duration.ofMinutes(1) : properties.window();
    }

    /** سازندهٔ تست با شمارندهٔ ساختگی. */
    public static RedisRateLimiter forTests(RedisCounter counter, int maxRequests, Duration window) {
        return new RedisRateLimiter(counter, new LoginRateLimitProperties(true, maxRequests, window));
    }

    @Override
    public boolean tryAcquire(String key) {
        long count = counter.incrementAndGet("rl:" + key, window);
        return count <= maxRequests;
    }

    /**
     * قرارداد شمارنده Redis — INCR + EXPIRE در پیاده‌سازی واقعی.
     */
    @FunctionalInterface
    public interface RedisCounter {
        long incrementAndGet(String key, Duration ttl);
    }

    /**
     * شمارندهٔ حافظه‌ای که قرارداد Redis را شبیه‌سازی می‌کند — وقتی
     * Redis واقعی در محیط نیست ولی {@code app.redis.enabled=true}.
     */
    @Component
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
    public static class InProcessRedisCounter implements RedisCounter {

        private final java.util.concurrent.ConcurrentHashMap<String, Entry> map =
                new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public long incrementAndGet(String key, Duration ttl) {
            long now = System.currentTimeMillis();
            long ttlMs = Math.max(1L, ttl.toMillis());
            Entry entry = map.compute(key, (BiFunction<String, Entry, Entry>) (k, existing) -> {
                if (existing == null || now - existing.startedAt >= ttlMs) {
                    return new Entry(now, 1L);
                }
                return new Entry(existing.startedAt, existing.count + 1);
            });
            return entry.count;
        }

        private record Entry(long startedAt, long count) {
        }
    }
}
