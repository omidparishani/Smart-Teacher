package ir.omid.smartteacher.ai.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * افزودن نسخه پیش‌نویس به قالب.
 */
public record CreatePromptVersionRequest(

        @NotBlank(message = "system_prompt الزامی است")
        String systemPrompt,

        @NotBlank(message = "user_prompt الزامی است")
        String userPrompt,

        String variables,

        String responseSchema,

        BigDecimal temperature,

        Integer maxTokens,

        @Size(max = 2000)
        String changeNote
) {
}
