package ir.omid.smartteacher.ai.internal.domain;

import ir.omid.smartteacher.common.jpa.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * مدل ثبت‌شده در کاتالوگ — {@code ai_models}.
 *
 * <p>شناسه مدل برای API واقعی در {@link #code} است و در کد Java
 * hardcode نمی‌شود ({@code docs/ai-providers.md} اصل طراحی).
 */
@Entity
@Table(name = "ai_models")
public class AiModelEntity extends AuditableEntity {

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "input_cost_per_million", precision = 18, scale = 8)
    private BigDecimal inputCostPerMillion;

    @Column(name = "output_cost_per_million", precision = 18, scale = 8)
    private BigDecimal outputCostPerMillion;

    @Column(name = "cached_input_cost_per_million", precision = 18, scale = 8)
    private BigDecimal cachedInputCostPerMillion;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "USD";

    protected AiModelEntity() {
    }

    public AiModelEntity(UUID providerId, String code, String displayName) {
        this.providerId = providerId;
        this.code = code;
        this.displayName = displayName;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getInputCostPerMillion() {
        return inputCostPerMillion;
    }

    public void setInputCostPerMillion(BigDecimal inputCostPerMillion) {
        this.inputCostPerMillion = inputCostPerMillion;
    }

    public BigDecimal getOutputCostPerMillion() {
        return outputCostPerMillion;
    }

    public void setOutputCostPerMillion(BigDecimal outputCostPerMillion) {
        this.outputCostPerMillion = outputCostPerMillion;
    }

    public BigDecimal getCachedInputCostPerMillion() {
        return cachedInputCostPerMillion;
    }

    public void setCachedInputCostPerMillion(BigDecimal cachedInputCostPerMillion) {
        this.cachedInputCostPerMillion = cachedInputCostPerMillion;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
