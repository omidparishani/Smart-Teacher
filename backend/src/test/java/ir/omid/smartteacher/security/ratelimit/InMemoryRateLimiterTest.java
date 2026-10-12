package ir.omid.smartteacher.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * تست واحد محدودیت نرخ حافظه‌ای — بدون Spring/DB.
 */
class InMemoryRateLimiterTest {

    @Test
    @DisplayName("تا سقف مجاز true و بعد false")
    void allowsUntilLimitThenRejects() {
        InMemoryRateLimiter limiter = InMemoryRateLimiter.forTests(3, Duration.ofMinutes(1));
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    @DisplayName("کلیدهای جدا سهمیه مستقل دارند")
    void keysAreIndependent() {
        InMemoryRateLimiter limiter = InMemoryRateLimiter.forTests(1, Duration.ofMinutes(1));
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.tryAcquire("b")).isTrue();
    }

    @Test
    @DisplayName("پس از پایان پنجره دوباره مجاز است")
    void windowResets() throws InterruptedException {
        InMemoryRateLimiter limiter = InMemoryRateLimiter.forTests(1, Duration.ofMillis(80));
        assertThat(limiter.tryAcquire("w")).isTrue();
        assertThat(limiter.tryAcquire("w")).isFalse();
        Thread.sleep(120);
        assertThat(limiter.tryAcquire("w")).isTrue();
    }
}
