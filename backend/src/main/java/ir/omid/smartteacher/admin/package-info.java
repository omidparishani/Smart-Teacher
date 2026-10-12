/**
 * ماژول پنل مدیریت — امکانات پلتفرم‌سطح برای {@code SUPER_ADMIN} و {@code ADMIN}.
 *
 * <p>این ماژول {@code CLOSED} است: فقط بسته ریشه برای بیرون دیده می‌شود.
 * کنترلرها و سرویس‌ها در {@code admin.internal} پنهان‌اند تا ماژول‌های
 * دامنه (کلاس، دانش‌آموز، …) به جزئیات پنل وابسته نشوند.
 *
 * <p><b>فاز ۴:</b> ورود، داشبورد شمارشی، مدیریت کاربران/مستأجرها، و
 * Feature Flags حداقلی.
 *
 * <p><b>فاز ۱۱:</b> نمودار، گزارش، Excel، کش.
 *
 * <p><b>فاز ۱۲:</b> 2FA از طریق {@code security :: totp}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Admin",
        allowedDependencies = {
                "security :: totp",
                "security :: jwt",
                "security :: audit",
                "user",
                "tenant",
                "audit",
                "common"
        }
)
package ir.omid.smartteacher.admin;
