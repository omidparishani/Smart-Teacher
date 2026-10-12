package ir.omid.smartteacher.common.error;

/**
 * توکن ارائه‌شده معتبر نیست.
 *
 * <p>شامل هر خرابی‌ای می‌شود که به «توکن» مربوط است اما «استفاده مجدد»
 * نیست: امضای نادرست، قالب خراب، صادرکننده نادرست، مخاطب نادرست، توکن
 * باطل‌شده، و توکن منقضی.
 *
 * <p><b>⚠️ چرا حالت‌ها تفکیک نمی‌شوند؟</b> اگر پاسخ «امضا نامعتبر است»
 * با «منقضی شده است» فرق کند، مهاجم می‌فهمد توکنی که ساخته به مرحله
 * بررسی امضا رسیده یا نه — یعنی یک اوراکل برای جعل توکن می‌سازیم. پیام
 * کاربر یکسان است؛ تفکیک فقط در Log می‌ماند.
 */
public class InvalidTokenException extends AppException {

    /** دلیل فنی — فقط برای Log و تست. */
    private final transient String internalReason;

    private InvalidTokenException(ErrorCode errorCode, String internalReason, Throwable cause) {
        super(errorCode, errorCode.defaultMessage(), null, cause);
        this.internalReason = internalReason;
    }

    /** امضا، صادرکننده، مخاطب یا قالب توکن پذیرفته نشد. */
    public static InvalidTokenException malformed(String internalReason) {
        return new InvalidTokenException(ErrorCode.UNAUTHENTICATED, internalReason, null);
    }

    /** توکن از نظر زمانی منقضی شده است. */
    public static InvalidTokenException expired() {
        return new InvalidTokenException(ErrorCode.TOKEN_EXPIRED, "توکن منقضی شده است", null);
    }

    /** توکن در دیتابیس باطل شده است (خروج، تغییر رمز، یا ابطال خانواده). */
    public static InvalidTokenException revoked(String internalReason) {
        return new InvalidTokenException(ErrorCode.UNAUTHENTICATED, internalReason, null);
    }

    public String getInternalReason() {
        return internalReason;
    }
}
