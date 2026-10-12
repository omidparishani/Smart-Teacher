package ir.omid.smartteacher.common.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

/**
 * موجودیت پایه برای جدول‌هایی که «چه کسی» را ثبت می‌کنند.
 *
 * <p>مقدار این دو ستون از {@code AuditorAware} می‌آید که آن هم کاربر جاری
 * را از {@code TenantContext} می‌خواند — یعنی از JWT، هرگز از ورودی کاربر.
 *
 * <p>برای جدول‌هایی که ستون {@code created_by} ندارند (مثل جدول‌های اتصالی
 * خالص یا توکن‌ها) از {@link BaseEntity} مستقیم ارث‌بری کنید.
 */
@MappedSuperclass
public abstract class AuditableEntity extends BaseEntity {

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }
}
