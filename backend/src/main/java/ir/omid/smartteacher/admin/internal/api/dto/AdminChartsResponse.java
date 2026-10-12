package ir.omid.smartteacher.admin.internal.api.dto;

import java.util.List;

/**
 * سری‌های روزانه داشبورد ادمین — فقط دامنه‌هایی که جدول دارند.
 *
 * @param period        دورهٔ درخواستی (مثل {@code 30d})
 * @param newUsers      کاربران جدید روزانه (حذف‌نشده)
 * @param newTenants    مستأجرهای جدید روزانه (حذف‌نشده)
 * @param subscriptions اشتراک‌های جدید روزانه — جدول {@code subscriptions} موجود است
 * @param revenue       درآمد روزانه از فاکتورهای PAID
 */
public record AdminChartsResponse(
        String period,
        List<DailyCount> newUsers,
        List<DailyCount> newTenants,
        List<DailyCount> subscriptions,
        List<DailyRevenue> revenue
) {

    /** @param date  روز UTC به شکل {@code yyyy-MM-dd} · @param count تعداد */
    public record DailyCount(String date, long count) {
    }

    public record DailyRevenue(String date, long invoiceCount, java.math.BigDecimal total) {
    }
}
