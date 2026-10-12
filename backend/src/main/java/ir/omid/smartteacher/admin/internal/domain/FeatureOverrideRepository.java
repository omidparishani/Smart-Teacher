package ir.omid.smartteacher.admin.internal.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * بازنویسی امکانات سطح مستأجر.
 */
public interface FeatureOverrideRepository extends JpaRepository<FeatureOverride, UUID> {

    Optional<FeatureOverride> findByTenantIdAndFeatureCode(UUID tenantId, String featureCode);
}
