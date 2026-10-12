package ir.omid.smartteacher.eitaa.internal.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ChannelPublishQueueResponse(
        UUID id,
        UUID channelId,
        String topic,
        String contentText,
        String status,
        Instant createdAt
) {
}
