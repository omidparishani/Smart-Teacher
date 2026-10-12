package ir.omid.smartteacher.eitaa.internal.api.dto;

import java.time.Instant;
import java.util.UUID;

/** پاسخ پیام زمان‌بندی‌شده. */
public record ScheduledMessageResponse(
        UUID id,
        String targetType,
        UUID targetId,
        String contentText,
        Instant scheduledAt,
        String status,
        String eitaaMessageId
) {}
