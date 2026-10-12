package ir.omid.smartteacher.admin.internal.api.dto;

import java.time.Instant;
import java.util.UUID;

public record SystemSettingResponse(
        UUID id,
        String key,
        String value,
        String valueType,
        String category,
        boolean encrypted,
        String description,
        UUID updatedBy,
        Instant updatedAt
) {
}
