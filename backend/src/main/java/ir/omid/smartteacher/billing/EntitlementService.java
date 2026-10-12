package ir.omid.smartteacher.billing;

/**
 * بررسی entitlement — آیا Feature برای مستأجر جاری مجاز است؟
 *
 * <p>گام ۱: stub. اگر اشتراک ACTIVE و معتبر نباشد،
 * {@link FeatureNotEntitledException} پرتاب می‌شود.
 *
 * <p>فاز ۶ گام ۲: سقف اختیاری {@code storageBytes} از {@code plan_limits}.
 */
public interface EntitlementService {

    /** متریک سقف ذخیره‌سازی در {@code plan_limits}. */
    String METRIC_STORAGE_BYTES = "storageBytes";

    /** آیا Feature فعال است؟ بدون پرتاب استثنا. */
    boolean isEnabled(String featureCode);

    /**
     * الزام Feature — در صورت عدم دسترسی پرتاب می‌کند.
     *
     * @throws FeatureNotEntitledException اگر اشتراک منقضی یا Feature غیرفعال باشد
     */
    void requireFeature(String featureCode);

    /**
     * بررسی سقف اختیاری ذخیره‌سازی.
     *
     * <p>اگر پلن جاری متریک {@link #METRIC_STORAGE_BYTES} نداشته باشد،
     * عبور می‌کند (سقف اختیاری). اگر داشته باشد و
     * {@code currentUsed + additional} از سقف بگذرد،
     * {@link ir.omid.smartteacher.common.error.ErrorCode#USAGE_LIMIT_REACHED}.
     *
     * @param currentUsedBytes بایت‌های فعلی مستأجر
     * @param additionalBytes  بایت‌های در حال افزودن
     */
    void requireStorageCapacity(long currentUsedBytes, long additionalBytes);
}
