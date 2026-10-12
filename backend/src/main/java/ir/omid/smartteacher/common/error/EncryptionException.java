package ir.omid.smartteacher.common.error;

/**
 * عملیات رمزنگاری شکست خورده است.
 *
 * <p>سه علت ممکن، و هر سه از دید کاربر یکسان پاسخ داده می‌شوند:
 * <ul>
 *   <li><b>خرابی داده (Tamper):</b> برچسب احراز اصالت GCM با متن رمز
 *       نمی‌خواند. یعنی کسی بایت‌ها را دست‌کاری کرده یا کلید عوض شده.</li>
 *   <li><b>کلید نامعتبر:</b> کلید اصلی محیط اجرا طول یا قالب درست را
 *       ندارد.</li>
 *   <li><b>خطای پیاده‌سازی:</b> مثل نبود الگوریتم در JVM.</li>
 * </ul>
 *
 * <p><b>⚠️ چرا پیام عمومی؟</b> تفکیک این سه حالت به مهاجم می‌گوید آیا
 * دست‌کاری او مؤثر بوده یا کلید را اشتباه حدس زده — یک خطای رمزنگاری
 * نباید به اوراکل تبدیل شود. علت واقعی در {@code cause} می‌ماند و فقط
 * Log می‌شود.
 */
public class EncryptionException extends AppException {

    /** توضیح فنی فقط برای Log. */
    private final transient String internalReason;

    private EncryptionException(String internalReason, Throwable cause) {
        super(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), null, cause);
        this.internalReason = internalReason;
    }

    /**
     * رمزگشایی ناموفق — معمولاً به دلیل دست‌کاری داده یا کلید نادرست.
     *
     * @param cause علت فنی
     */
    public static EncryptionException decryptionFailed(Throwable cause) {
        return new EncryptionException("رمزگشایی ناموفق بود", cause);
    }

    /** کلید اصلی نامعتبر است. */
    public static EncryptionException invalidKey(String internalReason) {
        return new EncryptionException(internalReason, null);
    }

    /** خطای فنی در فرآیند رمزنگاری. */
    public static EncryptionException failed(String internalReason, Throwable cause) {
        return new EncryptionException(internalReason, cause);
    }

    public String getInternalReason() {
        return internalReason;
    }
}
