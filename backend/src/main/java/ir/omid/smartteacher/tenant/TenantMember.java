package ir.omid.smartteacher.tenant;

import ir.omid.smartteacher.common.jpa.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * عضویت یک کاربر در یک مستأجر.
 *
 * <p><b>⚠️ چرا {@code @TenantId} ندارد، با اینکه ستون
 * {@code tenant_id} دارد؟</b> این مهم‌ترین تصمیم طراحی این کلاس است.
 *
 * <p>این جدول در <b>زمان ورود</b> خوانده می‌شود: کاربر وارد می‌شود،
 * هنوز هیچ مستأجری انتخاب نشده، و باید بپرسیم «این کاربر عضو کدام
 * مستأجرهاست؟». اگر Hibernate فیلتر خودکار مستأجر را اعمال می‌کرد، آن
 * کوئری با مستأجر جاری (که هنوز وجود ندارد) اجرا می‌شد و <b>همیشه خالی
 * برمی‌گشت</b> — یعنی هیچ کاربری هرگز نمی‌توانست وارد شود. این یک
 * بن‌بست راه‌اندازی است، نه یک محدودیت جزئی.
 *
 * <p>پس اسکوپ این جدول در لایه سرویس اعمال می‌شود: کوئری‌ها یا
 * {@code findByUserId} هستند (خودبه‌خود به کاربر احراز‌هویت‌شده محدودند)
 * یا {@code findByTenantId} که سرویس مقدارش را از
 * {@code TenantContext.requireTenantId()} می‌گیرد، هرگز از ورودی کاربر.
 * همین استدلال برای {@code refresh_tokens} هم برقرار است.
 *
 * <p>ارتباط با {@code User} و {@code Tenant} با شناسه ({@code UUID})
 * مدل شده، نه {@code @ManyToOne}. دلیل: عبور از مرز ماژول با ارجاع
 * مستقیم به موجودیت ماژول دیگر، قید معماری Spring Modulith را می‌شکند و
 * بارگذاری تنبل را به منطق کسب‌وکار نشت می‌دهد.
 */
@Entity
@Table(name = "tenant_members")
public class TenantMember extends AuditableEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    /** سازنده بدون آرگومان برای JPA. */
    protected TenantMember() {
    }

    public TenantMember(UUID tenantId, UUID userId, MemberRole role) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.role = role;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public void setStatus(MemberStatus status) {
        this.status = status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    /** آیا این عضویت اجازه دسترسی می‌دهد؟ */
    public boolean isEffective() {
        return status == MemberStatus.ACTIVE;
    }
}
