package ir.omid.smartteacher.admin.internal.api.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * گزارش درآمد از {@code invoices} — فقط فاکتورهای با وضعیت پرداخت‌شده.
 *
 * @param paidInvoiceCount تعداد فاکتور {@code PAID}
 * @param paidTotal        مجموع {@code total} فاکتورهای پرداخت‌شده
 * @param currency         واحد پول غالب (اگر چند واحد باشد، پرتکرارترین)
 * @param byStatus         شمارش فاکتور بر اساس وضعیت
 */
public record RevenueReportResponse(
        long paidInvoiceCount,
        BigDecimal paidTotal,
        String currency,
        Map<String, Long> byStatus
) {
}
