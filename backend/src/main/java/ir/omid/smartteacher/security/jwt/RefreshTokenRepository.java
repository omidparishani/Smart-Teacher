package ir.omid.smartteacher.security.jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * دسترسی به توکن‌های تازه‌سازی.
 *
 * <p>این جدول فیلتر خودکار مستأجر ندارد (دلیلش در
 * {@link RefreshToken}). جست‌وجو بر پایه {@code tokenHash} انجام
 * می‌شود که خودش یک راز است — کسی که هش را دارد، توکن را دارد. پس
 * نبودِ فیلتر مستأجر در اینجا نشتی ایجاد نمی‌کند: دسترسی از پیش نیازمند
 * داشتن توکن است.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * یافتن توکن با هش آن.
     *
     * <p>ایندکس یکتا روی {@code token_hash} است، پس این پرس‌وجو سریع
     * است و حداکثر یک رکورد برمی‌گرداند.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserId(UUID userId);

    List<RefreshToken> findByFamilyId(UUID familyId);

    /**
     * باطل‌کردن همه توکن‌های یک خانواده.
     *
     * <p>قلب تشخیص سرقت. وقتی توکنی که قبلاً مصرف شده دوباره ارائه شود،
     * یعنی یا مهاجم آن را دزدیده یا کاربر اصلی از نسخه قدیمی استفاده
     * کرده؛ در هر دو حالت باید <b>همه</b> اعضای خانواده باطل شوند. اگر
     * فقط همان یک توکن رد می‌شد، مهاجمی که نسخه‌ای در دست دارد همچنان
     * دسترسی داشت.
     *
     * <p>به‌روزرسانی انبوه است (نه بارگذاری و تغییر تک‌تک) تا در یک
     * پرس‌وجو انجام شود؛ همین باعث می‌شود پنجره مسابقه بین دو درخواست
     * هم‌زمان تا حد ممکن کوچک بماند. چون این عملیات قفل خوش‌بینانه
     * {@code @Version} را دور می‌زند، {@code clearAutomatically} لازم
     * است تا موجودیت‌های کهنه در حافظه نمانند.
     *
     * @param familyId خانواده هدف
     * @param now      زمان باطل‌سازی
     * @param reason   دلیل، برای حسابرسی
     * @return تعداد توکن‌های باطل‌شده
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RefreshToken t
               SET t.revokedAt = :now,
                   t.revokedReason = :reason
             WHERE t.familyId = :familyId
               AND t.revokedAt IS NULL
            """)
    int revokeFamily(@Param("familyId") UUID familyId,
                     @Param("now") Instant now,
                     @Param("reason") String reason);

    /**
     * باطل‌کردن همه توکن‌های فعال یک کاربر.
     *
     * <p>برای «خروج از همه دستگاه‌ها» و باطل‌سازی اجباری پس از تغییر
     * رمز عبور.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RefreshToken t
               SET t.revokedAt = :now,
                   t.revokedReason = :reason
             WHERE t.userId = :userId
               AND t.revokedAt IS NULL
            """)
    int revokeAllForUser(@Param("userId") UUID userId,
                         @Param("now") Instant now,
                         @Param("reason") String reason);

    /** تعداد توکن‌های فعال یک کاربر — برای نمایش «دستگاه‌های واردشده». */
    long countByUserIdAndRevokedAtIsNull(UUID userId);
}
