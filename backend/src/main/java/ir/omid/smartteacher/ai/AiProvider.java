package ir.omid.smartteacher.ai;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * اینترفیس واحد همه Providerهای هوش مصنوعی ({@code docs/ai-providers.md} §۲).
 *
 * <p>پیاده‌سازی واقعی HTTP در گام‌های بعدی می‌آید. در این گام فقط
 * {@link ir.omid.smartteacher.ai.internal.MockAiProvider} وجود دارد.
 */
public interface AiProvider {

    /** کد یکتا — با {@code ai_providers.code} مطابقت دارد. */
    String code();

    /** آیا این Provider پیکربندی شده و فعال است. */
    boolean isAvailable();

    /**
     * تولید متن آزاد (تک‌نوبت یا با تاریخچه).
     *
     * <p>{@link #chat(AiRequest)} و {@link #complete(AiRequest)} پیش‌فرض
     * به همین متد وصل‌اند تا Endpoint معلم و فراخوانی‌های داخلی یک مسیر
     * داشته باشند.
     */
    AiResponse generate(AiRequest request);

    /** گفت‌وگوی دستیار — پیش‌فرض همان {@link #generate(AiRequest)}. */
    default AiResponse chat(AiRequest request) {
        return generate(request);
    }

    /** تکمیل تک‌نوبت — پیش‌فرض همان {@link #generate(AiRequest)}. */
    default AiResponse complete(AiRequest request) {
        return generate(request);
    }

    /** تحلیل — پیش‌فرض همان تولید با پرامپت تحلیلی. */
    default AiResponse analyze(AiRequest request) {
        return generate(request);
    }

    /**
     * تولید خروجی ساختاریافته.
     *
     * <p>در Mock از fixture ساخته می‌شود؛ Adapter واقعی پس از تأیید
     * مستندات رسمی می‌آید.
     */
    <T> AiStructuredResponse<T> generateStructured(AiRequest request, Class<T> responseType);

    /** آیا قابلیت مشخصی پشتیبانی می‌شود: VISION|JSON_MODE|TOOL_USE|… */
    boolean supports(String capability);

    /** فهرست مدل‌های موجود از دید همین Provider. */
    List<AiModelDescriptor> models();

    /** بررسی سلامت. */
    ProviderHealth health();

    // ─── مدل‌های درخواست/پاسخ (عمومی برای ماژول‌های دیگر) ───

    record AiMessage(String role, String content) {
    }

    record AiRequest(
            String modelId,
            String systemPrompt,
            String userPrompt,
            List<AiMessage> history,
            Double temperature,
            Integer maxTokens,
            Double topP,
            List<String> stopSequences,
            String jsonSchema,
            String traceId,
            Duration timeout,
            Map<String, Object> providerOptions
    ) {
        public static AiRequest ofUserPrompt(String modelId, String userPrompt) {
            return new AiRequest(
                    modelId, null, userPrompt, List.of(),
                    null, null, null, List.of(),
                    null, null, null, Map.of());
        }
    }

    record AiResponse(
            String content,
            Integer inputTokens,
            Integer outputTokens,
            Integer cachedTokens,
            String finishReason,
            String modelId,
            Duration duration,
            Map<String, Object> rawMetadata
    ) {
    }

    record AiStructuredResponse<T>(
            T parsed,
            String rawContent,
            boolean schemaValid,
            Integer inputTokens,
            Integer outputTokens,
            Integer cachedTokens,
            Duration duration,
            List<String> validationErrors
    ) {
    }

    record AiModelDescriptor(
            String modelId,
            String displayName,
            Integer contextWindow,
            Integer maxOutputTokens,
            java.util.Set<String> capabilities
    ) {
    }

    record ProviderHealth(
            boolean healthy,
            String message,
            Duration latency,
            java.time.Instant checkedAt
    ) {
    }
}
