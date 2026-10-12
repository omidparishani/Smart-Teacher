package ir.omid.smartteacher.ai.internal.domain;

import ir.omid.smartteacher.common.jpa.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * تخصیص Provider در یک سطح دامنه — {@code ai_provider_assignments}.
 */
@Entity
@Table(name = "ai_provider_assignments")
public class AiProviderAssignment extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    private AiScopeType scopeType;

    @Column(name = "scope_id")
    private UUID scopeId;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "model_id")
    private UUID modelId;

    @Column(name = "priority", nullable = false)
    private int priority = 100;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "overrides", nullable = false)
    private String overrides = "{}";

    protected AiProviderAssignment() {
    }

    public AiProviderAssignment(AiScopeType scopeType, UUID providerId) {
        this.scopeType = scopeType;
        this.providerId = providerId;
    }

    public AiScopeType getScopeType() {
        return scopeType;
    }

    public void setScopeType(AiScopeType scopeType) {
        this.scopeType = scopeType;
    }

    public UUID getScopeId() {
        return scopeId;
    }

    public void setScopeId(UUID scopeId) {
        this.scopeId = scopeId;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public void setProviderId(UUID providerId) {
        this.providerId = providerId;
    }

    public UUID getModelId() {
        return modelId;
    }

    public void setModelId(UUID modelId) {
        this.modelId = modelId;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getOverrides() {
        return overrides;
    }

    public void setOverrides(String overrides) {
        this.overrides = overrides == null || overrides.isBlank() ? "{}" : overrides;
    }
}
