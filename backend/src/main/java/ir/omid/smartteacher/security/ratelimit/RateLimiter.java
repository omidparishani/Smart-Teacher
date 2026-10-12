package ir.omid.smartteacher.security.ratelimit;

/**
 * محدودیت نرخ درخواست — قرارداد مشترک برای پیاده‌سازی حافظه‌ای (گام ۱)
 * و Redis (فاز ۱۲ کامل).
 */
public interface RateLimiter {

    /**
     * یک واحد از سهمیهٔ کلید را مصرف می‌کند.
     *
     * @param key شناسهٔ سطل (مثلاً {@code login:ip|username})
     * @return {@code true} اگر مجاز باشد؛ {@code false} اگر سقف پر شده
     */
    boolean tryAcquire(String key);
}
