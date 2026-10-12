package ir.omid.smartteacher.common.jpa;

import ir.omid.smartteacher.common.util.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * ریشه همه موجودیت‌های سیستم.
 *
 * <p>شامل ستون‌های استانداردی است که در طراحی دیتابیس برای هر جدول الزامی
 * شده‌اند ({@code docs/database.md} بخش ۱).
 *
 * <p><b>شناسه:</b> در سازنده تولید می‌شود، نه با {@code @GeneratedValue}.
 * چرا؟ چون UUID نسخه ۷ باید در اپلیکیشن ساخته شود (PostgreSQL 17 تابع
 * {@code uuidv7()} ندارد) و تولید زودهنگام اجازه می‌دهد شناسه پیش از
 * {@code persist} در دسترس باشد — لازم برای رویدادهای دامنه‌ای و
 * ثبت حسابرسی.
 *
 * <p><b>نسخه:</b> {@code @Version} قفل خوش‌بینانه را فعال می‌کند. اگر دو
 * درخواست همزمان یک رکورد را ویرایش کنند، دومی استثنا می‌گیرد — به‌جای
 * اینکه بی‌صدا روی اولی بنویسد.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UuidV7Generator.generate();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    /**
     * یکسان‌بودن بر پایه شناسه.
     *
     * <p>پیش از {@code persist} شناسه از قبل مقدار دارد (چون در سازنده
     * تولید می‌شود)، پس این پیاده‌سازی برخلاف الگوی رایج JPA در همه
     * حالت‌ها درست کار می‌کند.
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !getClass().equals(other.getClass())) {
            return false;
        }
        return id.equals(((BaseEntity) other).id);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    /**
     * ⚠️ هر زیرکلاسی که فیلد حساس دارد (کلید API، توکن) <b>باید</b>
     * این متد را بازنویسی کند و مقدار را حذف نماید.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", version=" + version + '}';
    }
}
