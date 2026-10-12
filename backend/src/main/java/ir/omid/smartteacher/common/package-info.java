/**
 * ماژول مشترک — ابزارها، موجودیت‌های پایه، مدل خطا.
 *
 * <p>این ماژول {@link org.springframework.modulith.ApplicationModule.Type#OPEN} است:
 * همه پکیج‌هایش برای سایر ماژول‌ها قابل دسترسی‌اند. دلیل: ابزارهای مشترک
 * (تولید UUID، هش، مدل خطا) ذاتاً «عمومی» هستند و محدودکردن دسترسی به آن‌ها
 * فقط نویز پیکربندی تولید می‌کند، نه مرز معماری.
 *
 * <p>ماژول‌های دامنه‌ای برعکس این‌ها {@code CLOSED} هستند و فقط از ریشه
 * پکیج خود چیزی بیرون می‌دهند.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        displayName = "Common"
)
package ir.omid.smartteacher.common;
