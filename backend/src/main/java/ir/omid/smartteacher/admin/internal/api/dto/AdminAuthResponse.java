package ir.omid.smartteacher.admin.internal.api.dto;

import java.util.Set;
import java.util.UUID;

/**
 * پاسخ موفق ورود ادمین — هم‌شکل نمونه احراز هویت در {@code docs/api.md}.
 *
 * <p>مستأجر و اشتراک در ورود پلتفرم‌سطح {@code null} می‌مانند؛ ادمین
 * پلتفرم به یک مستأجر گره نخورده است.
 *
 * @param accessToken      توکن دسترسی با دامنه {@code ADMIN}
 * @param refreshToken     توکن تازه‌سازی — خام، فقط همین یک بار
 * @param tokenType        همیشه {@code Bearer}
 * @param expiresIn        عمر توکن دسترسی به ثانیه
 * @param refreshExpiresIn عمر توکن تازه‌سازی به ثانیه
 * @param user             کاربر ادمین
 */
public record AdminAuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn,
        AdminUserView user
) {

    @Override
    public String toString() {
        return "AdminAuthResponse{user=" + ((user == null) ? null : user.id())
                + ", accessToken=[REDACTED], refreshToken=[REDACTED]}";
    }

    /**
     * نمای کاربر در پاسخ ورود ادمین.
     *
     * @param id       شناسه
     * @param fullName نام کامل
     * @param username نام کاربری
     * @param roles    نقش‌های پلتفرمی ({@code SUPER_ADMIN}، {@code ADMIN})
     */
    public record AdminUserView(UUID id, String fullName, String username, Set<String> roles) {
    }
}
