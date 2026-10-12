package ir.omid.smartteacher.billing.internal.application;

import ir.omid.smartteacher.billing.PaymentGateway;
import ir.omid.smartteacher.billing.internal.platform.BillingPlatformQueries;
import ir.omid.smartteacher.billing.internal.api.dto.AdminSubscriptionResponse;
import ir.omid.smartteacher.billing.internal.api.dto.CancelSubscriptionRequest;
import ir.omid.smartteacher.billing.internal.api.dto.CheckoutRequest;
import ir.omid.smartteacher.billing.internal.api.dto.CouponResponse;
import ir.omid.smartteacher.billing.internal.api.dto.CouponValidateRequest;
import ir.omid.smartteacher.billing.internal.api.dto.CouponValidateResponse;
import ir.omid.smartteacher.billing.internal.api.dto.CreateCouponRequest;
import ir.omid.smartteacher.billing.internal.api.dto.CreatePlanRequest;
import ir.omid.smartteacher.billing.internal.api.dto.InvoiceResponse;
import ir.omid.smartteacher.billing.internal.api.dto.PlanResponse;
import ir.omid.smartteacher.billing.internal.api.dto.SubscriptionResponse;
import ir.omid.smartteacher.billing.internal.api.dto.UpdatePlanFeaturesRequest;
import ir.omid.smartteacher.billing.internal.api.dto.UpdatePlanRequest;
import ir.omid.smartteacher.billing.internal.domain.Invoice;
import ir.omid.smartteacher.billing.internal.domain.InvoiceRepository;
import ir.omid.smartteacher.billing.internal.domain.PaymentCallbackReceipt;
import ir.omid.smartteacher.billing.internal.domain.PaymentCallbackReceiptRepository;
import ir.omid.smartteacher.billing.internal.domain.Plan;
import ir.omid.smartteacher.billing.internal.domain.PlanFeature;
import ir.omid.smartteacher.billing.internal.domain.PlanFeatureRepository;
import ir.omid.smartteacher.billing.internal.domain.PlanLimit;
import ir.omid.smartteacher.billing.internal.domain.PlanLimitRepository;
import ir.omid.smartteacher.billing.internal.domain.PlanRepository;
import ir.omid.smartteacher.billing.internal.domain.Subscription;
import ir.omid.smartteacher.billing.internal.domain.SubscriptionRepository;
import ir.omid.smartteacher.billing.internal.payment.MockPaymentGateway;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.tenant.TenantContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * سرویس پلن و اشتراک — فاز ۱۰ (کوپن، فاکتور اتمیک، callback، انصراف).
 */
@Service
public class BillingService {

    private final PlanRepository planRepository;
    private final PlanFeatureRepository planFeatureRepository;
    private final PlanLimitRepository planLimitRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentCallbackReceiptRepository callbackReceiptRepository;
    private final PaymentGateway paymentGateway;
    private final InvoiceNumberService invoiceNumberService;
    private final CouponService couponService;
    private final BillingPlatformQueries billingPlatformQueries;

