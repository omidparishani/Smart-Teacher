package ir.omid.smartteacher.ai.internal.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiUsageRecordRepository extends JpaRepository<AiUsageRecord, UUID> {

    long countByFeature(String feature);
}
