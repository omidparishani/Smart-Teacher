package ir.omid.smartteacher.exam.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {

    List<ExamAttempt> findByExamIdOrderByStartedAtDesc(UUID examId);

    Optional<ExamAttempt> findByExamIdAndStudentIdAndStatus(
            UUID examId, UUID studentId, AttemptStatus status);

    @Query("select coalesce(max(a.attemptNumber), 0) from ExamAttempt a where a.examId = :examId and a.studentId = :studentId")
    int maxAttemptNumber(@Param("examId") UUID examId, @Param("studentId") UUID studentId);
}
