package ir.omid.smartteacher.eitaa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * دسترسی به پیوندهای کاربران ایتا.
 *
 * <p><b>⚠️ این جدول فیلتر خودکار مستأجر ندارد</b> — دلیلش در
 * {@link EitaaIdentity} توضیح داده شده: این جدول پیش از معلوم‌شدن
 * مستأجر خوانده می‌شود. پس هیچ کوئری‌ای اینجا نباید داده مستأجری
 * برگرداند؛ فقط نگاشت «کدام کاربر ایتا ↔ کدام کاربر ما».
 *
 * <p>هر کوئری که به داده دامنه‌ای (کلاس، تکلیف، دانش‌آموز) دست می‌زند
 * باید از مسیر جدول‌های {@code @TenantId} دار برود، نه از اینجا.
 */
public interface EitaaIdentityRepository extends JpaRepository<EitaaIdentity, UUID> {

    /**
     * یافتن پیوند با شناسه کاربر ایتا.
     *
     * <p>ایندکس یکتا روی {@code eitaa_user_id} است، پس سریع است و
     * حداکثر یک رکورد برمی‌گرداند.
     */
    Optional<EitaaIdentity> findByEitaaUserId(Long eitaaUserId);

    /**
     * یافتن پیوندهای یک کاربر داخلی.
     *
     * <p>فهرست است، نه یک مورد: در آینده ممکن است یک کاربر داخلی چند
     * حساب ایتا داشته باشد (مثلاً معلمی با دو حساب).
     */
    List<EitaaIdentity> findByUserId(UUID userId);

    /** آیا این حساب ایتا قبلاً به کاربری پیوند شده است؟ */
    boolean existsByEitaaUserId(Long eitaaUserId);
}
