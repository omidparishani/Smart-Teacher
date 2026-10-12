package ir.omid.smartteacher.billing;

/**
 * مصرف اتمیک سقف پلن — API عمومی ماژول billing برای سایر ماژول‌ها.
 *
 * <p>پیاده‌سازی در {@code internal.application.UsageMeteringService}.
 */
public interface UsageMetering {

    /** متریک سقف درخواست‌های AI در {@code plan_limits}. */
    String METRIC_AI_REQUESTS = "maxAIRequests";

    /**
     * یک واحد از متریک را مصرف می‌کند؛ اگر سقف پر باشد پرتاب می‌کند.
     *
     * @throws ir.omid.smartteacher.common.error.AppException با
     *         {@code USAGE_LIMIT_REACHED} یا {@code SUBSCRIPTION_EXPIRED}
     */
    void consume(String metric, long units);
}
