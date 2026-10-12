package ir.omid.smartteacher.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * دسترسی به کاربران.
 *
 * <p>جدول {@code users} سطح پلتفرم است و فیلتر خودکار مستأجر ندارد —
 * درست است، چون یک کاربر می‌تواند عضو چند مستأجر باشد. محدودسازی
 * دسترسی به کاربران یک مستأجر وظیفه سرویس است: باید از
 * {@code TenantMemberRepository.findByTenantId} شروع کند و شناسه‌ها را
 * به این Repository بدهد، نه اینکه مستقیم فهرست همه کاربران را بگیرد.
 *
 * <p><b>⚠️ چرا همه متدها {@code DeletedAtIsNull} دارند؟</b> چون
 * ایندکس‌های یکتای V2 جزئی هستند: {@code lower(username) WHERE ... AND
 * deleted_at IS NULL}. یعنی نام کاربری یک حساب حذف‌شده <b>آزاد</b> است
 * و کاربر دیگری می‌تواند آن را بگیرد. اگر این متدها رکورد حذف‌شده را هم
 * برمی‌گرداندند، {@code existsBy...} به‌اشتباه می‌گفت «این نام کاربری
 * گرفته است» و ثبت‌نام کاربر جدید را برای همیشه می‌بست. پس شرط
 * {@code deleted_at IS NULL} دقیقاً همان چیزی است که ایندکس دیتابیس
 * می‌گوید و باید در کوئری هم تکرار شود.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Optional<User> findByMobileAndDeletedAtIsNull(String mobile);

    boolean existsByUsernameIgnoreCaseAndDeletedAtIsNull(String username);

    boolean existsByMobileAndDeletedAtIsNull(String mobile);

    /** فهرست کاربران فعال (حذف‌نشده) — برای پنل ادمین. */
    Page<User> findByDeletedAtIsNull(Pageable pageable);

    /** تعداد کاربران حذف‌نشده — شمارنده داشبورد. */
    long countByDeletedAtIsNull();

    /** تعداد کاربران با وضعیت مشخص و حذف‌نشده. */
    long countByStatusAndDeletedAtIsNull(UserStatus status);

    /** یک کاربر حذف‌نشده با شناسه — شکل امن دسترسی به یک رکورد مشخص. */
    Optional<User> findByIdAndDeletedAtIsNull(UUID id);
}
