package ir.omid.smartteacher.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * درگاه پرداخت — فقط قرارداد. هیچ API واقعی اینجا نیست
 * ({@code docs/payments.md}).
 */
public interface PaymentGateway {

    String code();

    boolean isAvailable();

    PaymentInitiation initiate(PaymentInitiationRequest request);

    PaymentVerification verify(PaymentVerificationRequest request);

    record PaymentInitiationRequest(
            UUID paymentId,
            BigDecimal amount,
            String currency,
            String description,
            String callbackUrl,
            Map<String, String> metadata
    ) {}

    record PaymentInitiation(
            String authority,
            String redirectUrl,
            Instant expiresAt
    ) {}

    record PaymentVerificationRequest(
            String authority,
            BigDecimal amount,
            Map<String, String> callbackParams
    ) {}

    record PaymentVerification(
            boolean successful,
            String referenceId,
            BigDecimal paidAmount,
            Instant paidAt,
            String failureReason
    ) {}
}
