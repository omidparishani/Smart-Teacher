package ir.omid.smartteacher.billing.internal.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCallbackReceiptRepository extends JpaRepository<PaymentCallbackReceipt, UUID> {

    Optional<PaymentCallbackReceipt> findByAuthority(String authority);

    boolean existsByAuthority(String authority);
}
