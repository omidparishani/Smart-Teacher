package ir.omid.smartteacher.security.ratelimit;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * محدودیت نرخ در حافظه — پیش‌فرض وقتی {@code app.redis.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    private final int maxRequests;
    private final long windowMillis;

    public InMemoryRateLimiter(LoginRateLimitProperties properties) {
        this.maxRequests = Math.max(1, properties.maxRequests());
        Duration window = properties.window() == null ? Duration.ofMinutes(1) : properties.window();
        this.windowMillis = Math.max(1L, window.toMillis());
    }

    public static InMemoryRateLimiter forTests(int maxRequests, Duration window) {
        return new InMemoryRateLimiter(new LoginRateLimitProperties(true, maxRequests, window));
    }

    @Override
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.startedAtMillis >= windowMillis) {
                return new Window(now, new AtomicInteger(0));
            }
            return existing;
        });
        int current = window.count.incrementAndGet();
        return current <= maxRequests;
    }

    public void reset() {
        windows.clear();
    }

    private record Window(long startedAtMillis, AtomicInteger count) {
    }
}
