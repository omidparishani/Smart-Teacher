package ir.omid.smartteacher.billing.internal.domain;

import ir.omid.smartteacher.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * امکان فعال روی یک پلن — {@code plan_features}.
 */
@Entity
@Table(name = "plan_features")
public class PlanFeature extends BaseEntity {

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "feature_code", nullable = false, length = 60)
    private String featureCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb", nullable = false)
    private String config = "{}";

    protected PlanFeature() {
    }

    public PlanFeature(UUID planId, String featureCode, boolean enabled) {
        this.planId = planId;
        this.featureCode = featureCode;
        this.enabled = enabled;
    }

    public UUID getPlanId() {
        return planId;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config == null || config.isBlank() ? "{}" : config;
    }
}
