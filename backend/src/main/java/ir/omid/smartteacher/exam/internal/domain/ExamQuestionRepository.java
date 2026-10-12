package ir.omid.smartteacher.exam.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, UUID> {

    List<ExamQuestion> findByExamIdOrderByOrderIndexAsc(UUID examId);

    Optional<ExamQuestion> findByIdAndExamId(UUID id, UUID examId);

    int countByExamId(UUID examId);
}
