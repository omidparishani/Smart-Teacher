package ir.omid.smartteacher.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import ir.omid.smartteacher.common.jpa.AuditableEntity;

/**
 * ریشه همه موجودیت‌های «متعلق به مستأجر».
 *
 * <p>حاشیه امنیت اصلی مدل جداسازی همین کلاس است: نشانه‌گذاری ستون با
 * {@link TenantId} باعث می‌شود Hibernate خودکار شرط
 * {@code tenant_id = :currentTenant} را به <b>هر</b> کوئری این موجودیت
 * اضافه کند — خواندن، شمارش، به‌روزرسانی و حذف. یعنی حتی اگر توسعه‌دهنده
 * فراموش کند در شرط کوئری مستأجر را بگذارد، داده‌ی مستأجر دیگری برنمی‌گردد.
 *
 * <p><b>دو لایه دفاعی:</b> این لایه اول (خودکار) است. لایه دوم
 * {@code TenantGuard} در سرویس هاست که برای دسترسی به یک رکورد مشخص
 * (مثل {@code GET /classes/{id}}) مالکیت را صریح بررسی می‌کند و در صورت
 * عدم مالکیت {@code 404} می‌دهد، نه {@code 403}. هیچ‌کدام جایگزین
 * دیگری نیست: لایه اول کوئری‌های فراموش‌شده را می‌گیرد، لایه دوم
 * منطق دسترسی را صریح و قابل‌بازبینی می‌کند.
 *
 * <p><b>⚠️ چه موجودیتی نباید از این کلاس ارث‌بری کند؟</b> جدول‌هایی که
 * برای <i>شناسایی</i> مستأجر لازم‌اند و پیش از معلوم‌شدن آن خوانده
 * می‌شوند — مثل {@code tenant_members} و {@code refresh_tokens}. اگر
 * آن‌ها هم فیلتر خودکار می‌گرفتند، کوئری «مستأجرهای این کاربر» در زمان
 * ورود خالی برمی‌گشت و ورود برای همیشه می‌شکست.
 */
@MappedSuperclass
public abstract class TenantAwareEntity extends AuditableEntity {

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /**
     * تخصیص شناسه مستأجر در صورت خالی‌بودن.
     *
     * <p>Hibernate معمولاً خودش این مقدار را از
     * {@code CurrentTenantIdentifierResolver} پر می‌کند. این متد تور
     * اطمینان دوم است: اگر به هر دلیلی پر نشده باشد، مقدار از
     * {@link TenantContext} گرفته می‌شود.
     *
     * <p>اگر هیچ مستأجری در زمینه نباشد، {@code requireTenantId} پرتاب
     * می‌کند و ذخیره‌سازی شکست می‌خورد. این رفتار عمدی است: نوشتن رکورد
     * مستأجری بدون مستأجر هرگز نباید بی‌صدا موفق شود.
     */
    @PrePersist
    private void assignTenantIfAbsent() {
        if (tenantId == null) {
            tenantId = TenantContext.requireTenantId();
        }
    }

    public UUID getTenantId() {
        return tenantId;
    }

    /**
     * ⚠️ موجودیت‌های حساس زیرکلاس باید این را بازنویسی کنند.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{id=" + getId() + ", tenantId=" + tenantId + ", version=" + getVersion() + '}';
    }
}
