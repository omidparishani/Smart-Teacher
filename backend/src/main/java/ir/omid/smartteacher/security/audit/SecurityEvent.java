package ir.omid.smartteacher.security.audit;

import ir.omid.smartteacher.audit.Severity;

/**
 * رویدادهای امنیتی که باید ثبت شوند ({@code docs/security.md} بخش ۹).
 *
 * <p>هر ثابت، درجه پیش‌فرض خود را دارد. تفکیک درجه در همین‌جا انجام
 * می‌شود تا در محل فراخوانی، سلیقه‌ای و ناهمگون تعیین نشود.
 *
 * <p>نام رویداد ({@code action}) با پیشوند {@code SECURITY_} ذخیره
 * می‌شود تا در جدول حسابرسی، رویدادهای امنیتی از عملیات عادی با یک
 * پرس‌وجو قابل تفکیک باشند.
 */
public enum SecurityEvent {

    /**
     * تلاش دسترسی به داده‌ی مستأجر دیگر.
     *
     * <p>CRITICAL چون یا نشانه نفوذ است یا باگ جداسازی — هر دو باید
     * فوراً بررسی شوند.
     */
    IDOR_ATTEMPT(Severity.CRITICAL),

    /**
     * استفاده مجدد از توکن تازه‌سازی مصرف‌شده.
     *
     * <p>CRITICAL چون تقریباً قطعاً یعنی توکن دزدیده شده است.
     */
    TOKEN_REUSE_DETECTED(Severity.CRITICAL),

    /** کل خانواده توکن به‌دنبال تشخیص سرقت باطل شد. */
    TOKEN_FAMILY_REVOKED(Severity.WARNING),

    /** همه توکن‌های یک کاربر باطل شد — خروج از همه دستگاه‌ها یا تغییر رمز. */
    TOKEN_ALL_REVOKED(Severity.WARNING),

    /** داده‌ای که باید رمز باشد، در رمزگشایی رد شد — نشانه دست‌کاری. */
    ENCRYPTION_FAILURE(Severity.CRITICAL),

    /** عملیاتی به مستأجر نیاز داشت اما زمینه مستأجر وجود نداشت. */
    TENANT_RESOLUTION_FAILED(Severity.CRITICAL),

    /** تلاش ورود ناموفق. */
    LOGIN_FAILED(Severity.WARNING),

    /** حساب به‌دلیل تلاش‌های ناموفق پشت‌سرهم قفل شد. */
    ACCOUNT_LOCKED(Severity.WARNING),

    /** احراز هویت ایتا نامعتبر بود — امضای initData نخواند یا منقضی بود. */
    INVALID_INIT_DATA(Severity.WARNING),

    /** کاربری که مجوز لازم را نداشت، عملیاتی را درخواست کرد. */
    ACCESS_DENIED(Severity.WARNING),

    /** عملیات حساس انجام شد — تغییر پلن، بازپرداخت، تغییر نقش. */
    SENSITIVE_ACTION(Severity.INFO);

    private final Severity defaultSeverity;

    SecurityEvent(Severity defaultSeverity) {
        this.defaultSeverity = defaultSeverity;
    }

    public Severity defaultSeverity() {
        return defaultSeverity;
    }

    /**
     * نامی که در ستون {@code action} جدول حسابرسی ثبت می‌شود.
     */
    public String actionName() {
        return "SECURITY_" + name();
    }
}
