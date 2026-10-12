package ir.omid.smartteacher.tenant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * دسترسی به عضویت‌های مستأجر.
 *
 * <p><b>⚠️ این Repository فیلتر خودکار مستأجر ندارد</b> — دلیلش در
 * Javadoc خود {@link TenantMember} توضیح داده شده. یعنی اگر سرویسی
 * {@code findById} را صدا بزند، رکورد هر مستأجری را می‌گیرد. پس:
 * <ul>
 *   <li>برای دسترسی به یک رکورد مشخص، همیشه از
 *       {@link #findByTenantIdAndUserId} استفاده کنید که مستأجر را در
 *       خود شرط دارد.</li>
 *   <li>اگر ناچار به {@code findById} شدید، نتیجه را <b>باید</b> به
 *       {@code TenantGuard.requireOwned} بسپارید.</li>
 *   <li>هیچ متدی نباید {@code tenantId} را از ورودی کاربر بگیرد؛
 *       همیشه از {@code TenantContext}.</li>
 * </ul>
 *
 * <p>این جدول {@code deleted_at} ندارد (حذف عضویت با
 * {@link MemberStatus#REMOVED} مدل شده)، پس شرط حذف نرم لازم نیست.
 */
public interface TenantMemberRepository extends JpaRepository<TenantMember, UUID> {

    /** عضویت یک کاربر مشخص در یک مستأجر مشخص — شکل امن دسترسی. */
    Optional<TenantMember> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    /**
     * همه عضویت‌های یک کاربر در همه مستأجرها.
     *
     * <p>در زمان ورود استفاده می‌شود تا فهرست مستأجرهای قابل انتخاب
     * ساخته شود. به کاربر احراز‌هویت‌شده محدود است، پس نشت بین‌مستأجری
     * ندارد.
     */
    List<TenantMember> findByUserId(UUID userId);

    /** همه اعضای یک مستأجر. */
    List<TenantMember> findByTenantId(UUID tenantId);

    List<TenantMember> findByTenantIdAndStatus(UUID tenantId, MemberStatus status);

    boolean existsByTenantIdAndUserId(UUID tenantId, UUID userId);

    long countByTenantIdAndStatus(UUID tenantId, MemberStatus status);
}
