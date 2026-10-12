package ir.omid.smartteacher.ai.internal.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * زمینه رزولوشن Provider/Prompt — شناسه‌های دامنه برای شش سطح.
 *
 * @param tenantId    مستأجر جاری
 * @param userId      کاربر
 * @param classId     کلاس
 * @param planId      پلن
 * @param featureCode کد فیچر (برای فیلتر قالب/تخصیص FEATURE)
 */
public record ResolutionContext(
        UUID tenantId,
        UUID userId,
        UUID classId,
        UUID planId,
        String featureCode
) {

    public Optional<UUID> scopeIdFor(AiScopeType scope) {
        return switch (scope) {
            case USER -> Optional.ofNullable(userId);
            case CLASS -> Optional.ofNullable(classId);
            case TENANT -> Optional.ofNullable(tenantId);
            case PLAN -> Optional.ofNullable(planId);
            case FEATURE, GLOBAL -> Optional.empty();
        };
    }

    public String cacheKey() {
        return String.join(":",
                nullSafe(tenantId),
                nullSafe(userId),
                nullSafe(classId),
                nullSafe(planId),
                featureCode == null ? "" : featureCode);
    }

    private static String nullSafe(UUID id) {
        return id == null ? "-" : id.toString();
    }
}
