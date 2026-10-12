package ir.omid.smartteacher.admin.internal.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * پاسخ بازنویسی امکان.
 *
 * @param id          شناسه
 * @param scope       TENANT یا USER
 * @param tenantId    مستأجر؛ فقط برای TENANT
 * @param userId      کاربر؛ فقط برای USER
 * @param featureCode کد امکان
 * @param enabled     فعال بودن
 * @param reason      دلیل
 * @param expiresAt   انقضا
 * @param grantedBy   اعطاکننده
 * @param createdAt   زمان ایجاد
 */
public record FeatureOverrideResponse(
        UUID id,
        String scope,
        UUID tenantId,
        UUID userId,
        String featureCode,
        boolean enabled,
        String reason,
        Instant expiresAt,
        UUID grantedBy,
        Instant createdAt
) {
}
