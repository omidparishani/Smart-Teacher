package ir.omid.smartteacher.common.error;

/**
 * منابع تکراری — مثلاً کلاسی با همین نام در همین سال تحصیلی، یا درسی
 * با همین کد در همین مستأجر.
 *
 * <p>پاسخ {@code 409} است و نه {@code 400}: درخواست <i>از نظر شکل</i>
 * بی‌عیب است؛ تعارض با وضعیت فعلی دیتابیس دارد. تفاوت این دو برای
 * کلاینت مهم است: {@code 400} یعنی «درخواستت را اصلاح کن»، {@code 409}
 * یعنی «این مورد از قبل هست — شاید بخواهی همان را ویرایش کنی».
 *
 * <p><b>⚠️ پیام این استثنا نام فیلد تکراری را می‌گوید ولی مقدارش را
 * نه.</b> دلیل همان قاعده {@code GlobalExceptionHandler} است: مقدار
 * ردشده در مرورگر و Log پروکسی‌ها ثبت می‌شود. برای نام کلاس بی‌خطر
 * است، ولی همین کلاس برای کد ملی دانش‌آموز هم استفاده می‌شود و آنجا
 * نباید در چند جا باقی بماند. یکدست‌بودن، جلوی اشتباه بعدی را می‌گیرد.
 */
public class DuplicateResourceException extends AppException {

    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }

    /**
     * پیام آماده برای تعارض بر اساس یک مقدار مشخص.
     *
     * @param resourceName نام فارسی منبع، مثل «کلاس» یا «درس»
     * @param fieldName    نام فارسی میدانی که تکراری است، مثل «نام»
     */
    public static DuplicateResourceException of(String resourceName, String fieldName) {
        return new DuplicateResourceException(
                "«" + resourceName + "» با این " + fieldName + " از قبل ثبت شده است.");
    }
}
