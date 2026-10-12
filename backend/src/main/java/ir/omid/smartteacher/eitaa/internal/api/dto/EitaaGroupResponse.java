package ir.omid.smartteacher.eitaa.internal.api.dto;

import java.time.Instant;
import java.util.UUID;

/** پاسخ گروه ایتا. */
public record EitaaGroupResponse(
        UUID id,
        String eitaaChatId,
        String title,
        String type,
        UUID classroomId,
        String status,
        boolean canSendMessages,
        Instant createdAt
) {}
