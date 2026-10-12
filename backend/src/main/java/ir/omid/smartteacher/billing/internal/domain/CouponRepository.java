package ir.omid.smartteacher.billing.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    List<Coupon> findAllByOrderByCodeAsc();

    /**
     * افزایش اتمیک used_count فقط اگر هنوز زیر سقف باشد.
     *
     * @return تعداد ردیف به‌روزشده — ۰ یعنی ظرفیت پر شده
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE coupons
               SET used_count = used_count + 1,
                   updated_at = now(),
                   version = version + 1
             WHERE id = :id
               AND (usage_limit IS NULL OR used_count < usage_limit)
            """, nativeQuery = true)
    int incrementUsedCountAtomically(@Param("id") UUID id);
}
