package ir.omid.smartteacher.assignment.internal.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * دسترسی به ارزیابی‌های AI.
 */
public interface AiEvaluationRepository extends JpaRepository<AiEvaluation, UUID> {

    Optional<AiEvaluation> findBySubmissionId(UUID submissionId);
}
