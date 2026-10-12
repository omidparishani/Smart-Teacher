package ir.omid.smartteacher.assignment.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * دسترسی به تکالیف — فیلتر مستأجر خودکار؛ حذف نرم صریح.
 */
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    Optional<Assignment> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            select a from Assignment a
            where a.deletedAt is null
              and a.classroomId in :classroomIds
              and (:pattern is null or lower(a.title) like :pattern escape '\\')
              and (:status is null or a.status = :status)
              and (:classroomId is null or a.classroomId = :classroomId)
            """)
    Page<Assignment> findForListing(@Param("classroomIds") List<UUID> classroomIds,
                                    @Param("pattern") String pattern,
                                    @Param("status") AssignmentStatus status,
                                    @Param("classroomId") UUID classroomId,
                                    Pageable pageable);
}
