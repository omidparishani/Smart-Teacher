package ir.omid.smartteacher.billing.internal.domain;

import ir.omid.smartteacher.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class Subscription extends TenantAwareEntity {

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "purchased_by_user_id")
    private UUID purchasedByUserId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew;

    @Column(name = "price_paid", precision = 19, scale = 4)
    private BigDecimal pricePaid;

    @Column(name = "discount_amount", precision = 19, scale = 4)
    private BigDecimal discountAmount;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    protected Subscription() {
    }

    public Subscription(UUID planId) {
        this.planId = planId;
    }

    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID planId) { this.planId = planId; }
    public UUID getPurchasedByUserId() { return purchasedByUserId; }
    public void setPurchasedByUserId(UUID purchasedByUserId) { this.purchasedByUserId = purchasedByUserId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getStartDate() { return startDate; }
    public void setStartDate(Instant startDate) { this.startDate = startDate; }
    public Instant getEndDate() { return endDate; }
    public void setEndDate(Instant endDate) { this.endDate = endDate; }
    public boolean isAutoRenew() { return autoRenew; }
    public void setAutoRenew(boolean autoRenew) { this.autoRenew = autoRenew; }
    public BigDecimal getPricePaid() { return pricePaid; }
    public void setPricePaid(BigDecimal pricePaid) { this.pricePaid = pricePaid; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    /** آیا اشتراک هم‌اکنون فعال و در بازه زمانی معتبر است؟ */
    public boolean isCurrentlyActive() {
        if (!"ACTIVE".equals(status)) {
            return false;
        }
        Instant now = Instant.now();
        if (startDate != null && now.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !now.isAfter(endDate);
    }
}
