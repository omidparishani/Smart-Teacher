package ir.omid.smartteacher.eitaa;

import ir.omid.smartteacher.eitaa.internal.EitaaAppMessageAdapter;
import ir.omid.smartteacher.eitaa.internal.EitaaHttpClient;
import ir.omid.smartteacher.eitaa.internal.EitaaRateLimiter;
import ir.omid.smartteacher.eitaa.internal.EitaaYarBotAdapter;

/**
 * ساخت پشته <b>واقعی</b> ایتا برای آزمون — بدون Spring و بدون شبیه‌ساز.
 *
 * <p><b>⚠️ چرا این کلاس لازم است؟</b> چون آزمون‌های امنیتی مهم‌ترین
 * لایه‌ها را باید روی <i>کد واقعی</i> بسنجند، نه روی
 * {@code MockEitaaAdapter}. شبیه‌ساز عمداً مهربان است: هرگز {@code ok}
 * را بررسی نمی‌کند، هرگز توکن را در URL نمی‌گذارد، و هرگز خطای شبکه
 * نمی‌دهد. اگر آزمون «آیا {@code ok:false} درست گرفته می‌شود؟» روی
 * شبیه‌ساز نوشته شود، چیزی را می‌سنجد که در Production وجود ندارد.
 *
 * <p>پس اینجا {@link EitaaYarBotAdapter} و {@link EitaaHttpClient} واقعی
 * ساخته می‌شوند و رو به یک سرور محلی می‌گیرند. آن‌وقت هر آزمون می‌تواند
 * پاسخ‌های دقیق بدهد — بدنه خالی، JSON بی‌ربط، {@code ok:false} با کد
 * وضعیت ۲۰۰ — و ببیند کد ما چه می‌کند.
 */
final class EitaaRealStack {

    private EitaaRealStack() {
        throw new AssertionError("کلاس ابزاری نباید نمونه‌سازی شود");
    }

    static EitaaHttpClient httpClient(EitaaProperties properties) {
        return new EitaaHttpClient(properties, new EitaaRateLimiter(), EitaaTestData.mapper());
    }

    static EitaaAppMessageAdapter appAdapter(EitaaProperties properties) {
        return new EitaaAppMessageAdapter(httpClient(properties), properties);
    }

    /**
     * آداپتور واقعی روی یک کلاینت مشترک.
     *
     * <p>کلاینت مشترک عمدی است: {@code EitaaHttpClient} یک
     * ExecutorService می‌سازد و ساختن چند نمونه در یک آزمون، منابع
     * بی‌دلیل باز می‌گذارد.
     */
    static EitaaYarBotAdapter botAdapter(EitaaProperties properties) {
        EitaaHttpClient httpClient = httpClient(properties);
        return new EitaaYarBotAdapter(
                properties,
                httpClient,
                new EitaaAppMessageAdapter(httpClient, properties),
                EitaaTestCredits.noop());
    }
}
