package ir.omid.smartteacher.ai.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiProviderRepository extends JpaRepository<AiProviderEntity, UUID> {

    Optional<AiProviderEntity> findByCodeIgnoreCase(String code);

    List<AiProviderEntity> findAllByOrderByCodeAsc();

    boolean existsByCodeIgnoreCase(String code);
}
