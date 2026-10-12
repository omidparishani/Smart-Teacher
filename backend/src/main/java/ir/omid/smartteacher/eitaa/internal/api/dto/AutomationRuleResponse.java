package ir.omid.smartteacher.eitaa.internal.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AutomationRuleResponse(
        UUID id,
        String name,
        String triggerType,
        String triggerConfig,
        String actionType,
        String actionConfig,
        boolean enabled,
        int executionCount,
        Instant lastExecutedAt,
        Instant createdAt
) {
}
