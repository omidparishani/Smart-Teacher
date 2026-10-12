package ir.omid.smartteacher.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RedisRateLimiter با شمارندهٔ mock — بدون Redis زنده.
 */
class RedisRateLimiterTest {

    @Test
    @DisplayName("rate limiter مبتنی بر RedisCounter همچنان سقف را اعمال می‌کند")
    void redisRateLimiter_stillWorksWithMockCounter() {
        AtomicLong seq = new AtomicLong();
        RedisRateLimiter.RedisCounter counter = (key, ttl) -> seq.incrementAndGet();

        RedisRateLimiter limiter = RedisRateLimiter.forTests(counter, 2, Duration.ofMinutes(1));

        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
    }
}
