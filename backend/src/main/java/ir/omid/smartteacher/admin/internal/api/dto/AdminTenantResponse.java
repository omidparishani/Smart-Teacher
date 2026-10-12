package ir.omid.smartteacher.admin.internal.api.dto;

import ir.omid.smartteacher.tenant.TenantStatus;
import ir.omid.smartteacher.tenant.TenantType;
import java.time.Instant;
import java.util.UUID;

/**
 * پاسخ مستأجر برای پنل ادمین.
 *
 * @param id             شناسه
 * @param type           نوع مستأجر
 * @param name           نام نمایشی
 * @param slug           نامک یکتا
 * @param parentTenantId والد؛ می‌تواند {@code null} باشد
 * @param ownerUserId    مالک
 * @param status         وضعیت
 * @param settings       JSON تنظیمات (رشته خام)
 * @param createdAt      زمان ایجاد
 */
public record AdminTenantResponse(
        UUID id,
        TenantType type,
        String name,
        String slug,
        UUID parentTenantId,
        UUID ownerUserId,
        TenantStatus status,
        String settings,
        Instant createdAt
) {
}
