package ir.omid.smartteacher.ai.internal.application;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * نگهداری Circuit Breaker per provider code — درون‌حافظه.
 */
@Component
public class ProviderCircuitBreakerRegistry {

    private final ConcurrentHashMap<String, ProviderCircuitBreaker> breakers =
            new ConcurrentHashMap<>();

    private final int failureThreshold;
    private final Duration cooldown;

    public ProviderCircuitBreakerRegistry(
            @Value("${app.ai.circuit-breaker.failure-threshold:5}") int failureThreshold,
            @Value("${app.ai.circuit-breaker.cooldown-ms:30000}") long cooldownMs) {
        this.failureThreshold = failureThreshold;
        this.cooldown = Duration.ofMillis(cooldownMs);
    }

    public ProviderCircuitBreaker forProvider(String providerCode) {
        String key = providerCode == null ? "" : providerCode.toUpperCase();
        return breakers.computeIfAbsent(key,
                k -> new ProviderCircuitBreaker(failureThreshold, cooldown));
    }

    /** فقط برای تست. */
    public void resetAllForTest() {
        breakers.values().forEach(ProviderCircuitBreaker::resetForTest);
    }
}
