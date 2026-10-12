package ir.omid.smartteacher.ai.internal.domain;

import ir.omid.smartteacher.common.jpa.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * تخصیص پرامپت به دامنه — {@code prompt_assignments}.
 */
@Entity
@Table(name = "prompt_assignments")
public class PromptAssignment extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    private AiScopeType scopeType;

    @Column(name = "scope_id")
    private UUID scopeId;

    @Column(name = "prompt_id", nullable = false)
    private UUID promptId;

    @Column(name = "version_id")
    private UUID versionId;

    @Column(name = "priority", nullable = false)
    private int priority = 100;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "tenant_id")
    private UUID tenantId;

    protected PromptAssignment() {
    }

    public PromptAssignment(AiScopeType scopeType, UUID promptId) {
        this.scopeType = scopeType;
        this.promptId = promptId;
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

    public UUID getPromptId() {
        return promptId;
    }

    public void setPromptId(UUID promptId) {
        this.promptId = promptId;
    }

    public UUID getVersionId() {
        return versionId;
    }

    public void setVersionId(UUID versionId) {
        this.versionId = versionId;
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
}
