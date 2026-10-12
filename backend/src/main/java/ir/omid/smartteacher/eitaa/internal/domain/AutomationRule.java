package ir.omid.smartteacher.eitaa.internal.domain;

import ir.omid.smartteacher.tenant.SoftDeletableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** قاعده خودکارسازی بر اساس رویداد داخلی — نه پیام ورودی ایتا. */
@Entity
@Table(name = "automation_rules")
public class AutomationRule extends SoftDeletableTenantEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "trigger_type", nullable = false, length = 40)
    private String triggerType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_config", columnDefinition = "jsonb")
    private String triggerConfig;

    @Column(name = "action_type", nullable = false, length = 40)
    private String actionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_config", columnDefinition = "jsonb")
    private String actionConfig;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "execution_count", nullable = false)
    private int executionCount;

    @Column(name = "last_executed_at")
    private Instant lastExecutedAt;

    protected AutomationRule() {
    }

    public AutomationRule(String name, String triggerType, String actionType) {
        this.name = name;
        this.triggerType = triggerType;
        this.actionType = actionType;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public String getTriggerConfig() { return triggerConfig; }
    public void setTriggerConfig(String triggerConfig) { this.triggerConfig = triggerConfig; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getActionConfig() { return actionConfig; }
    public void setActionConfig(String actionConfig) { this.actionConfig = actionConfig; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getExecutionCount() { return executionCount; }
    public void setExecutionCount(int executionCount) { this.executionCount = executionCount; }
    public Instant getLastExecutedAt() { return lastExecutedAt; }
    public void setLastExecutedAt(Instant lastExecutedAt) { this.lastExecutedAt = lastExecutedAt; }

    public void markExecuted() {
        this.executionCount++;
        this.lastExecutedAt = Instant.now();
    }
}
