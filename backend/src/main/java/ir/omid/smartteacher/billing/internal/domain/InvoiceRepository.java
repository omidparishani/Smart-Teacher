package ir.omid.smartteacher.billing.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findAllByOrderByIssuedAtDesc();

    Optional<Invoice> findBySubscriptionId(UUID subscriptionId);
}
