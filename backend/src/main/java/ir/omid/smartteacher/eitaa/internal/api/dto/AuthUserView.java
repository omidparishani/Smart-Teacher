package ir.omid.smartteacher.eitaa.internal.api.dto;

import java.util.Set;
import java.util.UUID;

/**
 * نمای کاربر در پاسخ‌های احراز هویت.
 *
 * <p>شکل آن دقیقاً مطابق نمونه {@code docs/api.md} بخش ۵ است:
 * {@code { "id", "fullName", "roles" }}.
 *
 * @param id       شناسه کاربر
 * @param fullName نام کامل — همان چیزی که در ایتا ثبت شده
 * @param roles    نقش‌های پلتفرمی
 */
public record AuthUserView(UUID id, String fullName, Set<String> roles) {
}
