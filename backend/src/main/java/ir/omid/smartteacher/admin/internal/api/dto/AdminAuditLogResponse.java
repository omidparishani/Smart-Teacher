package ir.omid.smartteacher.admin.internal.api.dto;

import ir.omid.smartteacher.audit.ActorType;
import ir.omid.smartteacher.audit.Severity;
import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogResponse(
        UUID id,
        UUID tenantId,
        UUID actorUserId,
        ActorType actorType,
        String action,
        String entityType,
        UUID entityId,
        String metadata,
        String ipAddress,
        String traceId,
        Severity severity,
        Instant createdAt
) {
}
