package ir.omid.smartteacher.billing.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    @Query("""
            select s from Subscription s
            where s.status = 'ACTIVE'
            order by s.endDate desc nulls last
            """)
    List<Subscription> findActiveOrdered();

    default Optional<Subscription> findCurrentActive() {
        return findActiveOrdered().stream().findFirst();
    }

    List<Subscription> findByStatusOrderByCreatedAtDesc(String status);

    @Query("""
            select s from Subscription s
            where s.status = 'ACTIVE'
              and s.endDate is not null
              and s.endDate < :now
            """)
    List<Subscription> findActivePastEndDate(
            @org.springframework.data.repository.query.Param("now") java.time.Instant now);
}
