package ir.omid.smartteacher.ai.internal.api.dto;

import ir.omid.smartteacher.ai.internal.domain.AiProviderType;
import java.util.UUID;

/**
 * پاسخ Provider برای ادمین.
 *
 * <p><b>🔴 هرگز کلید خام نیست</b> — فقط {@code tokenHint}.
 */
public record AiProviderResponse(
        UUID id,
        String code,
        String name,
        AiProviderType type,
        String baseUrl,
        String tokenHint,
        boolean enabled
) {
}
