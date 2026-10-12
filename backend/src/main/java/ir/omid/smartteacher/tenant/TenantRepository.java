package ir.omid.smartteacher.tenant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * دسترسی به مستأجرها.
 *
 * <p>جدول {@code tenants} سطح پلتفرم است، پس این Repository فیلتر خودکار
 * مستأجر ندارد. هر متدی که برای کاربر عادی صدا زده می‌شود باید در سرویس
 * با بررسی مالکیت یا نقش همراه باشد.
 *
 * <p>همه متدها شرط {@code deleted_at IS NULL} دارند تا با ایندکس یکتای
 * جزئی {@code uk_tenants_slug} هم‌خوان باشند: نامک یک مستأجر حذف‌شده
 * آزاد است، پس بررسی یکتایی نباید رکورد حذف‌شده را ببیند.
 */
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlugAndDeletedAtIsNull(String slug);

    boolean existsBySlugAndDeletedAtIsNull(String slug);

    List<Tenant> findByOwnerUserIdAndDeletedAtIsNull(UUID ownerUserId);

    List<Tenant> findByParentTenantIdAndDeletedAtIsNull(UUID parentTenantId);

    List<Tenant> findByStatusAndDeletedAtIsNull(TenantStatus status);

    /** تعداد مستأجرهای حذف‌نشده — شمارنده داشبورد ادمین. */
    long countByDeletedAtIsNull();

    /** یک مستأجر حذف‌نشده با شناسه. */
    Optional<Tenant> findByIdAndDeletedAtIsNull(UUID id);

    /** فهرست صفحه‌بندی‌شده مستأجرهای حذف‌نشده — پنل ادمین. */
    Page<Tenant> findByDeletedAtIsNull(Pageable pageable);

    /** فهرست مدارس (مستأجر با نوع SCHOOL) حذف‌نشده. */
    Page<Tenant> findByTypeAndDeletedAtIsNull(TenantType type, Pageable pageable);
}
