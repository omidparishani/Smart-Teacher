package ir.omid.smartteacher.billing.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.billing.internal.api.dto.AdminSubscriptionResponse;
import ir.omid.smartteacher.billing.internal.application.BillingService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * فهرست اشتراک‌های پلتفرم — {@code /api/v1/admin/subscriptions}.
 */
@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@Tag(name = "اشتراک‌های ادمین", description = "فهرست read-only اشتراک‌ها")
public class AdminSubscriptionController {

    private static final String ADMIN_ONLY = "hasAnyAuthority('SUPER_ADMIN','ADMIN')";

    private final BillingService billingService;

    public AdminSubscriptionController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "فهرست اشتراک‌ها")
    public List<AdminSubscriptionResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return billingService.listAllSubscriptions(page, size);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "لغو اشتراک توسط ادمین")
    public AdminSubscriptionResponse cancel(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "لغو توسط ادمین") String reason) {
        return billingService.adminCancelSubscription(id, reason);
    }

    @PostMapping("/{id}/extend")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "تمدید اشتراک توسط ادمین")
    public AdminSubscriptionResponse extend(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "30") int days) {
        return billingService.adminExtendSubscription(id, days);
    }
}
