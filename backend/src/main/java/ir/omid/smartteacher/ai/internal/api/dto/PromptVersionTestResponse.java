package ir.omid.smartteacher.ai.internal.api.dto;

/** نتیجه تست playground نسخه. */
public record PromptVersionTestResponse(
        String renderedSystemPrompt,
        String renderedUserPrompt,
        String rawResponse,
        String provider,
        String model,
        Integer promptTokens,
        Integer completionTokens,
        long durationMs
) {
}
