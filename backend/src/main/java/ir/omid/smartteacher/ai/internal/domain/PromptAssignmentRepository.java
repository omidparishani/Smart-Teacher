package ir.omid.smartteacher.ai.internal.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromptAssignmentRepository extends JpaRepository<PromptAssignment, UUID> {

    @Query("""
            select a from PromptAssignment a, PromptEntity p
             where a.promptId = p.id
               and a.enabled = true
               and p.featureCode = :featureCode
               and a.scopeType = :scopeType
               and (
                    (:scopeId is null and a.scopeId is null)
                    or a.scopeId = :scopeId
               )
             order by a.priority asc
            """)
            List<PromptAssignment> findActive(
            @Param("featureCode") String featureCode,
            @Param("scopeType") AiScopeType scopeType,
            @Param("scopeId") UUID scopeId);

    List<PromptAssignment> findByPromptId(UUID promptId);
    void deleteByPromptId(UUID promptId);
}
