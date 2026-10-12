package ir.omid.smartteacher.eitaa.internal.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageCreditRepository extends JpaRepository<MessageCredit, UUID> {

    Optional<MessageCredit> findByTenantId(UUID tenantId);
}
