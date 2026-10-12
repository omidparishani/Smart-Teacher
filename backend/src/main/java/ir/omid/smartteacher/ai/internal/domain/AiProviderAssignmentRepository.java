package ir.omid.smartteacher.ai.internal.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiProviderAssignmentRepository extends JpaRepository<AiProviderAssignment, UUID> {

    @Query("""
            select a from AiProviderAssignment a
             where a.enabled = true
               and a.scopeType = :scopeType
               and (
                    (:scopeId is null and a.scopeId is null)
                    or a.scopeId = :scopeId
               )
             order by a.priority asc
            """)
    List<AiProviderAssignment> findActive(
            @Param("scopeType") AiScopeType scopeType,
            @Param("scopeId") UUID scopeId);
}
