package ir.omid.smartteacher.eitaa.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/** ساخت گروه ایتا. */
public record CreateEitaaGroupRequest(
        @NotBlank String eitaaChatId,
        String title,
        String type
) {}
