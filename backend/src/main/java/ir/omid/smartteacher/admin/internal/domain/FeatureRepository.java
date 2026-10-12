package ir.omid.smartteacher.admin.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * دسترسی به کاتالوگ امکانات.
 */
public interface FeatureRepository extends JpaRepository<Feature, UUID> {

    Optional<Feature> findByCode(String code);

    boolean existsByCode(String code);

    List<Feature> findAllByOrderBySortOrderAscCodeAsc();
}
