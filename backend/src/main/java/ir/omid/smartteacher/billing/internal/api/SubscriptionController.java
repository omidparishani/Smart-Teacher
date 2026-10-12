package ir.omid.smartteacher.billing.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.billing.internal.api.dto.CancelSubscriptionRequest;
import ir.omid.smartteacher.billing.internal.api.dto.CheckoutRequest;
import ir.omid.smartteacher.billing.internal.api.dto.CouponValidateRequest;
import ir.omid.smartteacher.billing.internal.api.dto.CouponValidateResponse;
import ir.omid.smartteacher.billing.internal.api.dto.InvoiceResponse;
import ir.omid.smartteacher.billing.internal.api.dto.PaymentCallbackRequest;
import ir.omid.smartteacher.billing.internal.api.dto.PlanResponse;
import ir.omid.smartteacher.billing.internal.api.dto.SubscriptionResponse;
import ir.omid.smartteacher.billing.internal.application.BillingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * مسیرهای پلن و اشتراک — docs/api.md بخش ۴.۹.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "اشتراک", description = "پلن‌ها و اشتراک معلم")
public class SubscriptionController {

    static final String STAFF_ONLY =
            "hasAnyAuthority('TENANT_OWNER','TENANT_SCHOOL_ADMIN','TENANT_TEACHER')";

    private final BillingService billingService;

    public SubscriptionController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/plans")
    @Operation(summary = "فهرست پلن‌های عمومی", description = "بدون احراز هویت.")
    public List<PlanResponse> listPlans() {
        return billingService.listPublicPlans();
    }

    @GetMapping("/subscription/current")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "اشتراک فعال جاری")
    public SubscriptionResponse current() {
        return billingService.currentSubscription();
    }

    @PostMapping("/subscription/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "خرید اشتراک با MockPaymentGateway")
    public SubscriptionResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return billingService.checkout(request);
    }

    @PostMapping("/subscription/cancel")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "انصراف از اشتراک جاری")
    public SubscriptionResponse cancel(@RequestBody(required = false) CancelSubscriptionRequest request) {
        return billingService.cancelCurrent(
                request != null ? request : new CancelSubscriptionRequest(null));
    }

    @GetMapping("/subscription/invoices")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "فهرست فاکتورهای مستأجر")
    public List<InvoiceResponse> invoices() {
        return billingService.listInvoices();
    }

    @PostMapping("/coupons/validate")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "اعتبارسنجی کد تخفیف بدون redeem")
    public CouponValidateResponse validateCoupon(@Valid @RequestBody CouponValidateRequest request) {
        return billingService.validateCoupon(request);
    }

    @PostMapping("/payments/mock/callback")
    @PreAuthorize(STAFF_ONLY)
    @Operation(summary = "Callback Mock پرداخت — idempotent")
    public SubscriptionResponse mockCallback(@Valid @RequestBody PaymentCallbackRequest request) {
        return billingService.processPaymentCallback(request.authority());
    }
}
