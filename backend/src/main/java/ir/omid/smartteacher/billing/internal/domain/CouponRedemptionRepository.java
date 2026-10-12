package ir.omid.smartteacher.billing.internal.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, UUID> {

    long countByCouponIdAndTenantId(UUID couponId, UUID tenantId);

    @Query("select count(r) from CouponRedemption r where r.couponId = :couponId and r.tenantId = :tenantId")
    long countByCouponAndTenant(@Param("couponId") UUID couponId, @Param("tenantId") UUID tenantId);
}
