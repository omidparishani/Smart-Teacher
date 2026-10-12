package ir.omid.smartteacher.admin.internal.api.dto;

import java.util.Map;

/**
 * گزارش اشتراک‌ها + تقریب churn/conversion.
 *
 * @param total              مجموع
 * @param byStatus           وضعیت → تعداد
 * @param churnRateApprox    CANCELLED+EXPIRED / total (۰ اگر total=۰)
 * @param conversionRateApprox ACTIVE غیر رایگان تقریبی — paidActive / (paidActive+freeActive)
 * @param paidActiveCount    اشتراک ACTIVE روی پلن غیر FREE
 * @param freeActiveCount    اشتراک ACTIVE روی پلن FREE
 */
public record SubscriptionsReportResponse(
        long total,
        Map<String, Long> byStatus,
        double churnRateApprox,
        double conversionRateApprox,
        long paidActiveCount,
        long freeActiveCount
) {
}
