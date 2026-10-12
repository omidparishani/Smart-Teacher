package ir.omid.smartteacher.common.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * تولیدکننده UUID نسخه ۷ (زمان‌مرتب) بر پایه RFC 9562.
 *
 * <p><b>چرا در Java و نه در PostgreSQL؟</b> PostgreSQL 17 تابع {@code uuidv7()}
 * ندارد (در نسخه ۱۸ افزوده شد). تولید در اپلیکیشن کنترل کامل می‌دهد و
 * به نسخه دیتابیس گره نمی‌زند.
 *
 * <p><b>چرا نسخه ۷ و نه ۴؟</b> دو دلیل:
 * <ol>
 *   <li><b>کارایی ایندکس:</b> پیشوند زمانی باعث درج ترتیبی در درخت B-tree
 *       می‌شود و صفحه‌آرایی تصادفیِ UUIDv4 را از بین می‌برد.</li>
 *   <li><b>ترتیب زمانی:</b> بدون ستون اضافه، ترتیب ایجاد رکوردها از خود شناسه
 *       قابل استخراج است.</li>
 * </ol>
 *
 * <p><b>هشدار امنیتی:</b> بخش تصادفی با {@link SecureRandom} تولید می‌شود، نه
 * {@code Random}. اگر شناسه قابل پیش‌بینی باشد، مهاجم می‌تواند شناسه‌های
 * آینده را حدس بزند — دقیقاً همان چیزی که کنترل ضد-IDOR می‌خواهد ببندد.
 *
 * <p>چیدمان بیت‌ها:
 * <pre>
 *   bits  0..47  →  unix_ts_ms   (۴۸ بیت زمان)
 *   bits 48..51  →  version = 7
 *   bits 52..63  →  rand_a       (۱۲ بیت تصادفی)
 *   bits 64..65  →  variant = 10
 *   bits 66..127 →  rand_b       (۶۲ بیت تصادفی)
 * </pre>
 */
public final class UuidV7Generator {

    /** ۴۸ بیت زمان — بیشترین مقداری که در بخش timestamp جا می‌شود. */
    private static final long TIMESTAMP_MASK = 0xFFFF_FFFF_FFFFL;

    /** نگه‌داشتن ۶۲ بیت کم‌ارزش برای rand_b. */
    private static final long RAND_B_MASK = 0x3FFF_FFFF_FFFF_FFFFL;

    /** بیت‌های variant برابر {@code 10} — استاندارد UUID. */
    private static final long VARIANT_BITS = 0x8000_0000_0000_0000L;

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7Generator() {
        throw new AssertionError("کلاس ابزاری نباید نمونه‌سازی شود");
    }

    /**
     * تولید شناسه با زمان جاری سیستم.
     *
     * @return یک UUID نسخه ۷
     */
    public static UUID generate() {
        return generate(System.currentTimeMillis());
    }

    /**
     * تولید شناسه با زمان صریح — برای تست‌پذیری و بازتولید سناریوها.
     *
     * @param epochMilli میلی‌ثانیه از مبدأ یونیکس
     * @return یک UUID نسخه ۷
     */
    public static UUID generate(long epochMilli) {
        if (epochMilli < 0) {
            throw new IllegalArgumentException("زمان نمی‌تواند پیش از مبدأ یونیکس باشد");
        }

        long mostSignificant =
                ((epochMilli & TIMESTAMP_MASK) << 16)   // زمان در ۴۸ بیت بالا
                        | 0x7000L                        // نسخه ۷ در بیت‌های ۴۸..۵۱
                        | (RANDOM.nextLong() & 0x0FFFL); // rand_a — ۱۲ بیت

        long leastSignificant =
                (RANDOM.nextLong() & RAND_B_MASK) | VARIANT_BITS;

        return new UUID(mostSignificant, leastSignificant);
    }

    /**
     * استخراج زمان نهفته در یک UUID نسخه ۷.
     *
     * @param uuid شناسه
     * @return میلی‌ثانیه از مبدأ یونیکس
     * @throws IllegalArgumentException اگر UUID نسخه ۷ نباشد
     */
    public static long extractTimestamp(UUID uuid) {
        if (!isVersion7(uuid)) {
            throw new IllegalArgumentException("UUID نسخه ۷ نیست: " + uuid);
        }
        return uuid.getMostSignificantBits() >>> 16;
    }

    /**
     * بررسی نسخه ۷ بودن شناسه.
     *
     * @param uuid شناسه (می‌تواند {@code null} باشد)
     * @return {@code true} اگر نسخه ۷ باشد
     */
    public static boolean isVersion7(UUID uuid) {
        return uuid != null && uuid.version() == 7;
    }
}
