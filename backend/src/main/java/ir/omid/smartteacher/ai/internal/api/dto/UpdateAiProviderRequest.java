package ir.omid.smartteacher.ai.internal.api.dto;

import ir.omid.smartteacher.ai.internal.domain.AiProviderType;
import jakarta.validation.constraints.Size;

public record UpdateAiProviderRequest(
        @Size(max = 120, message = "نام Provider نباید بیشتر از ۱۲۰ کاراکتر باشد")
        String name,
        AiProviderType type,
        @Size(max = 250, message = "آدرس Base URL نباید بیشتر از ۲۵۰ کاراکتر باشد")
        String baseUrl,
        String apiKey,
        Boolean enabled
) {
}
