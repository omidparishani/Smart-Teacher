package ir.omid.smartteacher.tenant.testsupport;

import ir.omid.smartteacher.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * موجودیت کاوشگر برای آزمودن جداسازی مستأجر. ⚠️ فقط تست.
 *
 * <p>این کلاس در {@code src/test/java} است و هرگز روی classpath
 * production نمی‌آید. جدولش را Migration فقط-تست
 * ({@code V900__isolation_probe.sql}) می‌سازد.
 *
 * <p><b>⚠️ چرا بسته‌اش {@code tenant.testsupport} است؟</b> عمدی است.
 * Spring Modulith هر زیربسته <i>مستقیم</i> بسته ریشه را یک ماژول
 * مستقل می‌شمارد. اگر این کلاس در
 * {@code ir.omid.smartteacher.testsupport} می‌بود، Modulith آن را
 * ماژولی جدا می‌دید که به {@code tenant} وابسته است — و بسته به
 * پیکربندی، {@code verify()} می‌توانست رد کند. با گذاشتنش داخل بسته
 * {@code tenant}، صرف‌نظر از اینکه Modulith کلاس‌های تست را وارد
 * می‌کند یا نه، بخشی از همان ماژول است و مسئله‌ای ایجاد نمی‌کند.
 *
 * <p>ارث‌بری از {@link TenantAwareEntity} همان چیزی است که آزمون را
 * معنادار می‌کند: ستون {@code tenant_id} با {@code @TenantId} نگاشت
 * شده و Hibernate باید خودکار کوئری‌ها را فیلتر کند. اگر این نگاشت
 * خراب شود، تست‌های {@code TenantIsolationTest} شکست می‌خورند.
 */
@Entity
@Table(name = "isolation_probe")
public class IsolationProbe extends TenantAwareEntity {

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    /** سازنده بدون آرگومان — الزام JPA. */
    protected IsolationProbe() {
    }

    public IsolationProbe(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
