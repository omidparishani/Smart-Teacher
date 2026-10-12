package ir.omid.smartteacher.billing.internal.application;

import ir.omid.smartteacher.billing.internal.api.dto.WalletResponse;
import ir.omid.smartteacher.billing.internal.api.dto.WalletTransactionResponse;
import ir.omid.smartteacher.billing.internal.domain.Wallet;
import ir.omid.smartteacher.billing.internal.domain.WalletRepository;
import ir.omid.smartteacher.billing.internal.domain.WalletTransaction;
import ir.omid.smartteacher.billing.internal.domain.WalletTransactionRepository;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * اعتبار/بدهکار کیف پول با قفل ردیف.
 */
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository,
                         WalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public WalletResponse currentWallet() {
        UUID tenantId = TenantContext.requireTenantId();
        Wallet wallet = walletRepository.findByTenantId(tenantId)
                .orElseGet(() -> new Wallet(tenantId));
        return toResponse(wallet);
    }

    @Transactional
    public WalletResponse credit(BigDecimal amount, String description) {
        if (amount == null || amount.signum() <= 0) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "مبلغ اعتبار باید مثبت باشد.");
        }
        return mutate(amount, "CREDIT", description);
    }

    @Transactional
    public WalletResponse debit(BigDecimal amount, String description) {
        if (amount == null || amount.signum() <= 0) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "مبلغ بدهکار باید مثبت باشد.");
        }
        return mutate(amount.negate(), "DEBIT", description);
    }

    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> listTransactions() {
        UUID tenantId = TenantContext.requireTenantId();
        return transactionRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(tx -> new WalletTransactionResponse(
                        tx.getId(), tx.getType(), tx.getAmount(),
                        tx.getBalanceAfter(), tx.getDescription(), tx.getCreatedAt()))
                .toList();
    }

    private WalletResponse mutate(BigDecimal signedAmount, String type, String description) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requirePrincipal().userId();

        Wallet wallet = walletRepository.findByTenantIdForUpdate(tenantId)
                .orElseGet(() -> walletRepository.save(new Wallet(tenantId)));

        BigDecimal next = wallet.getBalance().add(signedAmount);
        if (next.signum() < 0) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "موجودی کیف پول کافی نیست.");
        }
        wallet.setBalance(next);

        WalletTransaction tx = new WalletTransaction(
                wallet.getId(), tenantId, type, signedAmount.abs(), next);
        tx.setDescription(description);
        tx.setCreatedBy(userId);
        transactionRepository.save(tx);

        return toResponse(wallet);
    }

    private static WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getTenantId(),
                wallet.getBalance(),
                wallet.getCurrency());
    }
}
