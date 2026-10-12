package ir.omid.smartteacher.exam.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamRepository extends JpaRepository<Exam, UUID> {

    Optional<Exam> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            select e from Exam e
            where e.deletedAt is null
              and (:classroomId is null or e.classroomId = :classroomId)
              and (:status is null or e.status = :status)
            """)
    Page<Exam> findForListing(@Param("classroomId") UUID classroomId,
                              @Param("status") ExamStatus status,
                              Pageable pageable);
}
