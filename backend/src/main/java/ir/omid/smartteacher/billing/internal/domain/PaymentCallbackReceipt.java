package ir.omid.smartteacher.billing.internal.domain;

import ir.omid.smartteacher.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** رسید پردازش‌شدهٔ callback پرداخت — یکتایی authority برای idempotency. */
@Entity
@Table(name = "payment_callback_receipts")
public class PaymentCallbackReceipt extends BaseEntity {

    @Column(name = "authority", nullable = false, length = 120)
    private String authority;

    @Column(name = "gateway_code", nullable = false, length = 40)
    private String gatewayCode = "MOCK";

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "reference_code", length = 120)
    private String referenceCode;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    protected PaymentCallbackReceipt() {
    }

    public PaymentCallbackReceipt(String authority, String gatewayCode,
                                  UUID subscriptionId, UUID tenantId) {
        this.authority = authority;
        this.gatewayCode = gatewayCode;
        this.subscriptionId = subscriptionId;
        this.tenantId = tenantId;
    }

    public String getAuthority() {
        return authority;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }
}
