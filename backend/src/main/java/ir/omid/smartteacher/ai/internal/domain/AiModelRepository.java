package ir.omid.smartteacher.ai.internal.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiModelRepository extends JpaRepository<AiModelEntity, UUID> {

    List<AiModelEntity> findAllByOrderByDisplayNameAsc();

    List<AiModelEntity> findByProviderIdOrderByCodeAsc(UUID providerId);

    boolean existsByProviderIdAndCodeIgnoreCase(UUID providerId, String code);
}
