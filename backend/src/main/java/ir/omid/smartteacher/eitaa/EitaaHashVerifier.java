package ir.omid.smartteacher.eitaa;

/**
 * اعتبارسنجی امضای {@code initData} ({@code docs/eitaa.md} بخش ۷.۳).
 *
 * <p><b>چرا اینترفیس است؟</b> چون ایتا یک سرویس اعتبارسنجی از راه دور هم
 * دارد ({@code POST /api/app/verify}) که <b>استفاده نمی‌کنیم</b> — ولی
 * مشتری سازمانی ممکن است آن را قراردادی بخواهد. اینترفیس، افزودن آن را
 * در آینده به یک پیاده‌سازی تازه تبدیل می‌کند، نه تغییر در جریان ورود.
 *
 * <p><b>⚠️ چرا از سرویس از راه دور استفاده نمی‌کنیم؟</b>
 * ({@code docs/eitaa.md} بخش ۷.۳)
 * <ul>
 *   <li>HMAC محلی <i>دقیقاً همان تضمین امنیتی</i> را می‌دهد.</li>
 *   <li>آن سرویس یک‌بارمصرف است و فقط برای {@code initData} کمتر از یک
 *       دقیقه کار می‌کند؛ یعنی تازه‌سازی توکن غیرممکن می‌شود.</li>
 *   <li>سقف ۲۰ درخواست در ثانیه دارد و بعد از آن <b>IP ما مسدود
 *       می‌شود</b>. بیست معلم هم‌زمان پشت یک NAT شرکتی کافی است تا کل
 *       سرویس از کار بیفتد.</li>
 * </ul>
 */
public interface EitaaHashVerifier {

    /**
     * بررسی درستی امضای {@code initData}.
     *
     * @param dataCheckString رشته‌ای که باید امضا شده باشد — بدون {@code hash}،
     *                        مرتب‌شده به ترتیب الفبا و متصل با {@code \n}
     * @param receivedHash    مقدار {@code hash} که ایتا فرستاده
     * @return {@code true} اگر امضا معتبر باشد
     */
    boolean verify(String dataCheckString, String receivedHash);
}
