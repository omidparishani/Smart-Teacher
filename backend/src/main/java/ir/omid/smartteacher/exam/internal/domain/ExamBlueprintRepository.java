package ir.omid.smartteacher.exam.internal.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamBlueprintRepository extends JpaRepository<ExamBlueprint, UUID> {

    List<ExamBlueprint> findByExamIdOrderByCreatedAtDesc(UUID examId);
}
