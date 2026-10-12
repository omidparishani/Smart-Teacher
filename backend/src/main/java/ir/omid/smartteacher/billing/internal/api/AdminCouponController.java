package ir.omid.smartteacher.billing.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.billing.internal.api.dto.CouponResponse;
import ir.omid.smartteacher.billing.internal.api.dto.CreateCouponRequest;
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
 * مدیریت کوپن‌ها از پنل ادمین — {@code /api/v1/admin/coupons}.
 */
@RestController
@RequestMapping("/api/v1/admin/coupons")
@Tag(name = "کوپن‌های ادمین", description = "فهرست، ساخت و غیرفعال‌سازی کوپن")
public class AdminCouponController {

    private static final String ADMIN_ONLY = "hasAnyAuthority('SUPER_ADMIN','ADMIN')";

    private final BillingService billingService;

    public AdminCouponController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "فهرست همه کوپن‌ها")
    public List<CouponResponse> list() {
        return billingService.listAllCoupons();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "ساخت کوپن")
    public CouponResponse create(@Valid @RequestBody CreateCouponRequest request) {
        return billingService.createCoupon(request);
    }

    @PostMapping("/{couponId}/disable")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "غیرفعال‌سازی کوپن")
    public CouponResponse disable(@PathVariable UUID couponId) {
        return billingService.disableCoupon(couponId);
    }

    @PostMapping("/{couponId}/enable")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "فعال‌سازی کوپن")
    public CouponResponse enable(@PathVariable UUID couponId) {
        return billingService.enableCoupon(couponId);
    }

    @PutMapping("/{couponId}")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "ویرایش کوپن")
    public CouponResponse update(@PathVariable UUID couponId,
                                 @Valid @RequestBody ir.omid.smartteacher.billing.internal.api.dto.UpdateCouponRequest request) {
        return billingService.updateCoupon(couponId, request);
    }

    @DeleteMapping("/{couponId}")
    @PreAuthorize(ADMIN_ONLY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "حذف کوپن")
    public void delete(@PathVariable UUID couponId) {
        billingService.deleteCoupon(couponId);
    }
}
