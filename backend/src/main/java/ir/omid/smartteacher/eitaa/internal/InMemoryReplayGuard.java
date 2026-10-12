package ir.omid.smartteacher.eitaa.internal;

import ir.omid.smartteacher.eitaa.ReplayGuard;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * نگه‌بان ضدبازپخش در حافظه — پیاده‌سازی پیش‌فرض
 * ({@link ir.omid.smartteacher.eitaa.ReplayGuard}) وقتی {@code app.redis.enabled=false}.
 *
 * <p><b>⚠️ محدودیت‌ها — هر سه واقعی‌اند و پنهان نشده‌اند:</b>
 * <ol>
 *   <li><b>تک‌نمونه‌ای.</b> روی چند نمونه، دو درخواست یکسان که به دو
 *       نمونه برسند هر دو پذیرفته می‌شوند. فاز ۱۲ با Redis حلش می‌کند.</li>
 *   <li><b>با راه‌اندازی مجدد پاک می‌شود.</b> پنجره پنج‌دقیقه‌ای
 *       {@code auth_date} همچنان پابرجاست، پس بازپخش فقط در همان پنجره
 *       ممکن است — و بعد از راه‌اندازی مجدد، دوباره ممکن می‌شود.</li>
 *   <li><b>حافظه محدود است.</b> سقف {@link #MAX_ENTRIES} دارد تا رشد
 *       بی‌کران حافظه ممکن نباشد.</li>
 * </ol>
 *
 * <p><b>چرا سقف گذاشته شده؟</b> برای رسیدن به این کلاس باید امضای
 * {@code initData} خوانده باشد، پس مهاجم بدون توکن ربات نمی‌تواند
 * حافظه را پر کند. ولی تکیه بر «مهاجم نمی‌تواند» عادت بدی است: سقف
 * صریح، این مسیر را به‌کل از فهرست راه‌های مصرف حافظه بیرون می‌آورد.
 * با پر شدن سقف، قدیمی‌ترین رکوردها حذف می‌شوند (رفتار FIFO تقریبی).
 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryReplayGuard implements ReplayGuard {

    private static final Logger log = LoggerFactory.getLogger(InMemoryReplayGuard.class);

    /**
     * سقف تعداد اثر انگشت‌های نگه‌داشته‌شده.
     *
     * <p>هر رکورد یک رشته ۶۴ کاراکتری و یک زمان است — چند صد بایت.
     * صد هزار رکورد یعنی چند ده مگابایت، که با پنجره پنج‌دقیقه‌ای
     * بسیار بیشتر از هر بار واقعی است.
     */
    static final int MAX_ENTRIES = 100_000;

    private final Map<String, Instant> usedFingerprints = new ConcurrentHashMap<>();
    private final AtomicInteger purgeCounter = new AtomicInteger();

    @Override
    public boolean markUsed(String fingerprint, Duration window) {
        if (fingerprint == null || fingerprint.isBlank()) {
            // بدون اثر انگشت نمی‌شود بازپخش را تشخیص داد. رد کردن
            // امن‌تر از پذیرفتن است، چون فراخوان در آن حالت باگی دارد.
            log.warn("اثر انگشت initData خالی بود — برای جلوگیری از بازپخش، رد شد");
            return false;
        }

        Instant expiresAt = Instant.now().plus(window);

        // putIfAbsent اتمی است: در یک درخواست هم‌زمان، فقط یکی true می‌گیرد.
        Instant previous = usedFingerprints.putIfAbsent(fingerprint, expiresAt);

        // پاک‌سازی دوره‌ای — نه در هر درخواست، که خودش هزینه شود.
        maybePurge();

        if (previous == null) {
            return true;
        }

        // رکورد قبلی منقضی شده باشد، یعنی بازپخش واقعی نیست ولی ورودی
        // کهنه مانده بود. در آن حالت رکورد را تازه می‌کنیم.
        //
        // ⚠️ شرط «منقضی شده» یک `!isAfter` است و نه `isBefore`. مقدار
        // ذخیره‌شده خودِ لحظه انقضا است، پس در `now == expiresAt`
        // پنجره تمام شده و رکورد کهنه است. با `isBefore` آن یک لحظه
        // «هنوز تازه» حساب می‌شد — خطای یک‌لحظه‌ای که در Production
        // بی‌اثر است ولی در پنجره‌های بسیار کوتاه (و روی ساعتی با دقت
        // کم) قابل مشاهده می‌شود: دو فراخوان پشت‌سرهم در یک تیک ساعت،
        // دومی را به‌غلط بازپخش اعلام می‌کرد.
        if (!previous.isAfter(Instant.now())) {
            usedFingerprints.put(fingerprint, expiresAt);
            return true;
        }

        return false;
    }

    @Override
    public int purgeExpired() {
        Instant now = Instant.now();
        int before = usedFingerprints.size();

        // ⚠️ همان مرز markUsed: رکوردی که لحظه انقضایش رسیده، کهنه است.
        usedFingerprints.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));

        int removed = before - usedFingerprints.size();

        if (usedFingerprints.size() >= MAX_ENTRIES) {
            evictOldestApproximately();
        }

        return removed;
    }

    /** تعداد رکوردهای نگه‌داشته‌شده — برای پایش و تست. */
    public int size() {
        return usedFingerprints.size();
    }

    /**
     * حذف تقریبی قدیمی‌ترین‌ها وقتی سقف پر شده.
     *
     * <p>«تقریبی» عمدی است: {@code ConcurrentHashMap} ترتیب ندارد و
     * مرتب‌کردن کامل برای یک مسیر پاک‌سازی، هزینه‌ای بی‌تناسب است.
     * هدف فقط جلوگیری از رشد بی‌کران است، نه حفظ دقیق قدیمی‌ترین‌ها.
     */
    private void evictOldestApproximately() {
        int excess = usedFingerprints.size() - (MAX_ENTRIES * 3 / 4);
        if (excess <= 0) {
            return;
        }

        log.warn("حافظه نگه‌بان ضدبازپخش پر شد ({} رکورد) — {} رکورد قدیمی حذف می‌شود",
                usedFingerprints.size(), excess);

        var iterator = usedFingerprints.entrySet().iterator();
        for (int removed = 0; removed < excess && iterator.hasNext(); removed++) {
            iterator.next();
            iterator.remove();
        }
    }

    private void maybePurge() {
        // هر ۱۰۰۰ ثبت، یک بار پاک‌سازی — تا هزینه در هر درخواست پخش شود.
        if (purgeCounter.incrementAndGet() % 1000 == 0) {
            purgeExpired();
        }
    }
}
