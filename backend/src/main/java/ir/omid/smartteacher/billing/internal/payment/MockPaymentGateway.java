package ir.omid.smartteacher.billing.internal.payment;

import ir.omid.smartteacher.billing.PaymentGateway;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * درگاه پرداخت شبیه‌ساز — تأیید خودکار برای dev/test.
 *
 * <p>با {@code app.mock.payment=true} فعال می‌شود. هیچ تماس شبکه‌ای
 * ندارد و API واقعی هیچ درگاهی حدس زده نمی‌شود.
 */
@Component
@ConditionalOnProperty(name = "app.mock.payment", havingValue = "true")
public class MockPaymentGateway implements PaymentGateway {

    private final ConcurrentHashMap<String, PaymentInitiationRequest> pending =
            new ConcurrentHashMap<>();

    @Override
    public String code() {
        return "MOCK";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public PaymentInitiation initiate(PaymentInitiationRequest request) {
        String authority = "mock-" + UUID.randomUUID();
        pending.put(authority, request);
        return new PaymentInitiation(
                authority,
                "https://mock-payment.local/pay/" + authority,
                Instant.now().plusSeconds(1800));
    }

    @Override
    public PaymentVerification verify(PaymentVerificationRequest request) {
        PaymentInitiationRequest original = pending.remove(request.authority());
        if (original == null) {
            return new PaymentVerification(false, null, null, null, "authority نامعتبر");
        }
        return new PaymentVerification(
                true,
                "REF-MOCK-" + request.authority().substring(0, Math.min(8, request.authority().length())),
                original.amount(),
                Instant.now(),
                null);
    }

    /** peek بدون حذف — برای callback idempotent قبل از activate. */
    public PaymentInitiationRequest peek(String authority) {
        return pending.get(authority);
    }

    /** حذف authority پس از فعال‌سازی موفق. */
    public void consumeAuthority(String authority) {
        pending.remove(authority);
    }

    /** تأیید فوری بدون callback — سازگاری با تست‌های قدیمی. */
    public PaymentVerification autoSucceed(PaymentInitiation initiation, BigDecimal amount) {
        pending.remove(initiation.authority());
        return new PaymentVerification(
                true,
                "REF-MOCK-AUTO",
                amount,
                Instant.now(),
                null);
    }
}
