package ir.omid.smartteacher.billing.internal.domain;

import ir.omid.smartteacher.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * شمارنده مصرف دوره — {@code usage_counters} از V18.
 */
@Entity
@Table(name = "usage_counters")
public class UsageCounter extends TenantAwareEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "metric", nullable = false, length = 50)
    private String metric;

    @Column(name = "used_value", nullable = false)
    private long usedValue;

    @Column(name = "limit_value", nullable = false)
    private long limitValue;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "reserved_value", nullable = false)
    private long reservedValue;

    protected UsageCounter() {
    }

    public UsageCounter(String metric, long limitValue, Instant periodStart, Instant periodEnd) {
        this.metric = metric;
        this.limitValue = limitValue;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getMetric() {
        return metric;
    }

    public long getUsedValue() {
        return usedValue;
    }

    public void setUsedValue(long usedValue) {
        this.usedValue = usedValue;
    }

    public long getLimitValue() {
        return limitValue;
    }

    public void setLimitValue(long limitValue) {
        this.limitValue = limitValue;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public long getReservedValue() {
        return reservedValue;
    }

    public void setReservedValue(long reservedValue) {
        this.reservedValue = reservedValue;
    }

    public long available() {
        return Math.max(0L, limitValue - usedValue - reservedValue);
    }
}
