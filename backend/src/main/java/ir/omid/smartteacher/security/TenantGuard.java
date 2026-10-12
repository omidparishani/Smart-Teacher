package ir.omid.smartteacher.security;

import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.security.audit.SecurityAuditLog;
import ir.omid.smartteacher.tenant.Tenant;
import ir.omid.smartteacher.tenant.TenantAwareEntity;
import ir.omid.smartteacher.tenant.TenantContext;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * لایه دفاعی دوم در برابر دسترسی بین‌مستأجری (IDOR).
 *
 * <p><b>لایه اول</b> فیلتر خودکار Hibernate است که با
 * {@code @TenantId} روی {@link TenantAwareEntity} کار می‌کند و
 * کوئری‌های فراموش‌شده را می‌گیرد. <b>لایه دوم</b> همین کلاس است: برای
 * دسترسی به یک رکورد <i>مشخص</i>، مالکیت را صریح بررسی می‌کند.
 *
 * <p>هیچ‌کدام جایگزین دیگری نیست. لایه اول نمی‌تواند تصمیم‌های منطقی
 * (مثلاً «معلم فقط کلاس خودش») را بیان کند؛ لایه دوم نمی‌تواند کوئری
 * فهرست فراموش‌شده را نجات دهد.
 *
 * <p><b>⚠️ قاعده آهنین:</b> پاسخ همیشه {@code 404} است، هرگز
 * {@code 403}. اگر {@code 403} برگردانیم، مهاجم می‌فهمد رکورد با آن
 * شناسه <b>وجود دارد</b> و فقط مالکش کس دیگری است — و همین برای ساختن
 * فهرست شناسه‌های معتبر کافی است. {@code 404} این کانال جانبی را
 * می‌بندد. دلیل کامل در {@code docs/multi-tenancy.md} بخش ۴.
 */
@Component
public class TenantGuard {

    private final SecurityAuditLog securityAuditLog;

    public TenantGuard(SecurityAuditLog securityAuditLog) {
        this.securityAuditLog = securityAuditLog;
    }

    /**
     * بررسی مالکیت یک موجودیت متعلق به مستأجر.
     *
     * <p>الگوی استفاده در سرویس:
     * <pre>{@code
     * ClassRoom room = tenantGuard.requireOwned(
     *         repository.findById(id).orElse(null), "ClassRoom");
     * }</pre>
     *
     * @param entity موجودیتی که با شناسه از دیتابیس خوانده شده؛
     *               {@code null} اگر یافت نشده باشد
     * @return همان موجودیت، اگر متعلق به مستأجر جاری باشد
     * @throws ResourceNotFoundException اگر موجودیت {@code null} باشد،
     *                                   متعلق به مستأجر دیگری باشد، یا
     *                                   هیچ مستأجری در زمینه نباشد
     */
    public <T extends TenantAwareEntity> T requireOwned(T entity) {
        return requireOwned(entity, entity == null
                ? "Unknown"
                : entity.getClass().getSimpleName());
    }

    /**
     * بررسی مالکیت با نام صریح نوع موجودیت — برای Log حسابرسی خواناتر.
     *
     * @param entity     موجودیت
     * @param entityType نام نوع برای حسابرسی
     * @param <T>        نوع موجودیت
     * @return همان موجودیت
     */
    public <T extends TenantAwareEntity> T requireOwned(T entity, String entityType) {
        // اول مستأجر جاری: اگر زمینه مستأجر وجود نداشته باشد، خطای درست
        // «احراز هویت» است نه «یافت نشد».
        UUID currentTenantId = TenantContext.requireTenantId();

        if (entity == null) {
            throw ResourceNotFoundException.generic();
        }

        if (!Objects.equals(entity.getTenantId(), currentTenantId)) {
            securityAuditLog.idorAttempt(entityType, entity.getId(), entity.getTenantId());
            throw ResourceNotFoundException.generic();
        }

        return entity;
    }

    /**
     * بررسی مالکیت برای موجودیت‌هایی که {@link TenantAwareEntity}
     * نیستند اما ستون {@code tenant_id} دارند.
     *
     * <p>مثل {@code TenantMember} که به‌عمد {@code @TenantId} ندارد
     * (دلیلش در Javadoc خودش). چنین جدول‌هایی فیلتر خودکار نمی‌گیرند،
     * پس این متد تنها سد راه است و <b>باید</b> در همه مسیرهایی که رکورد
     * مشخصی را با شناسه می‌خوانند صدا زده شود.
     *
     * @param resourceTenantId مستأجر مالک رکورد
     * @param entityType       نام نوع برای حسابرسی
     * @param entityId         شناسه رکورد برای حسابرسی
     * @throws ResourceNotFoundException اگر مستأجر نخواند
     */
    public void requireTenantMatch(UUID resourceTenantId, String entityType, UUID entityId) {
        UUID currentTenantId = TenantContext.requireTenantId();

        if (!Objects.equals(resourceTenantId, currentTenantId)) {
            securityAuditLog.idorAttempt(entityType, entityId, resourceTenantId);
            throw ResourceNotFoundException.generic();
        }
    }

    /**
     * بررسی دسترسی به یک مستأجر.
     *
     * <p>دسترسی وقتی مجاز است که کاربر یا <i>داخل</i> همان مستأجر باشد
     * یا مالک آن. توجه کنید که این متد عمداً استثنای سوپر‌ادمین ندارد:
     * عبور سوپر‌ادمین از مرز مستأجر یکی از سه مسیر مجاز است و باید در
     * کد <b>صریح</b> دیده شود، نه پنهان در یک شرط داخل این متد. اگر
     * اینجا پنهانش می‌کردیم، با یک {@code grep} نمی‌شد شمرد که کجاها
     * مرز مستأجر شکسته می‌شود.
     *
     * @param tenant مستأجر هدف
     * @return همان مستأجر
     * @throws ResourceNotFoundException اگر کاربر به آن دسترسی نداشته باشد
     */
    public Tenant requireAccess(Tenant tenant) {
        UUID currentTenantId = TenantContext.requireTenantId();

        if (tenant == null) {
            throw ResourceNotFoundException.generic();
        }

        boolean isMemberOfTenant = Objects.equals(tenant.getId(), currentTenantId);
        boolean isOwner = TenantContext.currentUserId()
                .map(ownerId -> Objects.equals(tenant.getOwnerUserId(), ownerId))
                .orElse(false);

        if (!isMemberOfTenant && !isOwner) {
            securityAuditLog.idorAttempt("Tenant", tenant.getId(), tenant.getId());
            throw ResourceNotFoundException.generic();
        }

        return tenant;
    }
}
