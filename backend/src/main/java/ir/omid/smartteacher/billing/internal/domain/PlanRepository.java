package ir.omid.smartteacher.billing.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

    List<Plan> findByActiveTrueAndIsPublicTrueOrderBySortOrderAsc();

    Optional<Plan> findByCode(String code);

    List<Plan> findAllByOrderBySortOrderAsc();
}
