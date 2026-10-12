package ir.omid.smartteacher.ai.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.omid.smartteacher.ai.AiProvider;
import ir.omid.smartteacher.ai.AiProvider.AiMessage;
import ir.omid.smartteacher.ai.AiProvider.AiModelDescriptor;
import ir.omid.smartteacher.ai.AiProvider.AiRequest;
import ir.omid.smartteacher.ai.AiProvider.AiResponse;
import ir.omid.smartteacher.ai.AiProvider.ProviderHealth;
import ir.omid.smartteacher.ai.AiProviderException;
import ir.omid.smartteacher.ai.internal.domain.AiProviderEntity;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * پیاده‌سازی ارائه‌دهنده سازگار با OpenAI (شامل OpenAI، DeepSeek، Groq،
 * OpenRouter و سرویس‌های همساز با Chat Completions).
 *
 * <p>دو مسیر ساخت:
 * <ul>
 *   <li>bean محیطی وقتی {@code app.ai.openai-compatible.enabled=true}</li>
 *   <li>{@link #fromCatalog(AiProviderEntity)} برای Provider ثبت‌شده در پنل ادمین</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "app.ai.openai-compatible.enabled", havingValue = "true")
public class OpenAiCompatibleProvider extends AbstractHttpAiProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleProvider.class);

    public static final String CODE = "OPENAI_COMPATIBLE";

    private final ObjectMapper objectMapper;
    private final String code;
    private final String baseUrl;
    private final String apiKey;
    private final boolean enabled;

    @Autowired
    public OpenAiCompatibleProvider(
            @Value("${app.ai.openai-compatible.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.ai.openai-compatible.api-key:}") String apiKey,
            @Value("${app.ai.openai-compatible.enabled:true}") boolean enabled) {
        this(CODE, baseUrl, apiKey, enabled);
    }

    public OpenAiCompatibleProvider(String code, String baseUrl, String apiKey, boolean enabled) {
        super(createDefaultClient(Duration.ofSeconds(10)), Duration.ofSeconds(60), 1);
        this.objectMapper = new ObjectMapper();
        this.code = code != null && !code.isBlank() ? code.trim() : CODE;
        this.baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl.trim() : "https://api.openai.com/v1";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.enabled = enabled;
    }

    /**
     * Adapter اجرایی از رکورد کاتالوگ ادمین — بدون افشای کلید در Log.
     */
    public static OpenAiCompatibleProvider fromCatalog(AiProviderEntity entity) {
        String base = entity.getBaseUrl();
        if (base == null || base.isBlank()) {
            base = "https://api.openai.com/v1";
        }
        String key = entity.getApiKey() == null ? "" : entity.getApiKey();
        return new OpenAiCompatibleProvider(entity.getCode(), base, key, entity.isEnabled());
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    @Override
    protected boolean isHttpConfigured() {
        return !apiKey.isBlank();
    }

    @Override
    protected byte[] buildRequestPayload(AiRequest request) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            String model = (request.modelId() != null && !request.modelId().isBlank())
                    ? request.modelId()
                    : "gpt-4o-mini";
            body.put("model", model);

            List<Map<String, String>> messages = new ArrayList<>();
            if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
                messages.add(Map.of("role", "system", "content", request.systemPrompt()));
            }
            if (request.history() != null) {
                for (AiMessage msg : request.history()) {
                    messages.add(Map.of("role", msg.role(), "content", msg.content()));
                }
            }
            if (request.userPrompt() != null && !request.userPrompt().isBlank()) {
                messages.add(Map.of("role", "user", "content", request.userPrompt()));
            }
            body.put("messages", messages);

            if (request.temperature() != null) {
                body.put("temperature", request.temperature());
            }
            if (request.maxTokens() != null) {
                body.put("max_tokens", request.maxTokens());
            }
            if (request.topP() != null) {
                body.put("top_p", request.topP());
            }
            if (request.stopSequences() != null && !request.stopSequences().isEmpty()) {
                body.put("stop", request.stopSequences());
            }
            if (request.jsonSchema() != null && !request.jsonSchema().isBlank()) {
                body.put("response_format", Map.of("type", "json_object"));
            }

            return objectMapper.writeValueAsBytes(body);
        } catch (IOException e) {
            throw new AiProviderException(code(), "خطا در سریال‌سازی درخواست OpenAI: " + e.getMessage(), false, e);
        }
    }

    @Override
    protected AiResponse parseSuccessResponse(byte[] body, AiRequest request) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode choices = root.path("choices");
            String content = "";
            String finishReason = "stop";
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode first = choices.get(0);
                content = first.path("message").path("content").asText("");
                finishReason = first.path("finish_reason").asText("stop");
            }

            int inputTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int outputTokens = root.path("usage").path("completion_tokens").asInt(0);
            int cachedTokens = root.path("usage").path("prompt_tokens_details").path("cached_tokens").asInt(0);
            String modelId = root.path("model").asText(
                    request.modelId() != null && !request.modelId().isBlank() ? request.modelId() : "gpt-4o-mini");

            return new AiResponse(
                    content,
                    inputTokens,
                    outputTokens,
                    cachedTokens,
                    finishReason,
                    modelId,
                    Duration.ZERO,
                    Map.of("provider", code()));
        } catch (IOException e) {
            throw new AiProviderException(code(), "خطا در پارس پاسخ OpenAI: " + e.getMessage(), false, e);
        }
    }

    @Override
    protected URI resolveEndpoint(AiRequest request) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return URI.create(base + "/chat/completions");
    }

    @Override
    protected Map<String, String> buildRequestHeaders(AiRequest request) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        if (!apiKey.isBlank()) {
            headers.put("Authorization", "Bearer " + apiKey);
        }
        return headers;
    }

    @Override
    public AiResponse generate(AiRequest request) {
        if (!isHttpConfigured()) {
            log.warn("کلید API برای Provider «{}» تعریف نشده است؛ failover", code());
            throw new AiProviderException(code(),
                    "کلید API برای ارائه‌دهنده سازگار با OpenAI تنظیم نشده است",
                    true);
        }
        return executeHttp(request);
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
        return List.of(
                new AiModelDescriptor("gpt-4o", "GPT-4o", 128_000, 4_096, Set.of("JSON_MODE", "VISION")),
                new AiModelDescriptor("gpt-4o-mini", "GPT-4o Mini", 128_000, 16_384, Set.of("JSON_MODE", "VISION")),
                new AiModelDescriptor("deepseek-chat", "DeepSeek Chat", 64_000, 4_096, Set.of("JSON_MODE")));
    }

    @Override
    public ProviderHealth health() {
        Instant start = Instant.now();
        if (apiKey.isBlank()) {
            return new ProviderHealth(true,
                    "آماده اتصال (کلید API پیکربندی نشده است)",
                    Duration.ofMillis(1),
                    Instant.now());
        }

        try {
            String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            HttpRequest pingRequest = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/models"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();

            HttpResponse<Void> resp = httpClient.send(pingRequest, HttpResponse.BodyHandlers.discarding());
            Duration latency = Duration.between(start, Instant.now());
            if (resp.statusCode() == 200) {
                return new ProviderHealth(true, "اتصال به OpenAI برقرار است", latency, Instant.now());
            } else if (resp.statusCode() == 401) {
                return new ProviderHealth(false, "کلید API نامعتبر است (401)", latency, Instant.now());
            } else {
                return new ProviderHealth(false, "وضعیت غیرمنتظره HTTP " + resp.statusCode(), latency, Instant.now());
            }
        } catch (Exception e) {
            Duration latency = Duration.between(start, Instant.now());
            return new ProviderHealth(false, "عدم برقراری ارتباط: " + e.getMessage(), latency, Instant.now());
        }
    }
}
