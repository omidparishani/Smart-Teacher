package ir.omid.smartteacher.eitaa.internal;

import ir.omid.smartteacher.common.util.Hashing;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * محدودکننده نرخ ارسال به ایتا — «قوطی توکن» با نرخ ثابت.
 *
 * <p><b>چرا لازم است؟</b> سند رسمی ایتایار سقف نرخ را اعلام نکرده، ولی
 * سرویس اعتبارسنجی‌اش را «حداکثر ۲۰ درخواست در ثانیه — سپس IP مسدود
 * می‌شود» توصیف می‌کند. مسدودشدن IP یعنی <b>همه</b> کاربران ما از کار
 * می‌افتند. پس حتی بدون دانستن سقف دقیق، باید خودمان را محدود کنیم.
 *
 * <p>هر توکن قوطی جدا دارد، چون سند می‌گوید سقف «مشترک برای همه
 * درخواست‌های خروجی <i>هر توکن</i>» است — نه یک سقف سراسری.
 *
 * <p><b>⚠️ محدودیت نسخه فعلی:</b> قوطی‌ها در حافظه‌اند، پس روی چند
 * نمونه، سقف واقعی به‌اندازه تعداد نمونه‌ها ضرب می‌شود. انتقال به Redis
 * در فاز ۱۲ انجام می‌شود.
 *
 * <p><b>⚠️ چرا کلید از هش توکن ساخته می‌شود؟</b> اگر خود توکن کلید Map
 * می‌شد، یک راز در حافظه می‌ماند که هر heap dump یا گزارش خطا
 * می‌تواند بیرونش بکشد. هش، همان تفکیک را می‌دهد بدون نگه‌داشتن راز.
 */
@Component
public class EitaaRateLimiter {

    private final Map<String, AtomicLong> nextFreeSlotNanos = new ConcurrentHashMap<>();

    /**
     * گرفتن یک نوبت ارسال، با انتظار در صورت نیاز.
     *
     * <p>روی Virtual Thread صدا زده می‌شود، پس انتظار، نخ سیستم‌عامل را
     * اشغال نمی‌کند. با این حال انتظار بی‌پایان ندارد: اگر نوبت بیش از
     * {@code maxWait} عقب باشد، {@code false} برمی‌گردد تا فراخوان
     * تصمیم بگیرد — نه اینکه درخواست کاربر بی‌نهایت معلق بماند.
     *
     * @param rateLimitKey    شناسه توکن — <b>خودِ توکن نیست</b>، هش آن
     * @param permitsPerSecond سقف مجاز در ثانیه
     * @param maxWait         بیشترین انتظار قابل‌قبول
     * @return {@code true} اگر نوبت گرفته شد
     */
    public boolean acquire(String rateLimitKey, int permitsPerSecond, Duration maxWait) {
        if (permitsPerSecond <= 0) {
            return true;
        }

        long intervalNanos = 1_000_000_000L / permitsPerSecond;
        AtomicLong slot = nextFreeSlotNanos.computeIfAbsent(
                rateLimitKey, key -> new AtomicLong(System.nanoTime()));

        // CAS تکراری: چند درخواست هم‌زمان باید نوبت‌های پشت‌سرهم بگیرند،
        // نه یک نوبت مشترک. اگر فقط get/set می‌کردیم، دو درخواست
        // هم‌زمان می‌توانستند همان شکاف را بگیرند.
        long mySlot;
        while (true) {
            long current = slot.get();
            long now = System.nanoTime();
            mySlot = Math.max(now, current) + intervalNanos;

            if (mySlot - now > maxWait.toNanos()) {
                return false;
            }
            if (slot.compareAndSet(current, mySlot)) {
                break;
            }
        }

        long waitNanos = mySlot - intervalNanos - System.nanoTime();
        if (waitNanos > 0) {
            try {
                Thread.sleep(waitNanos / 1_000_000L, (int) (waitNanos % 1_000_000L));
            } catch (InterruptedException e) {
                // بازگرداندن وضعیت وقفه — قاعده استاندارد Java.
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return true;
    }

    /**
     * کلید محدودیت از یک توکن.
     *
     * <p>هش SHA-256 است، نه خود توکن — تا راز در حافظه به‌عنوان کلید
     * Map نماند.
     */
    public static String keyFor(String tokenValue) {
        return (tokenValue == null) ? "anonymous" : Hashing.sha256Hex(tokenValue);
    }

    /** تعداد قوطی‌های فعال — برای پایش و تست. */
    public int bucketCount() {
        return nextFreeSlotNanos.size();
    }
}
