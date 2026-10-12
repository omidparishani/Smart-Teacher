package ir.omid.smartteacher.billing.internal.domain;

import ir.omid.smartteacher.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "plan_limits")
public class PlanLimit extends BaseEntity {

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "metric", nullable = false, length = 50)
    private String metric;

    @Column(name = "limit_value", nullable = false)
    private long limitValue;

    @Column(name = "period", nullable = false, length = 20)
    private String period = "MONTHLY";

    protected PlanLimit() {
    }

    public PlanLimit(UUID planId, String metric, long limitValue, String period) {
        this.planId = planId;
        this.metric = metric;
        this.limitValue = limitValue;
        this.period = period;
    }

    public UUID getPlanId() { return planId; }
    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    public long getLimitValue() { return limitValue; }
    public void setLimitValue(long limitValue) { this.limitValue = limitValue; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
}
