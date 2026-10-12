package ir.omid.smartteacher.common.error;

import java.time.Instant;

/**
 * {@code auth_date} قدیمی‌تر از پنجره مجاز است
 * ({@code docs/eitaa.md} بخش ۷.۴).
 *
 * <p>پنج دقیقه انتخاب شده، نه بیشتر: {@code initData} عملاً یک
 * «بلیت ورود» است و اگر دیرتر از این استفاده شود، یعنی جایی ذخیره و
 * بعداً بازپخش شده — همان چیزی که این بررسی جلویش را می‌گیرد.
 *
 * <p>پیام این استثنا با بقیه متفاوت است و عمداً: رسیدن به این نقطه
 * <b>نیازمند گذشتن از بررسی HMAC</b> است، پس کسی که این پیام را
 * می‌بیند از قبل امضای معتبر داشته — و بدون توکن ربات نمی‌شود امضای
 * معتبر ساخت. پس این پیام به مهاجم چیزی نمی‌گوید و به کاربر می‌گوید
 * چه کند: برنامک را دوباره باز کند.
 */
public class EitaaInitDataExpiredException extends EitaaInitDataException {

    private final transient Instant issuedAt;

    public EitaaInitDataExpiredException(Instant issuedAt) {
        super("نشست ایتا منقضی شده است. برنامک را دوباره باز کنید.");
        this.issuedAt = issuedAt;
    }

    /** زمانی که {@code initData} صادر شده بود — برای حسابرسی. */
    public Instant issuedAt() {
        return issuedAt;
    }
}
