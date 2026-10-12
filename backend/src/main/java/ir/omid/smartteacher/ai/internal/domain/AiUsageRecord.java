package ir.omid.smartteacher.ai.internal.domain;

import ir.omid.smartteacher.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * ثبت مصرف یک درخواست AI — {@code ai_usage_records} (حداقل گام ۱).
 */
@Entity
@Table(name = "ai_usage_records")
public class AiUsageRecord extends TenantAwareEntity {

    @Column(name = "feature", nullable = false, length = 80)
    private String feature;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "prompt_tokens", nullable = false)
    private long promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private long completionTokens;

    @Column(name = "cost", nullable = false, precision = 18, scale = 8)
    private BigDecimal cost = BigDecimal.ZERO;

    protected AiUsageRecord() {
    }

    public AiUsageRecord(String feature,
                         String model,
                         long promptTokens,
                         long completionTokens,
                         BigDecimal cost) {
        this.feature = feature;
        this.model = model;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.cost = cost == null ? BigDecimal.ZERO : cost;
    }

    public String getFeature() {
        return feature;
    }

    public String getModel() {
        return model;
    }

    public long getPromptTokens() {
        return promptTokens;
    }

    public long getCompletionTokens() {
        return completionTokens;
    }

    public BigDecimal getCost() {
        return cost;
    }
}
