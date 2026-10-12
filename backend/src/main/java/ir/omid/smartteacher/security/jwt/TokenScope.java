package ir.omid.smartteacher.security.jwt;

/**
 * دامنه کاربرد توکن — مطابق {@code chk_refresh_scope} در V4.
 *
 * <p>دو مسیر احراز هویت در {@code docs/security.md} دو سطح ریسک
 * متفاوت دارند و نباید توکن‌شان قابل تعویض باشد: توکن Mini App که در
 * حافظه مرورگر ایتا نگه داشته می‌شود، نباید بتواند به API پنل مدیریت
 * دسترسی بگیرد.
 */
public enum TokenScope {

    /** Mini App ایتا — معلم و دانش‌آموز. */
    MINIAPP,

    /** پنل وب مدیریت — مدیر مدرسه، ادمین و سوپر‌ادمین. */
    ADMIN
}
