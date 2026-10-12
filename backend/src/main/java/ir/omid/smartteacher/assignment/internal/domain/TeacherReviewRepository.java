package ir.omid.smartteacher.assignment.internal.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * دسترسی به بازبینی‌های معلم — فقط درج و خواندن (Append-Only).
 */
public interface TeacherReviewRepository extends JpaRepository<TeacherReview, UUID> {

    List<TeacherReview> findBySubmissionIdOrderByReviewedAtAsc(UUID submissionId);
}
