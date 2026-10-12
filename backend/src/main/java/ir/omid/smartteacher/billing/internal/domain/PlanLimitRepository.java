package ir.omid.smartteacher.billing.internal.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanLimitRepository extends JpaRepository<PlanLimit, UUID> {

    List<PlanLimit> findByPlanId(UUID planId);

    void deleteByPlanId(UUID planId);
}
