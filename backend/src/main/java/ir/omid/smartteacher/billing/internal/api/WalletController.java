package ir.omid.smartteacher.billing.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.billing.internal.api.dto.WalletAmountRequest;
import ir.omid.smartteacher.billing.internal.api.dto.WalletResponse;
import ir.omid.smartteacher.billing.internal.api.dto.WalletTransactionResponse;
import ir.omid.smartteacher.billing.internal.application.WalletService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API کیف پول مستأجر.
 */
@RestController
@RequestMapping("/api/v1/wallet")
@Tag(name = "کیف پول", description = "اعتبار و بدهکار کیف پول")
public class WalletController {

    static final String STAFF_ONLY =
            "hasAnyAuthority('TENANT_OWNER','TENANT_SCHOOL_ADMIN','TENANT_TEACHER','SUPER_ADMIN','ADMIN')";

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "موجودی کیف پول جاری")
    public WalletResponse current() {
        return walletService.currentWallet();
    }

    @PostMapping("/credit")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "افزایش موجودی")
    public WalletResponse credit(@Valid @RequestBody WalletAmountRequest request) {
        return walletService.credit(request.amount(), request.description());
    }

    @PostMapping("/debit")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "کاهش موجودی")
    public WalletResponse debit(@Valid @RequestBody WalletAmountRequest request) {
        return walletService.debit(request.amount(), request.description());
    }

    @GetMapping("/transactions")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "فهرست تراکنش‌ها")
    public List<WalletTransactionResponse> transactions() {
        return walletService.listTransactions();
    }
}
