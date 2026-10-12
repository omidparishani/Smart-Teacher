package ir.omid.smartteacher.tenant;

import ir.omid.smartteacher.common.jpa.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * مستأجر — واحد جداسازی و واحد صورتحساب.
 *
 * <p><b>چرا {@link TenantAwareEntity} نیست؟</b> چون {@code tenants} جدول
 * ریشه است و {@code tenant_id} ندارد؛ خودش مستأجر است. اگر
 * {@code @TenantId} می‌گرفت، هیچ‌وقت نمی‌شد فهرست مستأجرها را خواند.
 *
 * <p><b>دامنه فاز ۱:</b> این کلاس فقط موجودیت و Repository را دارد.
 * سرویس و API مدیریت مستأجر (ساخت مدرسه، تغییر پلن، تعلیق) بخشی از
 * فاز ۲ است و roadmap فاز ۱ آن را در دامنه زیرساخت نمی‌گنجاند.
 */
@Entity
@Table(name = "tenants")
public class Tenant extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TenantType type;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** نام یکتا در URL — فقط حروف کوچک، رقم و خط تیره. */
    @Column(name = "slug", nullable = false, length = 100)
    private String slug;

    /** مستأجر والد برای ساختار سلسله‌مراتبی سازمان. */
    @Column(name = "parent_tenant_id")
    private UUID parentTenantId;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TenantStatus status = TenantStatus.ACTIVE;

    /**
     * تنظیمات اختصاصی مستأجر به‌صورت JSONB.
     *
     * <p>به‌صورت {@code String} نگاشته شده و {@link JdbcTypeCode} به
     * Hibernate می‌گوید محتوا JSON خام است، نه متن ساده. اگر
     * {@code Map<String,Object>} می‌گذاشتیم، به وجود Jackson و پیکربندی
     * درستش گره می‌خورد؛ برای فاز ۱ که فقط نگه‌داری لازم است، {@code String}
     * کم‌ریسک‌تر است.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", nullable = false)
    private String settings = "{}";

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** سازنده بدون آرگومان برای JPA. */
    protected Tenant() {
    }

    public Tenant(TenantType type, String name, String slug, UUID ownerUserId) {
        this.type = type;
        this.name = name;
        this.slug = slug;
        this.ownerUserId = ownerUserId;
    }

    public TenantType getType() {
        return type;
    }

    public void setType(TenantType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public UUID getParentTenantId() {
        return parentTenantId;
    }

    public void setParentTenantId(UUID parentTenantId) {
        this.parentTenantId = parentTenantId;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(UUID ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public void setStatus(TenantStatus status) {
        this.status = status;
    }

    public String getSettings() {
        return settings;
    }

    public void setSettings(String settings) {
        this.settings = settings;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    /** آیا مستأجر می‌تواند عملیات نوشتنی انجام دهد؟ */
    public boolean isOperational() {
        return status == TenantStatus.ACTIVE && deletedAt == null;
    }
}
