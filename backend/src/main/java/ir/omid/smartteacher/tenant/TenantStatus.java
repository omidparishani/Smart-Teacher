package ir.omid.smartteacher.tenant;

/**
 * وضعیت چرخه حیات مستأجر — مطابق {@code chk_tenants_status} در V2.
 */
public enum TenantStatus {

    /** فعال — همه عملیات مجاز است. */
    ACTIVE,

    /**
     * معلق — ورود کاربران و هر عملیات نوشتنی ممنوع است.
     *
     * <p>داده حفظ می‌شود تا تعلیق به‌خاطر بدهی معوق قابل برگشت باشد.
     */
    SUSPENDED,

    /** بایگانی‌شده — فقط خواندنی، در مسیر حذف نهایی. */
    ARCHIVED
}
