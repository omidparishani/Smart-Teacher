package ir.omid.smartteacher.admin.internal.api.dto;

import ir.omid.smartteacher.tenant.TenantStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * پاسخ مدرسه — همان مستأجر با {@code type=SCHOOL}.
 *
 * @param id             شناسه مستأجر/مدرسه
 * @param name           نام
 * @param slug           نامک
 * @param parentTenantId والد
 * @param ownerUserId    مالک
 * @param status         وضعیت
 * @param settings       JSON
 * @param createdAt      زمان ایجاد
 */
public record AdminSchoolResponse(
        UUID id,
        String name,
        String slug,
        UUID parentTenantId,
        UUID ownerUserId,
        TenantStatus status,
        String settings,
        Instant createdAt
) {
}
