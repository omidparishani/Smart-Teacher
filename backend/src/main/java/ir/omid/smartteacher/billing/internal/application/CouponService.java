package ir.omid.smartteacher.billing.internal.application;

import ir.omid.smartteacher.billing.internal.api.dto.CouponResponse;
import ir.omid.smartteacher.billing.internal.api.dto.CreateCouponRequest;
import ir.omid.smartteacher.billing.internal.domain.Coupon;
import ir.omid.smartteacher.billing.internal.domain.CouponRedemption;
import ir.omid.smartteacher.billing.internal.domain.CouponRedemptionRepository;
import ir.omid.smartteacher.billing.internal.domain.CouponRepository;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * اعتبارسنجی و ثبت اتمیک کوپن.
 */
@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponRedemptionRepository redemptionRepository;

    public CouponService(CouponRepository couponRepository,
                         CouponRedemptionRepository redemptionRepository) {
        this.couponRepository = couponRepository;
        this.redemptionRepository = redemptionRepository;
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> listAll() {
        return couponRepository.findAllByOrderByCodeAsc().stream()
                .map(CouponService::toResponse)
                .toList();
    }

    @Transactional
    public CouponResponse create(CreateCouponRequest request) {
        String code = request.code().trim().toUpperCase();
        if (couponRepository.findByCodeIgnoreCase(code).isPresent()) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "کد کوپن تکراری است.");
        }
        if (!request.validUntil().isAfter(request.validFrom())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "validUntil باید بعد از validFrom باشد.");
        }
        String discountType = request.discountType().trim().toUpperCase();
        if (!"PERCENTAGE".equals(discountType) && !"FIXED".equals(discountType)) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "discountType باید PERCENTAGE یا FIXED باشد.");
        }
        Coupon coupon = new Coupon(code, discountType, request.discountValue(),
                request.validFrom(), request.validUntil());
        coupon.setMaxDiscountAmount(request.maxDiscountAmount());
        coupon.setMinPurchaseAmount(request.minPurchaseAmount());
        coupon.setUsageLimit(request.usageLimit());
        coupon.setUsageLimitPerTenant(request.usageLimitPerTenant());
        coupon.setApplicablePlanId(request.applicablePlanId());
        coupon.setActive(true);
        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse disable(UUID couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(ResourceNotFoundException::generic);
        coupon.setActive(false);
        return toResponse(coupon);
    }

    @Transactional
    public CouponResponse enable(UUID couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(ResourceNotFoundException::generic);
        coupon.setActive(true);
        return toResponse(coupon);
    }

    @Transactional
    public CouponResponse update(UUID couponId, ir.omid.smartteacher.billing.internal.api.dto.UpdateCouponRequest request) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(ResourceNotFoundException::generic);
        if (request.discountValue() != null) {
            coupon.setDiscountValue(request.discountValue());
        }
        if (request.validFrom() != null) {
            coupon.setValidFrom(request.validFrom());
        }
        if (request.validUntil() != null) {
            coupon.setValidUntil(request.validUntil());
        }
        if (request.maxDiscountAmount() != null) {
            coupon.setMaxDiscountAmount(request.maxDiscountAmount());
        }
        if (request.minPurchaseAmount() != null) {
            coupon.setMinPurchaseAmount(request.minPurchaseAmount());
        }
        if (request.usageLimit() != null) {
            coupon.setUsageLimit(request.usageLimit());
        }
        if (request.usageLimitPerTenant() != null) {
            coupon.setUsageLimitPerTenant(request.usageLimitPerTenant());
        }
        if (request.applicablePlanId() != null) {
            coupon.setApplicablePlanId(request.applicablePlanId());
        }
        if (request.active() != null) {
            coupon.setActive(request.active());
        }
        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public void delete(UUID couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(ResourceNotFoundException::generic);
        couponRepository.delete(coupon);
    }

    @Transactional(readOnly = true)
    public CouponValidation validate(String rawCode, UUID planId, UUID tenantId, BigDecimal amount) {
        if (rawCode == null || rawCode.isBlank()) {
            return CouponValidation.invalid("کد تخفیف خالی است.");
        }
        Coupon coupon = couponRepository.findByCodeIgnoreCase(rawCode.trim())
                .orElse(null);
        if (coupon == null) {
            return CouponValidation.invalid("کد تخفیف یافت نشد.");
        }
        if (!coupon.isActive()) {
            return CouponValidation.invalid("کد تخفیف غیرفعال است.");
        }
        Instant now = Instant.now();
        if (now.isBefore(coupon.getValidFrom())) {
            return CouponValidation.invalid("کد تخفیف هنوز فعال نشده است.");
        }
        if (now.isAfter(coupon.getValidUntil())) {
            return CouponValidation.invalid("کد تخفیف منقضی شده است.");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return CouponValidation.invalid("ظرفیت استفاده از این کد تکمیل شده است.");
        }
        if (coupon.getApplicablePlanId() != null
                && !coupon.getApplicablePlanId().equals(planId)) {
            return CouponValidation.invalid("این کد برای پلن انتخابی معتبر نیست.");
        }
        if (amount.compareTo(coupon.getMinPurchaseAmount()) < 0) {
            return CouponValidation.invalid("حداقل مبلغ خرید رعایت نشده است.");
        }
        long usedByTenant = redemptionRepository.countByCouponAndTenant(coupon.getId(), tenantId);
        if (coupon.getUsageLimitPerTenant() != null
                && usedByTenant >= coupon.getUsageLimitPerTenant()) {
            return CouponValidation.invalid("شما قبلاً از این کد استفاده کرده‌اید.");
        }
        return CouponValidation.valid(coupon, calculateDiscount(coupon, amount));
    }

    /**
     * ثبت اتمیک: افزایش used_count سپس redemption.
     *
     * @throws AppException اگر ظرفیت همزمان پر شود
     */
    @Transactional
    public CouponRedemption redeemAtomically(Coupon coupon, UUID tenantId, UUID userId,
                                             UUID subscriptionId, UUID invoiceId,
                                             BigDecimal discount) {
        int updated = couponRepository.incrementUsedCountAtomically(coupon.getId());
        if (updated == 0) {
            throw new AppException(ErrorCode.COUPON_INVALID,
                    "ظرفیت استفاده از این کد تکمیل شده است.");
        }
        CouponRedemption redemption = new CouponRedemption(coupon.getId(), tenantId, discount);
        redemption.setUserId(userId);
        redemption.setSubscriptionId(subscriptionId);
        redemption.setInvoiceId(invoiceId);
        return redemptionRepository.save(redemption);
    }

    private static BigDecimal calculateDiscount(Coupon coupon, BigDecimal amount) {
        BigDecimal discount = switch (coupon.getDiscountType()) {
            case "PERCENTAGE" -> amount.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            case "FIXED" -> coupon.getDiscountValue();
            default -> BigDecimal.ZERO;
        };
        if (coupon.getMaxDiscountAmount() != null) {
            discount = discount.min(coupon.getMaxDiscountAmount());
        }
        return discount.min(amount).max(BigDecimal.ZERO);
    }

    static CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMaxDiscountAmount(),
                coupon.getMinPurchaseAmount(),
                coupon.getUsageLimit(),
                coupon.getUsageLimitPerTenant(),
                coupon.getUsedCount(),
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                coupon.getApplicablePlanId(),
                coupon.isActive());
    }

    public record CouponValidation(boolean valid, String message, Coupon coupon, BigDecimal discount) {
        public static CouponValidation invalid(String message) {
            return new CouponValidation(false, message, null, BigDecimal.ZERO);
        }

        public static CouponValidation valid(Coupon coupon, BigDecimal discount) {
            return new CouponValidation(true, null, coupon, discount);
        }
    }
}
