package ir.omid.smartteacher.common.error;

/**
 * عملیاتی خواسته شده که مستأجر جاری مشخص نیست.
 *
 * <p>این استثنا نشانه یک نقص پیکربندی یا امنیتی است، نه خطای کاربر:
 * یعنی جایی داده‌ای درخواست شده که به مستأجر گره خورده، ولی هیچ مستأجری
 * در {@code TenantContext} وجود ندارد. حالت‌های واقعی:
 * <ul>
 *   <li>توکن JWT فاقد claim مربوط به مستأجر است.</li>
 *   <li>مسیری که باید احراز هویت‌شده باشد، به‌اشتباه عمومی مانده است.</li>
 *   <li>یک Job زمان‌بندی‌شده فراموش کرده داخل {@code runAsSystem} اجرا شود.</li>
 * </ul>
 *
 * <p><b>Fail-Closed:</b> به‌جای ادامه‌دادن با مقدار {@code null} — که به
 * کوئری بدون فیلتر مستأجر و نشت داده منجر می‌شود — عملیات همین‌جا متوقف
 * می‌شود.
 *
 * <p>کد پاسخ {@link ErrorCode#UNAUTHENTICATED} است چون از دید کاربر
 * یعنی «نشست شما معتبر نیست»، و چون نمی‌خواهیم ساختار داخلی مستأجرها را
 * افشا کنیم.
 */
public class TenantNotResolvedException extends AppException {

    /**
     * نام عملیاتی که بدون مستأجر فراخوانی شده — فقط برای Log، هرگز در
     * پاسخ API نمی‌آید.
     */
    private final transient String operation;

    public TenantNotResolvedException(String operation) {
        super(ErrorCode.UNAUTHENTICATED, ErrorCode.UNAUTHENTICATED.defaultMessage(), null, null);
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
