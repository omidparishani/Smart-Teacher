package ir.omid.smartteacher.eitaa.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationRuleRepository extends JpaRepository<AutomationRule, UUID> {

    List<AutomationRule> findByDeletedAtIsNullOrderByCreatedAtDesc();

    List<AutomationRule> findByTriggerTypeAndEnabledTrueAndDeletedAtIsNull(String triggerType);

    Optional<AutomationRule> findByIdAndDeletedAtIsNull(UUID id);
}
