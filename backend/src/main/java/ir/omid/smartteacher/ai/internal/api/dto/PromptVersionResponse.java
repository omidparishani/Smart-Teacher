package ir.omid.smartteacher.ai.internal.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** پاسخ نسخه پرامپت. */
public record PromptVersionResponse(
        UUID id,
        UUID promptId,
        int versionNumber,
        String systemPrompt,
        String userPrompt,
        String variables,
        String responseSchema,
        BigDecimal temperature,
        Integer maxTokens,
        String status,
        Instant publishedAt,
        String changeNote
) {
}
