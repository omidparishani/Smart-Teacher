package ir.omid.smartteacher.billing.internal.domain;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageCounterRepository extends JpaRepository<UsageCounter, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from UsageCounter c
            where c.metric = :metric
              and c.periodStart = :periodStart
              and c.userId is null
            """)
    Optional<UsageCounter> findTenantMetricForUpdate(
            @Param("metric") String metric,
            @Param("periodStart") Instant periodStart);
}
