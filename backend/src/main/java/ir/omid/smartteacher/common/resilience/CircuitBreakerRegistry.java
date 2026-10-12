package ir.omid.smartteacher.common.resilience;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * نگهداری Circuit Breaker per-provider برای {@link ir.omid.smartteacher.ai.internal.application.ProviderBroker}.
 */
@Component
public class CircuitBreakerRegistry {

    private final ConcurrentHashMap<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();
    private final int failureThreshold;
    private final Duration openDuration;

    public CircuitBreakerRegistry(
            @Value("${app.resilience.circuit-breaker.failure-threshold:5}") int failureThreshold,
            @Value("${app.resilience.circuit-breaker.open-duration:30s}") Duration openDuration) {
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
    }

    public CircuitBreaker forProvider(String providerCode) {
        String key = providerCode == null ? "unknown" : providerCode.trim().toLowerCase();
        return breakers.computeIfAbsent(key, k ->
                new CircuitBreaker(failureThreshold, openDuration));
    }
}
