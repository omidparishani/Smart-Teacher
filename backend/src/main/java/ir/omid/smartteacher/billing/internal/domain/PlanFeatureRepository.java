package ir.omid.smartteacher.billing.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanFeatureRepository extends JpaRepository<PlanFeature, UUID> {

    List<PlanFeature> findByPlanId(UUID planId);

    Optional<PlanFeature> findByPlanIdAndFeatureCode(UUID planId, String featureCode);

    void deleteByPlanId(UUID planId);
}
