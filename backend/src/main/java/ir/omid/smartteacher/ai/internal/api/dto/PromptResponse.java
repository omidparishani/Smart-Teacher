package ir.omid.smartteacher.ai.internal.api.dto;

import java.util.UUID;

/** پاسخ قالب پرامپت. */
public record PromptResponse(
        UUID id,
        String code,
        String name,
        String featureCode,
        String description,
        boolean system
) {
}
