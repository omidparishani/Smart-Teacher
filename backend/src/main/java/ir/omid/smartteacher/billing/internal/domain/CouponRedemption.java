package ir.omid.smartteacher.billing.internal.domain;

import ir.omid.smartteacher.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** ثبت یک‌بار مصرف کوپن برای مستأجر. */
@Entity
@Table(name = "coupon_redemptions")
public class CouponRedemption extends BaseEntity {

    @Column(name = "coupon_id", nullable = false)
    private UUID couponId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "discount_applied", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountApplied = BigDecimal.ZERO;

    @Column(name = "redeemed_at", nullable = false)
    private Instant redeemedAt = Instant.now();

    protected CouponRedemption() {
    }

    public CouponRedemption(UUID couponId, UUID tenantId, BigDecimal discountApplied) {
        this.couponId = couponId;
        this.tenantId = tenantId;
        this.discountApplied = discountApplied;
    }

    public UUID getCouponId() {
        return couponId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setSubscriptionId(UUID subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public BigDecimal getDiscountApplied() {
        return discountApplied;
    }
}