    public BillingService(PlanRepository planRepository,
                          PlanFeatureRepository planFeatureRepository,
                          PlanLimitRepository planLimitRepository,
                          SubscriptionRepository subscriptionRepository,
                          InvoiceRepository invoiceRepository,
                          PaymentCallbackReceiptRepository callbackReceiptRepository,
                          PaymentGateway paymentGateway,
                          InvoiceNumberService invoiceNumberService,
                          CouponService couponService,
                          BillingPlatformQueries billingPlatformQueries) {
        this.planRepository = planRepository;
        this.planFeatureRepository = planFeatureRepository;
        this.planLimitRepository = planLimitRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.callbackReceiptRepository = callbackReceiptRepository;
        this.paymentGateway = paymentGateway;
        this.invoiceNumberService = invoiceNumberService;
        this.couponService = couponService;
        this.billingPlatformQueries = billingPlatformQueries;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> listPublicPlans() {
        return planRepository.findByActiveTrueAndIsPublicTrueOrderBySortOrderAsc().stream()
                .map(this::toPlanResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> listAllPlans() {
        return planRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toPlanResponse)
                .toList();
    }

    @Transactional
    public PlanResponse createPlan(CreatePlanRequest request) {
        if (planRepository.findByCode(request.code().trim()).isPresent()) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "کد پلن تکراری است.");
        }
        Plan plan = new Plan(request.code().trim().toUpperCase(),
                request.name().trim(), request.price());
        plan.setDescription(request.description());
        plan.setCurrency(request.currency() != null ? request.currency() : "IRT");
        plan.setBillingPeriod(request.billingPeriod() != null
                ? request.billingPeriod() : "MONTHLY");
        plan.setTrialDays(request.trialDays());
        plan.setPublic(request.isPublic() == null || request.isPublic());
        plan.setDefault(Boolean.TRUE.equals(request.isDefault()));
        plan.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 100);
        plan.setActive(true);
        return toPlanResponse(planRepository.save(plan));
    }

    /**
     * جایگزینی کامل امکانات و سقف‌های پلن (PUT).
     */
    @Transactional
    public PlanResponse updatePlanFeaturesAndLimits(UUID planId, UpdatePlanFeaturesRequest request) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(ResourceNotFoundException::generic);

        if (request.features() != null) {
            planFeatureRepository.deleteByPlanId(planId);
            planFeatureRepository.flush();
            for (UpdatePlanFeaturesRequest.FeatureItem item : request.features()) {
                if (item.featureCode() == null || item.featureCode().isBlank()) {
                    continue;
                }
                boolean enabled = item.enabled() == null || item.enabled();
                planFeatureRepository.save(new PlanFeature(
                        planId, item.featureCode().trim().toUpperCase(), enabled));
            }
        }

        if (request.limits() != null) {
            List<PlanLimit> existing = planLimitRepository.findByPlanId(planId);
            planLimitRepository.deleteAll(existing);
            planLimitRepository.flush();
            for (UpdatePlanFeaturesRequest.LimitItem item : request.limits()) {
                if (item.metric() == null || item.limitValue() == null) {
                    continue;
                }
                String period = item.period() != null ? item.period() : "MONTHLY";
                planLimitRepository.save(new PlanLimit(
                        planId, item.metric().trim(), item.limitValue(), period));
            }
        }

        return toPlanResponse(plan);
    }

    @Transactional
    public PlanResponse updatePlan(UUID planId, UpdatePlanRequest request) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(ResourceNotFoundException::generic);
        if (request.name() != null && !request.name().isBlank()) {
            plan.setName(request.name().trim());
        }
        if (request.price() != null) {
            if (request.price().signum() < 0) {
                throw new AppException(ErrorCode.VALIDATION_FAILED, "قیمت نمی‌تواند منفی باشد.");
            }
            plan.setPrice(request.price());
        }
        return toPlanResponse(plan);
    }

    @Transactional(readOnly = true)
    public PlanResponse getPlan(UUID planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> ResourceNotFoundException.of("پلن"));
        return toPlanResponse(plan);
    }

    @Transactional
    public PlanResponse disablePlan(UUID planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> ResourceNotFoundException.of("پلن"));
        plan.setActive(false);
        return toPlanResponse(planRepository.save(plan));
    }

    @Transactional
    public void deletePlan(UUID planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> ResourceNotFoundException.of("پلن"));
        planFeatureRepository.deleteByPlanId(planId);
        planLimitRepository.deleteByPlanId(planId);
        planRepository.delete(plan);
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> listAllCoupons() {
        return couponService.listAll();
    }

    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest request) {
        return couponService.create(request);
    }

    @Transactional
    public CouponResponse disableCoupon(UUID couponId) {
        return couponService.disable(couponId);
    }

    @Transactional
    public CouponResponse enableCoupon(UUID couponId) {
        return couponService.enable(couponId);
    }

    @Transactional
    public CouponResponse updateCoupon(UUID couponId, ir.omid.smartteacher.billing.internal.api.dto.UpdateCouponRequest request) {
        return couponService.update(couponId, request);
    }

    @Transactional
    public void deleteCoupon(UUID couponId) {
        couponService.delete(couponId);
    }

    @Transactional
    public AdminSubscriptionResponse adminCancelSubscription(UUID subscriptionId, String reason) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> ResourceNotFoundException.of("اشتراک"));
        sub.setStatus("CANCELLED");
        sub.setCancelledAt(Instant.now());
        sub.setCancellationReason(reason != null ? reason : "لغو توسط ادمین");
        sub.setAutoRenew(false);
        subscriptionRepository.save(sub);
        return toAdminSubscriptionResponse(sub);
    }

    @Transactional
    public AdminSubscriptionResponse adminExtendSubscription(UUID subscriptionId, int days) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> ResourceNotFoundException.of("اشتراک"));
        Instant currentEnd = sub.getEndDate() != null ? sub.getEndDate() : Instant.now();
        sub.setEndDate(currentEnd.plus(days, ChronoUnit.DAYS));
        sub.setStatus("ACTIVE");
        subscriptionRepository.save(sub);
        return toAdminSubscriptionResponse(sub);
    }

    private AdminSubscriptionResponse toAdminSubscriptionResponse(Subscription sub) {
        Plan plan = planRepository.findById(sub.getPlanId()).orElse(null);
        return new AdminSubscriptionResponse(
                sub.getId(),
                sub.getTenantId(),
                sub.getPlanId(),
                plan != null ? plan.getCode() : null,
                plan != null ? plan.getName() : null,
                sub.getStatus(),
                sub.getStartDate(),
                sub.getEndDate(),
                sub.getPricePaid(),
                sub.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<AdminSubscriptionResponse> listAllSubscriptions(int page, int size) {
        return billingPlatformQueries.listSubscriptions(page, size);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse currentSubscription() {
        return subscriptionRepository.findCurrentActive()
                .map(this::toSubscriptionResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public CouponValidateResponse validateCoupon(CouponValidateRequest request) {
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(ResourceNotFoundException::generic);
        var result = couponService.validate(
                request.code(),
                plan.getId(),
                TenantContext.requireTenantId(),
                plan.getPrice());
        return new CouponValidateResponse(result.valid(), result.message(), result.discount());
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoices() {
        return invoiceRepository.findAllByOrderByIssuedAtDesc().stream()
                .map(inv -> new InvoiceResponse(
                        inv.getId(), inv.getInvoiceNumber(), inv.getSubscriptionId(),
                        inv.getSubtotal(), inv.getDiscount(), inv.getTotal(),
                        inv.getCurrency(), inv.getStatus(), inv.getIssuedAt()))
                .toList();
    }

    /**
     * خرید: PENDING → initiate → (Mock) callback اتمیک فعال‌سازی.
     */
    @Transactional
    public SubscriptionResponse checkout(CheckoutRequest request) {
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(ResourceNotFoundException::generic);
        if (!plan.isActive()) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "این پلن غیرفعال است.");
        }

        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requirePrincipal().userId();

        BigDecimal subtotal = plan.getPrice();
        BigDecimal discount = BigDecimal.ZERO;
        CouponService.CouponValidation couponValidation = null;
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            couponValidation = couponService.validate(
                    request.couponCode(), plan.getId(), tenantId, subtotal);
            if (!couponValidation.valid()) {
                throw new AppException(ErrorCode.COUPON_INVALID, couponValidation.message());
            }
            discount = couponValidation.discount();
        }
        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);

        Subscription subscription = new Subscription(plan.getId());
        subscription.setPurchasedByUserId(userId);
        subscription.setStatus("PENDING");
        subscription.setPricePaid(total);
        subscription.setDiscountAmount(discount);
        subscription = subscriptionRepository.save(subscription);

        String invoiceNumber = invoiceNumberService.nextInvoiceNumber();
        Invoice invoice = new Invoice(invoiceNumber);
        invoice.setSubscriptionId(subscription.getId());
        invoice.setSubtotal(subtotal);
        invoice.setDiscount(discount);
        invoice.setTotal(total);
        invoice.setCurrency(plan.getCurrency());
        invoice.setStatus("ISSUED");
        invoice.setIssuedAt(Instant.now());
        invoice = invoiceRepository.save(invoice);

        if (couponValidation != null && couponValidation.valid()) {
            couponService.redeemAtomically(
                    couponValidation.coupon(), tenantId, userId,
                    subscription.getId(), invoice.getId(), discount);
        }

        PaymentGateway.PaymentInitiation initiation = paymentGateway.initiate(
                new PaymentGateway.PaymentInitiationRequest(
                        subscription.getId(),
                        total,
                        plan.getCurrency(),
                        "اشتراک " + plan.getName(),
                        null,
                        Map.of(
                                "subscriptionId", subscription.getId().toString(),
                                "tenantId", tenantId.toString(),
                                "invoiceId", invoice.getId().toString())));

        // Mock: callback فوری؛ درگاه واقعی بعداً از webhook می‌آید
        return processPaymentCallback(initiation.authority());
    }

    /**
     * Callback پرداخت — idempotent بر اساس authority یکتا.
     */
    @Transactional
    public SubscriptionResponse processPaymentCallback(String authority) {
        if (authority == null || authority.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "authority الزامی است.");
        }

        var existing = callbackReceiptRepository.findByAuthority(authority);
        if (existing.isPresent()) {
            PaymentCallbackReceipt receipt = existing.get();
            Subscription sub = subscriptionRepository.findById(receipt.getSubscriptionId())
                    .orElseThrow(ResourceNotFoundException::generic);
            return toSubscriptionResponse(sub, authority);
        }

        UUID subscriptionId;
        String referenceCode;
        if (paymentGateway instanceof MockPaymentGateway mock) {
            PaymentGateway.PaymentInitiationRequest pending = mock.peek(authority);
            if (pending == null) {
                throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR, "authority نامعتبر");
            }
            // paymentId در Mock همان subscriptionId است (checkout)
            subscriptionId = pending.paymentId();
            referenceCode = "REF-MOCK-" + authority.substring(0, Math.min(8, authority.length()));
            mock.consumeAuthority(authority);
        } else {
            PaymentGateway.PaymentVerification verification =
                    paymentGateway.verify(new PaymentGateway.PaymentVerificationRequest(
                            authority, BigDecimal.ZERO, Map.of()));
            if (!verification.successful()) {
                throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR,
                        verification.failureReason() != null
                                ? verification.failureReason() : "پرداخت نامعتبر");
            }
            // درگاه واقعی باید subscriptionId را در metadata برگرداند — فعلاً Mock محور
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR,
                    "درگاه واقعی هنوز پیاده‌سازی نشده است.");
        }

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(ResourceNotFoundException::generic);
        Plan plan = planRepository.findById(subscription.getPlanId())
                .orElseThrow(ResourceNotFoundException::generic);
        Invoice invoice = invoiceRepository.findBySubscriptionId(subscriptionId)
                .orElseThrow(ResourceNotFoundException::generic);

        return activateAfterPayment(subscription, invoice, plan, authority, referenceCode);
    }

    @Transactional
    public SubscriptionResponse cancelCurrent(CancelSubscriptionRequest request) {
        Subscription sub = subscriptionRepository.findCurrentActive()
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_CONFLICT,
                        "اشتراک فعالی برای انصراف وجود ندارد."));
        if (!"ACTIVE".equals(sub.getStatus()) && !"PENDING".equals(sub.getStatus())) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "این اشتراک قابل انصراف نیست.");
        }
        sub.setStatus("CANCELLED");
        sub.setCancelledAt(Instant.now());
        sub.setCancellationReason(request != null ? request.reason() : null);
        sub.setAutoRenew(false);
        return toSubscriptionResponse(sub);
    }

    private SubscriptionResponse activateAfterPayment(Subscription subscription,
                                                      Invoice invoice,
                                                      Plan plan,
                                                      String authority,
                                                      String referenceCode) {
        // idempotent insert
        if (callbackReceiptRepository.existsByAuthority(authority)) {
            return toSubscriptionResponse(subscription, authority);
        }

        Instant start = Instant.now();
        Instant end = switch (plan.getBillingPeriod()) {
            case "YEARLY" -> start.plus(365, ChronoUnit.DAYS);
            case "QUARTERLY" -> start.plus(90, ChronoUnit.DAYS);
            case "LIFETIME" -> null;
            default -> start.plus(30, ChronoUnit.DAYS);
        };

        subscription.setStatus("ACTIVE");
        subscription.setStartDate(start);
        subscription.setEndDate(end);
        invoice.setStatus("PAID");

        PaymentCallbackReceipt receipt = new PaymentCallbackReceipt(
                authority, paymentGateway.code(),
                subscription.getId(), TenantContext.requireTenantId());
        receipt.setReferenceCode(referenceCode);
        try {
            callbackReceiptRepository.saveAndFlush(receipt);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // مسابقه همزمان — برندهٔ دیگر رسید را ساخته
            return toSubscriptionResponse(
                    subscriptionRepository.findById(subscription.getId()).orElse(subscription),
                    authority);
        }

        return new SubscriptionResponse(
                subscription.getId(),
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                subscription.getStatus(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getPricePaid(),
                invoice.getInvoiceNumber(),
                authority,
                null);
    }

    private PlanResponse toPlanResponse(Plan plan) {
        return new PlanResponse(
                plan.getId(), plan.getCode(), plan.getName(), plan.getDescription(),
                plan.getPrice(), plan.getCurrency(), plan.getBillingPeriod(),
                plan.getTrialDays(), plan.isPublic(), plan.isDefault(),
                plan.getSortOrder(), plan.isActive());
    }

    private SubscriptionResponse toSubscriptionResponse(Subscription sub) {
        return toSubscriptionResponse(sub, null);
    }

    private SubscriptionResponse toSubscriptionResponse(Subscription sub, String authority) {
        Plan plan = planRepository.findById(sub.getPlanId()).orElse(null);
        Invoice invoice = invoiceRepository.findBySubscriptionId(sub.getId()).orElse(null);
        return new SubscriptionResponse(
                sub.getId(),
                sub.getPlanId(),
                plan != null ? plan.getCode() : null,
                plan != null ? plan.getName() : null,
                sub.getStatus(),
                sub.getStartDate(),
                sub.getEndDate(),
                sub.getPricePaid(),
                invoice != null ? invoice.getInvoiceNumber() : null,
                authority,
                null);
    }
}
