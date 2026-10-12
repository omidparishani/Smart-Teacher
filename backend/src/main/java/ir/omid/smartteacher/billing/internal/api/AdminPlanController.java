package ir.omid.smartteacher.billing.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.billing.internal.api.dto.CreatePlanRequest;
import ir.omid.smartteacher.billing.internal.api.dto.PlanResponse;
import ir.omid.smartteacher.billing.internal.api.dto.UpdatePlanFeaturesRequest;
import ir.omid.smartteacher.billing.internal.api.dto.UpdatePlanRequest;
import ir.omid.smartteacher.billing.internal.application.BillingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * مدیریت پلن‌ها از پنل ادمین — {@code /api/v1/admin/plans}.
 */
@RestController
@RequestMapping("/api/v1/admin/plans")
@Tag(name = "پلن‌های ادمین", description = "کاتالوگ پلن‌ها")
public class AdminPlanController {

    private static final String ADMIN_ONLY = "hasAnyAuthority('SUPER_ADMIN','ADMIN')";

    private final BillingService billingService;

    public AdminPlanController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "فهرست همه پلن‌ها")
    public List<PlanResponse> list() {
        return billingService.listAllPlans();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "ساخت پلن")
    public PlanResponse create(@Valid @RequestBody CreatePlanRequest request) {
        return billingService.createPlan(request);
    }

    @PutMapping("/{planId}")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "به‌روزرسانی نام/قیمت پلن")
    public PlanResponse update(
            @PathVariable UUID planId,
            @Valid @RequestBody UpdatePlanRequest request) {
        return billingService.updatePlan(planId, request);
    }

    @PutMapping("/{planId}/features")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "به‌روزرسانی امکانات و سقف‌های پلن")
    public PlanResponse updateFeatures(
            @PathVariable UUID planId,
            @Valid @RequestBody UpdatePlanFeaturesRequest request) {
        return billingService.updatePlanFeaturesAndLimits(planId, request);
    }

    @GetMapping("/{planId}")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "دریافت جزئیات پلن")
    public PlanResponse get(@PathVariable UUID planId) {
        return billingService.getPlan(planId);
    }

    @PostMapping("/{planId}/disable")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "غیرفعال‌سازی پلن")
    public PlanResponse disable(@PathVariable UUID planId) {
        return billingService.disablePlan(planId);
    }

    @DeleteMapping("/{planId}")
    @PreAuthorize(ADMIN_ONLY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "حذف پلن")
    public void delete(@PathVariable UUID planId) {
        billingService.deletePlan(planId);
    }
}
