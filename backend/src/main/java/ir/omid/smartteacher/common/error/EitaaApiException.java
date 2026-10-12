package ir.omid.smartteacher.common.error;

/**
 * ایتا پاسخ {@code ok: false} داد یا در دسترس نبود
 * ({@code docs/eitaa.md} بخش ۴.۲).
 *
 * <p><b>⚠️ قاعده طلایی:</b> «اولین پارامتر هر پاسخ JSON، {@code ok}
 * است. همیشه ابتدا {@code ok} را بررسی کنید.» نادیده‌گرفتن این بررسی
 * یعنی یک خطای ایتا بی‌صدا موفق تلقی شود — و معلم فکر کند تکلیف به
 * گروه رسیده، در حالی که نرسیده.
 *
 * <p>پیام این استثنا متن خام ایتا را <b>حمل نمی‌کند</b>. پاسخ ایتا
 * ممکن است جزئیات داخلی داشته باشد، پس آن متن فقط به Log می‌رود و
 * کاربر پیام عمومی و قابل‌فهم می‌بیند.
 */
public class EitaaApiException extends AppException {

    private final transient String remoteErrorCode;
    private final transient String technicalDetail;

    private EitaaApiException(String message,
                              String remoteErrorCode,
                              String technicalDetail,
                              Throwable cause) {
        super(ErrorCode.EITAA_API_ERROR, message, null, cause);
        this.remoteErrorCode = remoteErrorCode;
        this.technicalDetail = technicalDetail;
    }

    /**
     * ساخت از پاسخ ناموفق ایتا — {@code ok: false}.
     *
     * @param remoteErrorCode کد خطای ایتا، اگر در پاسخ بود؛ می‌تواند {@code null} باشد
     * @param remoteMessage   توصیف خطای ایتا — <b>فقط برای Log</b>
     */
    public static EitaaApiException fromResponse(String remoteErrorCode, String remoteMessage) {
        return new EitaaApiException(
                ErrorCode.EITAA_API_ERROR.defaultMessage(),
                remoteErrorCode,
                remoteMessage,
                null);
    }

    /** خطای شبکه یا انقضای زمان انتظار — ایتا حتی پاسخ نداد. */
    public static EitaaApiException unreachable(Throwable cause) {
        return new EitaaApiException(
                "ارتباط با ایتا برقرار نشد. دوباره تلاش کنید.",
                null,
                (cause == null) ? null : cause.getClass().getSimpleName(),
                cause);
    }

    /** کد خطای ایتا — برای حسابرسی؛ می‌تواند {@code null} باشد. */
    public String remoteErrorCode() {
        return remoteErrorCode;
    }

    /** جزئیات فنی — <b>فقط برای Log</b>؛ هرگز به کاربر نمی‌رود. */
    public String technicalDetail() {
        return technicalDetail;
    }
}
