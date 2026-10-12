package ir.omid.smartteacher.admin.internal.domain;

import ir.omid.smartteacher.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * بازنویسی امکان در سطح مستأجر.
 */
@Entity
@Table(name = "feature_overrides")
public class FeatureOverride extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "feature_code", nullable = false, length = 60)
    private String featureCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "granted_by")
    private UUID grantedBy;

    protected FeatureOverride() {
    }

    public FeatureOverride(UUID tenantId, String featureCode, boolean enabled) {
        this.tenantId = tenantId;
        this.featureCode = featureCode;
        this.enabled = enabled;
    }

    public UUID getTenantId() {
        return tenantId;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UUID getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(UUID grantedBy) {
        this.grantedBy = grantedBy;
    }
}
