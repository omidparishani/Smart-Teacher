package ir.omid.smartteacher.admin.internal.api.dto;

import ir.omid.smartteacher.user.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ردیف فهرست/جزئیات کاربران ادمین — بدون هش رمز و فیلدهای حساس نشست.
 *
 * @param id             شناسه
 * @param username       نام کاربری؛ می‌تواند {@code null} باشد
 * @param fullName       نام کامل
 * @param email          ایمیل؛ می‌تواند {@code null} باشد
 * @param mobile         موبایل؛ می‌تواند {@code null} باشد
 * @param status         وضعیت حساب
 * @param platformRoles  نقش‌های پلتفرمی ({@code user_roles.tenant_id IS NULL})
 * @param lastLoginAt    آخرین ورود موفق؛ می‌تواند {@code null} باشد
 * @param createdAt      زمان ایجاد
 */
public record AdminUserResponse(
        UUID id,
        String username,
        String fullName,
        String email,
        String mobile,
        UserStatus status,
        List<String> platformRoles,
        Instant lastLoginAt,
        Instant createdAt
) {
}
