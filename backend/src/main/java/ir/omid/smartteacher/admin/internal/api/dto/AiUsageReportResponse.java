package ir.omid.smartteacher.admin.internal.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * گزارش مصرف AI + تفکیک مدل/ارائه‌دهنده تقریبی.
 */
public record AiUsageReportResponse(
        long requestCount,
        long promptTokens,
        long completionTokens,
        BigDecimal totalCost,
        List<ByModel> byModel,
        List<ByProvider> byProvider,
        List<ByUser> byUser
) {
    public record ByModel(String model, long requestCount, BigDecimal totalCost) {
    }

    public record ByProvider(String provider, long requestCount, BigDecimal totalCost) {
    }

    public record ByUser(
            java.util.UUID userId,
            String userName,
            long requestCount,
            long promptTokens,
            long completionTokens,
            BigDecimal totalCost
    ) {
    }
}
