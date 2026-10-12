package ir.omid.smartteacher.admin.internal.api.dto;

import java.util.Map;

/**
 * گزارش کاربران — شمارش بر اساس وضعیت (حذف‌نشده‌ها).
 *
 * @param total    مجموع حذف‌نشده
 * @param byStatus نقشهٔ وضعیت → تعداد ({@code ACTIVE}, {@code DISABLED}, …)
 */
public record UsersReportResponse(
        long total,
        Map<String, Long> byStatus
) {
}
