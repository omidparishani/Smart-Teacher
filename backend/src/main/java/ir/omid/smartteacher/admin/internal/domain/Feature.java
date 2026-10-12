package ir.omid.smartteacher.admin.internal.domain;

import ir.omid.smartteacher.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * امکان پلتفرم — کاتالوگ Feature Flags ({@code docs/billing.md} بخش ۴).
 *
 * <p>سطح پلتفرم است؛ {@code tenant_id} ندارد. فعال‌بودن مؤثر از
 * پیش‌فرض + override مستأجر/کاربر + پلن (فاز صورتحساب) محاسبه می‌شود.
 */
@Entity
@Table(name = "features")
public class Feature extends BaseEntity {

    @Column(name = "code", nullable = false, length = 60, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "default_enabled", nullable = false)
    private boolean defaultEnabled;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Feature() {
    }

    public Feature(String code, String name, String category) {
        this.code = code;
        this.name = name;
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public void setDefaultEnabled(boolean defaultEnabled) {
        this.defaultEnabled = defaultEnabled;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
