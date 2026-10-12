package ir.omid.smartteacher.common.resilience;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Circuit Breaker در حافظه — CLOSED → OPEN → HALF_OPEN.
 *
 * <p>پس از {@code failureThreshold} خطای متوالی، مدار تا {@code openDuration} باز می‌ماند.
 */
public final class CircuitBreaker {

    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    private final int failureThreshold;
    private final Duration openDuration;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private volatile Instant openedAt = Instant.EPOCH;

    public CircuitBreaker(int failureThreshold, Duration openDuration) {
        this.failureThreshold = Math.max(1, failureThreshold);
        this.openDuration = openDuration == null ? Duration.ofSeconds(30) : openDuration;
    }

    public State state() {
        transitionIfNeeded();
        return state.get();
    }

    public <T> T execute(Supplier<T> action, Supplier<T> fallback) {
        transitionIfNeeded();
        State current = state.get();
        if (current == State.OPEN) {
            return fallback.get();
        }
        try {
            T result = action.get();
            onSuccess();
            return result;
        } catch (RuntimeException ex) {
            onFailure();
            throw ex;
        }
    }

    public boolean allowCall() {
        transitionIfNeeded();
        return state.get() != State.OPEN;
    }

    public void onSuccess() {
        consecutiveFailures.set(0);
        state.set(State.CLOSED);
    }

    public void onFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= failureThreshold) {
            state.set(State.OPEN);
            openedAt = Instant.now();
        }
    }

    private void transitionIfNeeded() {
        if (state.get() == State.OPEN
                && Instant.now().isAfter(openedAt.plus(openDuration))) {
            state.compareAndSet(State.OPEN, State.HALF_OPEN);
            consecutiveFailures.set(0);
        }
    }
}
