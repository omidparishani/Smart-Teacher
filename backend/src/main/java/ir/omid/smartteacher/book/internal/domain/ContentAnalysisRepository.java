package ir.omid.smartteacher.book.internal.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * دسترسی به تحلیل‌های محتوا.
 */
public interface ContentAnalysisRepository extends JpaRepository<ContentAnalysis, UUID> {

    List<ContentAnalysis> findByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            AnalysisTargetType targetType, UUID targetId);

    List<ContentAnalysis> findByTargetIdInAndDeletedAtIsNullOrderByCreatedAtDesc(List<UUID> targetIds);
}
