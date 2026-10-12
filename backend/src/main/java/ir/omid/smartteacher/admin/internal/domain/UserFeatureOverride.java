package ir.omid.smartteacher.admin.internal.domain;

import ir.omid.smartteacher.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * بازنویسی امکان در سطح یک کاربر مشخص.
 */
@Entity
@Table(name = "user_feature_overrides")
public class UserFeatureOverride extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

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

    protected UserFeatureOverride() {
    }

    public UserFeatureOverride(UUID userId, String featureCode, boolean enabled) {
        this.userId = userId;
        this.featureCode = featureCode;
        this.enabled = enabled;
    }

    public UUID getUserId() {
        return userId;
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
