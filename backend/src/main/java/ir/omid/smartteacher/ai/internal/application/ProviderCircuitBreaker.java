package ir.omid.smartteacher.ai.internal.application;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Circuit Breaker درون‌حافظه‌ای برای یک Provider — CLOSED / OPEN / HALF_OPEN.
 */
public class ProviderCircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final int failureThreshold;
    private final Duration cooldown;

    private volatile State state = State.CLOSED;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile Instant openedAt;

    public ProviderCircuitBreaker(int failureThreshold, Duration cooldown) {
        this.failureThreshold = Math.max(1, failureThreshold);
        this.cooldown = cooldown == null || cooldown.isNegative() || cooldown.isZero()
                ? Duration.ofSeconds(30)
                : cooldown;
    }

    /** آیا فراخوانی Provider مجاز است؟ */
    public boolean allowCall() {
        return switch (state) {
            case CLOSED -> true;
            case OPEN -> {
                if (Duration.between(openedAt, Instant.now()).compareTo(cooldown) >= 0) {
                    state = State.HALF_OPEN;
                    yield true;
                }
                yield false;
            }
            case HALF_OPEN -> true;
        };
    }

    public void recordSuccess() {
        consecutiveFailures.set(0);
        state = State.CLOSED;
    }

    public void recordFailure() {
        if (state == State.HALF_OPEN) {
            open();
            return;
        }
        if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
            open();
        }
    }

    public State state() {
        return state;
    }

    public int consecutiveFailures() {
        return consecutiveFailures.get();
    }

    /** فقط برای تست — ریست کامل. */
    public void resetForTest() {
        consecutiveFailures.set(0);
        state = State.CLOSED;
        openedAt = null;
    }

    private void open() {
        state = State.OPEN;
        openedAt = Instant.now();
    }
}
