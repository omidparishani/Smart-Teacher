package ir.omid.smartteacher.ai.internal;

import ir.omid.smartteacher.ai.AiProvider;
import ir.omid.smartteacher.ai.AiProviderException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Mock دوم برای تست failover — با {@link #armFailure()} یک بار شکست
 * قابل‌جایگزینی می‌دهد.
 *
 * <p>کد: {@code MOCK_SECONDARY}. در مسیر عادی موفق است.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.mock.ai", havingValue = "true")
public class SecondaryMockAiProvider implements AiProvider {

    public static final String CODE = "MOCK_SECONDARY";

    private final AtomicBoolean failNext = new AtomicBoolean(false);

    /** برای تست: درخواست بعدی با خطای قابل‌failover شکست می‌خورد. */
    public void armFailure() {
        failNext.set(true);
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public AiResponse generate(AiRequest request) {
        if (failNext.compareAndSet(true, false)) {
            throw new AiProviderException(CODE, "Mock ثانویه عمداً شکست خورد", true);
        }
        String prompt = request.userPrompt() == null ? "" : request.userPrompt().trim();
        String content = "پاسخ ثانویه برای: " + (prompt.isEmpty() ? "(خالی)" : prompt);
        String modelId = request.modelId() == null || request.modelId().isBlank()
                ? "mock-secondary"
                : request.modelId();
        return new AiResponse(
                content,
                Math.max(1, prompt.length() / 4),
                Math.max(1, content.length() / 4),
                0,
                "stop",
                modelId,
                Duration.ofMillis(80),
                Map.of("mock", true, "secondary", true));
    }

    @Override
    public <T> AiStructuredResponse<T> generateStructured(AiRequest request, Class<T> responseType) {
        T sample = StructuredOutputFixtures.createSample(responseType);
        return new AiStructuredResponse<>(
                sample, "{}", true, 50, 25, 0, Duration.ofMillis(80), List.of());
    }

    @Override
    public boolean supports(String capability) {
        return "JSON_MODE".equals(capability);
    }

    @Override
    public List<AiModelDescriptor> models() {
        return List.of(new AiModelDescriptor(
                "mock-secondary", "Mock Secondary", 4_096, 1_024, Set.of("JSON_MODE")));
    }

    @Override
    public ProviderHealth health() {
        return new ProviderHealth(true, "Mock ثانویه آماده است", Duration.ofMillis(1), Instant.now());
    }
}
