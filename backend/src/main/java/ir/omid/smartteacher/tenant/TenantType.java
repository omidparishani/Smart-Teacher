package ir.omid.smartteacher.tenant;

/**
 * نوع مستأجر ({@code docs/multi-tenancy.md}).
 *
 * <p>این تفکیک تعیین می‌کند زیرساخت چه چیزی را باید پشتیبانی کند — نه
 * اینکه کدام ویژگی‌ها فعال باشند. فعال‌بودن ویژگی از پلن و Feature
 * می‌آید، نه از نوع مستأجر.
 */
public enum TenantType {

    /** معلم مستقل — یک نفر، بدون زیرمجموعه. */
    TEACHER,

    /** مدرسه — چند معلم زیر یک مدیر مدرسه. */
    SCHOOL,

    /** سازمان — ساختار سلسله‌مراتبی با {@code parent_tenant_id}. */
    ORGANIZATION
}
