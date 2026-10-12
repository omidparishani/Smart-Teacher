package ir.omid.smartteacher.admin.internal.api.dto;

/**
 * داشبورد پایه ادمین — فقط شمارنده‌هایی که جدول و منطقشان الان وجود دارد.
 *
 * <p><b>⚠️ عمداً ناقص نسبت به نمونه کامل {@code docs/admin-panel.md}:</b>
 * اشتراک، درآمد، AI، سلامت سیستم و ایتا هنوز پیاده نشده‌اند. برگرداندن
 * صفر برای آن‌ها یعنی «پیاده‌شده» علامت‌خوردن یک Endpoint توخالی — بدتر
 * از باز بودنش. وقتی هر بخش ساخته شد، فیلدش اینجا اضافه می‌شود.
 *
 * @param users       شمارنده‌های کاربران پلتفرم
 * @param tenants     شمارنده مستأجرها
 * @param education   شمارنده‌های دامنه آموزش موجود
 */
public record AdminDashboardResponse(
        UserStats users,
        TenantStats tenants,
        EducationStats education
) {

    /**
     * @param total  همه کاربران حذف‌نشده
     * @param active کاربران با وضعیت {@code ACTIVE}
     */
    public record UserStats(long total, long active) {
    }

    /** @param total همه مستأجرهای حذف‌نشده */
    public record TenantStats(long total) {
    }

    /**
     * @param classrooms تعداد کلاس‌های حذف‌نشده در همه مستأجرها
     * @param students   تعداد دانش‌آموزان حذف‌نشده در همه مستأجرها
     */
    public record EducationStats(long classrooms, long students) {
    }
}
