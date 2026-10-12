package ir.omid.smartteacher.billing.internal.api.dto;

import java.util.List;

/**
 * به‌روزرسانی امکانات و سقف‌های پلن.
 */
public record UpdatePlanFeaturesRequest(
        List<FeatureItem> features,
        List<LimitItem> limits
) {
    public record FeatureItem(String featureCode, Boolean enabled) {
    }

    public record LimitItem(String metric, Long limitValue, String period) {
    }
}
