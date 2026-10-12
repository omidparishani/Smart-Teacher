package ir.omid.smartteacher.assignment.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * دسترسی به ارسال‌های تکلیف.
 */
public interface AssignmentSubmissionRepository
        extends JpaRepository<AssignmentSubmission, UUID> {

    List<AssignmentSubmission> findByAssignmentIdOrderBySubmittedAtAsc(UUID assignmentId);

    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(
            UUID assignmentId, UUID studentId);

    long countByAssignmentId(UUID assignmentId);

    boolean existsByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);
}
