package ir.omid.smartteacher.ai.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptRepository extends JpaRepository<PromptEntity, UUID> {

    Optional<PromptEntity> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<PromptEntity> findAllByOrderByCodeAsc();

    List<PromptEntity> findByFeatureCodeOrderByCodeAsc(String featureCode);
}
