package ir.omid.smartteacher.security;

/**
 * پوشاندن داده‌های شخصی و رازها برای نمایش و Log.
 *
 * <p>هر جا قرار است چیزی از داده حساس وارد Log، پیام خطا یا پاسخ
 * مدیریتی شود، باید از این کلاس بگذرد. هدف این است که پشتیبانی بتواند
 * بگوید «کدام ایمیل» بدون اینکه مقدار کامل در فایل Log بنشیند.
 *
 * <p><b>⚠️ این کلاس جای رمزنگاری را نمی‌گیرد.</b> پوشاندن فقط نمایش را
 * امن می‌کند؛ داده اصلی همچنان باید رمز شود
 * ({@link ir.omid.smartteacher.security.crypto.SecretEncryptor}) یا هش
 * (برای رمز عبور). پوشاندن یک راز، آن را ذخیره‌پذیر نمی‌کند.
 */
public final class SensitiveDataMasker {

    private static final String MASK = "***";

    /** حداقل طول ایمیل معنادار. */
    private static final int MIN_EMAIL_LENGTH = 3;

    /** طول شماره موبایل ایران: {@code 09xxxxxxxxx}. */
    private static final int IRAN_MOBILE_LENGTH = 11;

    /** تعداد رقم آشکار در ابتدا و انتهای موبایل. */
    private static final int MOBILE_VISIBLE_DIGITS = 4;

    /** تعداد کاراکتر آشکار در انتهای یک راز. */
    private static final int SECRET_VISIBLE_CHARS = 4;

    private SensitiveDataMasker() {
        throw new AssertionError("کلاس ابزاری نباید نمونه‌سازی شود");
    }

    /**
     * پوشاندن ایمیل با نگه‌داشتن اولین کاراکتر و دامنه.
     *
     * @param email ایمیل؛ {@code null} یا نامعتبر مجاز است
     * @return مثل {@code a***@example.com}
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 0 || email.length() < MIN_EMAIL_LENGTH) {
            return MASK;
        }
        return email.charAt(0) + MASK + email.substring(at);
    }

    /**
     * پوشاندن شماره موبایل با نگه‌داشتن چهار رقم اول و چهار رقم آخر.
     *
     * <p>چهار رقم آخر نگه داشته می‌شود چون کاربر با همان شماره را
     * می‌شناسد؛ چهار رقم اول نگه داشته می‌شود چون پیش‌شماره اپراتور است
     * و خودش داده شخصی نیست.
     *
     * @param mobile شماره؛ {@code null} مجاز است
     * @return مثل {@code 0912***4567}
     */
    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return "";
        }
        if (mobile.length() != IRAN_MOBILE_LENGTH) {
            // طول غیرمنتظره یعنی قالب را نمی‌شناسیم؛ امن‌تر آن است که
            // کل مقدار پوشانده شود تا حدس نزنیم کدام بخش بی‌خطر است.
            return MASK;
        }
        return mobile.substring(0, MOBILE_VISIBLE_DIGITS)
                + MASK
                + mobile.substring(IRAN_MOBILE_LENGTH - MOBILE_VISIBLE_DIGITS);
    }

    /**
     * پوشاندن یک راز (کلید API، توکن) با نگه‌داشتن چند کاراکتر آخر.
     *
     * <p>منطقش با {@code SecretEncryptor.mask} یکسان است؛ اینجا تکرار
     * شده تا لایه‌های دیگر مجبور نباشند برای یک نمایش ساده به ماژول
     * رمزنگاری وابسته شوند.
     *
     * @param secret مقدار خام؛ {@code null} مجاز است
     * @return مثل {@code ••••••••AB12}
     */
    public static String maskSecret(String secret) {
        if (secret == null || secret.isEmpty()) {
            return "";
        }
        if (secret.length() <= SECRET_VISIBLE_CHARS) {
            return "•".repeat(SECRET_VISIBLE_CHARS + 4);
        }
        return "•".repeat(SECRET_VISIBLE_CHARS + 4)
                + secret.substring(secret.length() - SECRET_VISIBLE_CHARS);
    }
}
