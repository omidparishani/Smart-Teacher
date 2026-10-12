package ir.omid.smartteacher.common.error;

/**
 * ریشه همه استثناهای کاری سیستم.
 *
 * <p><b>قاعده:</b> هر خطایی که کاربر باید پیام مشخصی ببیند، از این کلاس
 * ارث‌بری می‌کند و {@link ErrorCode} مناسب را حمل می‌کند.
 * {@link GlobalExceptionHandler} فقط همین‌ها را با پیام واقعی به کاربر
 * نشان می‌دهد؛ هر استثنای دیگری به {@link ErrorCode#INTERNAL_ERROR}
 * تبدیل می‌شود تا جزئیات پیاده‌سازی بیرون نریزد.
 *
 * <p><b>⚠️ هشدار امنیتی:</b> {@code message} به کاربر نهایی نمایش داده
 * می‌شود. هرگز مقدار کلید API، توکن، رمز عبور، یا جزئیات ساختار داخلی
 * دیتابیس را داخل پیام نگذارید. برای این موارد پیام فارسی عمومی بدهید و
 * جزئیات را فقط Log کنید.
 */
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * داده تکمیلی برای ساخت بدنه خطا (مثلاً اطلاعات سقف مصرف).
     * {@code transient} است چون استثناها {@code Serializable} هستند و
     * این شیء لزوماً نیست.
     */
    private final transient Object details;

    public AppException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), null, null);
    }

    public AppException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    public AppException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, null, cause);
    }

    /**
     * @param errorCode کد خطا که هم وضعیت HTTP و هم پیام پیش‌فرض را تعیین می‌کند
     * @param message   پیام کاربرپسند — نباید داده حساس داشته باشد
     * @param details   داده تکمیلی ساختاریافته برای بدنه پاسخ (می‌تواند {@code null} باشد)
     * @param cause     علت اصلی برای Log (هرگز به کاربر نشان داده نمی‌شود)
     */
    public AppException(ErrorCode errorCode, String message, Object details, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object getDetails() {
        return details;
    }
}
