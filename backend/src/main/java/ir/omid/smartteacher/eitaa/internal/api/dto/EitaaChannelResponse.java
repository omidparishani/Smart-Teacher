package ir.omid.smartteacher.eitaa.internal.api.dto;

import java.time.Instant;
import java.util.UUID;

public record EitaaChannelResponse(
        UUID id,
        String eitaaChatId,
        String title,
        String username,
        String status,
        boolean canPost,
        Instant createdAt
) {
}
