package ir.omid.smartteacher.user;

/** وضعیت حساب کاربری — مطابق {@code chk_users_status} در V2. */
public enum UserStatus {

    /** فعال — می‌تواند وارد شود. */
    ACTIVE,

    /** غیرفعال‌شده توسط مدیر — ورود ممنوع. */
    DISABLED,

    /** معلق موقت، مثلاً به‌خاطر تخلف. */
    SUSPENDED,

    /** ثبت‌نام ناتمام — هنوز فرآیند فعال‌سازی تمام نشده. */
    PENDING
}
