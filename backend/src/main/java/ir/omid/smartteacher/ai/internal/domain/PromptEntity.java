package ir.omid.smartteacher.ai.internal.domain;

import ir.omid.smartteacher.common.jpa.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * قالب پرامپت پایدار — جدول {@code prompts}.
 */
@Entity
@Table(name = "prompts")
public class PromptEntity extends AuditableEntity {

    @Column(name = "code", nullable = false, length = 80, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "feature_code", nullable = false, length = 80)
    private String featureCode;

    @Column(name = "description")
    private String description;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    protected PromptEntity() {
    }

    public PromptEntity(String code, String name, String featureCode) {
        this.code = code;
        this.name = name;
        this.featureCode = featureCode;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }
}
