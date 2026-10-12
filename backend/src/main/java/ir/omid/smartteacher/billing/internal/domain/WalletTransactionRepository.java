package ir.omid.smartteacher.billing.internal.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    List<WalletTransaction> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
