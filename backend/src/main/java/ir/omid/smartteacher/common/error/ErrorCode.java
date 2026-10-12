package ir.omid.smartteacher.common.error;

import org.springframework.http.HttpStatus;

/**
 * کدهای خطای استاندارد سیستم ({@code docs/api.md} بخش ۳).
 *
 * <p><b>قاعده:</b> کد خطا انگلیسی است تا Frontend بتواند بر اساس آن رفتار
 * متفاوت داشته باشد و Log قابل جست‌وجو بماند؛ پیام فارسی است چون کاربر
 * نهایی آن را می‌بیند. اگر پیام فارسی داخل کد باشد، هر تغییر متن یک
 * تغییر کد می‌شود و تست‌ها به متن وابسته می‌شوند.
 *
 * <p><b>قاعده امنیتی:</b> پیام {@link #INTERNAL_ERROR} هرگز نباید جزئیات
 * استثنا را برگرداند. جزئیات فقط در Log با همان {@code traceId} می‌ماند.
 */
public enum ErrorCode {

    // ─── ۴۰۰ ───
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "داده‌های ورودی نامعتبر است."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "درخواست نامعتبر است."),

    // ─── ۴۰۱ ───
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "برای انجام این عملیات باید وارد شوید."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "نام کاربری یا رمز عبور نادرست است."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "نشست شما منقضی شده است. دوباره وارد شوید."),
    TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "نشست شما به دلایل امنیتی باطل شد."),
    EITAA_INIT_DATA_INVALID(HttpStatus.UNAUTHORIZED, "احراز هویت ایتا نامعتبر است."),

    // ─── ۴۰۳ ───
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "شما به این بخش دسترسی ندارید."),
    FEATURE_NOT_ENTITLED(HttpStatus.FORBIDDEN, "این قابلیت در پلن فعلی شما فعال نیست."),
    SUBSCRIPTION_EXPIRED(HttpStatus.FORBIDDEN, "اشتراک شما منقضی شده است."),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "حساب کاربری شما غیرفعال شده است."),

    // ─── ۴۰۴ ───
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "موردی که خواستید یافت نشد."),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "آدرس درخواستی یافت نشد."),

    // ─── ۴۰۹ ───
    RESOURCE_CONFLICT(HttpStatus.CONFLICT, "این عملیات با وضعیت فعلی سیستم در تناقض است."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "این مورد قبلاً ثبت شده است."),
    COUPON_INVALID(HttpStatus.CONFLICT, "کد تخفیف نامعتبر است."),

    // ─── ۴۲۹ ───
    USAGE_LIMIT_REACHED(HttpStatus.TOO_MANY_REQUESTS, "سقف استفاده از این قابلیت به پایان رسیده است."),
    AI_USAGE_LIMIT_REACHED(HttpStatus.TOO_MANY_REQUESTS, "سقف استفاده از هوش مصنوعی این پلن به پایان رسیده است."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "تعداد درخواست‌های شما بیش از حد مجاز است."),

    // ─── ۵۰۱ ───
    /**
     * قابلیتی که ایتا <b>ندارد</b> درخواست شده.
     *
     * <p>۵۰۱ و نه ۵۰۰: این خطای ما نیست، ایتا آن قابلیت را ارائه
     * نمی‌دهد. کد ۵۰۱ این را از یک خرابی واقعی جدا می‌کند تا در پایش،
     * «درخواست قابلیت ناموجود» با «سرویس خراب است» قاطی نشود.
     */
    EITAA_CAPABILITY_NOT_SUPPORTED(HttpStatus.NOT_IMPLEMENTED, "این قابلیت در ایتا وجود ندارد."),

    // ─── ۵۰۲ / ۵۰۳ ───
    AI_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "سرویس هوش مصنوعی موقتاً در دسترس نیست."),
    AI_STRUCTURED_OUTPUT_FAILED(HttpStatus.BAD_GATEWAY, "پاسخ هوش مصنوعی قابل پردازش نبود."),
    PAYMENT_GATEWAY_ERROR(HttpStatus.BAD_GATEWAY, "خطا در ارتباط با درگاه پرداخت."),
    /** ایتا پاسخ {@code ok: false} داد یا در دسترس نبود. */
    EITAA_API_ERROR(HttpStatus.BAD_GATEWAY, "ارتباط با ایتا با خطا مواجه شد."),

    // ─── ۵۰۰ ───
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "خطای غیرمنتظره در سیستم رخ داد.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public int httpStatus() {
        return status.value();
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
