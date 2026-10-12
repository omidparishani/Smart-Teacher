package ir.omid.smartteacher.eitaa.internal.api.dto;

/**
 * پاسخ موفق ورود — شکل دقیقاً مطابق {@code docs/api.md} بخش ۵.
 *
 * <p><b>⚠️ {@code refreshExpiresIn} افزوده ماست.</b> نمونه سند فقط
 * {@code expiresIn} دارد. ولی کلاینت Mini App باید بداند تا کی می‌تواند
 * تازه‌سازی کند — وگرنه یا بی‌دلیل تازه‌سازی می‌کند یا دیر می‌فهمد که
 * نشست مرده. نبودنش در سند، دلیل خوبی برای پنهان‌کردن اطلاعاتی نیست که
 * سرور قطعاً دارد. این افزودن در {@code docs/verification-log.md}
 * ثبت شده است.
 *
 * @param accessToken      توکن دسترسی
 * @param refreshToken     توکن تازه‌سازی — خام، فقط همین یک بار
 * @param tokenType        همیشه {@code Bearer}
 * @param expiresIn        عمر توکن دسترسی به ثانیه
 * @param refreshExpiresIn عمر توکن تازه‌سازی به ثانیه
 * @param user             کاربر
 * @param tenant           مستأجر فعال — می‌تواند {@code null} باشد
 * @param subscription     اشتراک — در فاز ۲ همیشه {@code null}
 * @param isNewUser        آیا این ورود، حساب را تازه ساخت؟
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn,
        AuthUserView user,
        AuthTenantView tenant,
        AuthSubscriptionView subscription,
        boolean isNewUser
) {

    /**
     * ⚠️ عمداً بازنویسی شده — همان دلیلی که در {@code TokenPair} آمد.
     * بدون این متد، یک {@code log.debug("{}", response)} ساده هر دو توکن
     * را برای همیشه در فایل Log ثبت می‌کرد و دزدیدن نشست کاربر از روی
     * Log ممکن می‌شد.
     */
    @Override
    public String toString() {
        return "AuthResponse{user=" + ((user == null) ? null : user.id())
                + ", tenant=" + ((tenant == null) ? null : tenant.id())
                + ", isNewUser=" + isNewUser
                + ", accessToken=[REDACTED], refreshToken=[REDACTED]}";
    }
}
