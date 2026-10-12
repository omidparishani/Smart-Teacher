package ir.omid.smartteacher.admin.internal.domain;

import ir.omid.smartteacher.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "system_settings")
public class SystemSetting extends BaseEntity {

    @Column(name = "key", nullable = false, length = 200, unique = true)
    private String key;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "value_type", nullable = false, length = 20)
    private String valueType = "STRING";

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "encrypted", nullable = false)
    private boolean encrypted = false;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "updated_by")
    private UUID updatedBy;

    protected SystemSetting() {
    }

    public SystemSetting(String key, String value, String valueType, String category, String description) {
        this.key = key;
        this.value = value;
        this.valueType = valueType != null ? valueType : "STRING";
        this.category = category;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }
}
