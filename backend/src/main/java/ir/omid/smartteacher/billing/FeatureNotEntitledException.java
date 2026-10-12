package ir.omid.smartteacher.billing;

/**
 * استثنا وقتی Feature در پلن/اشتراک فعال نیست یا اشتراک منقضی است.
 */
public class FeatureNotEntitledException extends ir.omid.smartteacher.common.error.AppException {

    public FeatureNotEntitledException(String featureCode) {
        super(ir.omid.smartteacher.common.error.ErrorCode.FEATURE_NOT_ENTITLED,
                "این قابلیت («" + featureCode + "») در پلن فعلی شما فعال نیست. "
                        + "برای ارتقا به بخش «اشتراک من» مراجعه کنید.");
    }

    public static FeatureNotEntitledException expired() {
        return new FeatureNotEntitledException(
                ir.omid.smartteacher.common.error.ErrorCode.SUBSCRIPTION_EXPIRED);
    }

    private FeatureNotEntitledException(ir.omid.smartteacher.common.error.ErrorCode code) {
        super(code, code.defaultMessage());
    }
}
