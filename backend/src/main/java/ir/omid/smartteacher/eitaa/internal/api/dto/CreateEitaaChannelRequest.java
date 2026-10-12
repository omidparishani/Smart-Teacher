package ir.omid.smartteacher.eitaa.internal.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateEitaaChannelRequest(
        @NotBlank String eitaaChatId,
        String title,
        String username
) {
}
