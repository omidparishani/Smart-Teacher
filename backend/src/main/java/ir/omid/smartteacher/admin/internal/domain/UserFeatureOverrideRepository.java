package ir.omid.smartteacher.admin.internal.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * بازنویسی امکانات سطح کاربر.
 */
public interface UserFeatureOverrideRepository extends JpaRepository<UserFeatureOverride, UUID> {

    Optional<UserFeatureOverride> findByUserIdAndFeatureCode(UUID userId, String featureCode);
}
