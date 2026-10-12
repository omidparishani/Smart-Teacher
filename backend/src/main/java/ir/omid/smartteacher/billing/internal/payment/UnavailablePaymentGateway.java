package ir.omid.smartteacher.billing.internal.payment;

import ir.omid.smartteacher.billing.PaymentGateway;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * جانشین وقتی Mock خاموش است و Adapter واقعی هنوز نیست.
 *
 * <p>عمداً هیچ API درگاهی حدس زده نمی‌شود — فقط مانع شکست بالا آمدن
 * کانتکست می‌شود تا مسیرهای غیرپرداخت کار کنند.
 */
@Component
@ConditionalOnProperty(name = "app.mock.payment", havingValue = "false", matchIfMissing = true)
public class UnavailablePaymentGateway implements PaymentGateway {

    @Override
    public String code() {
        return "UNAVAILABLE";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public PaymentInitiation initiate(PaymentInitiationRequest request) {
        throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR,
                "درگاه پرداخت پیکربندی نشده است. در توسعه app.mock.payment=true بگذارید.");
    }

    @Override
    public PaymentVerification verify(PaymentVerificationRequest request) {
        throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR,
                "درگاه پرداخت پیکربندی نشده است.");
    }
}
