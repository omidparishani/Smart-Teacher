package ir.omid.smartteacher.ai.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromptVersionRepository extends JpaRepository<PromptVersionEntity, UUID> {

    List<PromptVersionEntity> findByPromptIdOrderByVersionNumberDesc(UUID promptId);

    Optional<PromptVersionEntity> findByPromptIdAndStatus(
            UUID promptId, PromptVersionStatus status);

    @Query("""
            select coalesce(max(v.versionNumber), 0) from PromptVersionEntity v
             where v.promptId = :promptId
            """)
    int findMaxVersionNumber(@Param("promptId") UUID promptId);
}
