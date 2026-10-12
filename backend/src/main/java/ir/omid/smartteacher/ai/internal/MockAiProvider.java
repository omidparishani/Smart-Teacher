package ir.omid.smartteacher.ai.internal;

import ir.omid.smartteacher.ai.AiProvider;
import ir.omid.smartteacher.ai.AiProviderException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Mock اصلی که می‌تواند برای تست failover یک‌بار شکست بخورد.
 *
 * <p>جایگزین bean قبلی نیست — همان {@link MockAiProvider} با قابلیت
 * {@link #armFailure()} گسترش یافته است.
 */
@Component
@Primary
@Order(1)
@ConditionalOnProperty(name = "app.mock.ai", havingValue = "true")
public class MockAiProvider implements AiProvider {

    public static final String CODE = "MOCK";
    public static final String DEFAULT_MODEL = "mock-chat";

    private final AtomicBoolean failNext = new AtomicBoolean(false);
    private final AtomicBoolean persistentFailure = new AtomicBoolean(false);
    private final AtomicInteger generateInvocations = new AtomicInteger(0);

    /** برای تست: درخواست بعدی با خطای قابل‌failover شکست می‌خورد. */
    public void armFailure() {
        failNext.set(true);
    }

    /** برای تست Circuit Breaker: همهٔ درخواست‌ها تا disarm شکست می‌خورند. */
    public void armPersistentFailure() {
        persistentFailure.set(true);
    }

    public void disarmPersistentFailure() {
        persistentFailure.set(false);
    }

    /** تعداد فراخوانی generate — فقط برای تست. */
    public int generateInvocationCount() {
        return generateInvocations.get();
    }

    public void resetInvocationCountForTest() {
        generateInvocations.set(0);
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
        generateInvocations.incrementAndGet();
        if (persistentFailure.get()) {
            throw new AiProviderException(CODE, "Mock persistent failure", true);
        }
        if (failNext.compareAndSet(true, false)) {
            throw new AiProviderException(CODE, "Mock عمداً شکست خورد", true);
        }

        String prompt = request.userPrompt() == null ? "" : request.userPrompt().trim();
        String summary = summarize(prompt);
        String content = "پاسخ آزمایشی برای: " + summary;
        String modelId = request.modelId() == null || request.modelId().isBlank()
                ? DEFAULT_MODEL
                : request.modelId();

        int inputTokens = Math.max(1, prompt.length() / 4);
        int outputTokens = Math.max(1, content.length() / 4);

        return new AiResponse(
                content,
                inputTokens,
                outputTokens,
                0,
                "stop",
                modelId,
                Duration.ofMillis(120),
                Map.of("mock", true));
    }

    @Override
    public <T> AiStructuredResponse<T> generateStructured(AiRequest request, Class<T> responseType) {
        T sample = StructuredOutputFixtures.createSample(responseType);
        return new AiStructuredResponse<>(
                sample,
                "{}",
                true,
                100,
                50,
                0,
                Duration.ofMillis(120),
                List.of());
    }

    @Override
    public boolean supports(String capability) {
        return switch (capability) {
            case "JSON_MODE", "STREAMING" -> true;
            default -> false;
        };
    }

    @Override
    public List<AiModelDescriptor> models() {
        return List.of(new AiModelDescriptor(
                DEFAULT_MODEL,
                "Mock Chat",
                8_192,
                2_048,
                Set.of("JSON_MODE")));
    }

    @Override
    public ProviderHealth health() {
        return new ProviderHealth(true, "Mock آماده است", Duration.ofMillis(1), Instant.now());
    }

    private static String summarize(String prompt) {
        if (prompt.isEmpty()) {
            return "(خالی)";
        }
        if (prompt.length() <= 80) {
            return prompt;
        }
        return prompt.substring(0, 77) + "...";
    }
}
