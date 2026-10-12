package ir.omid.smartteacher.tenant;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * هویت جاری درخواست — «چه کسی، در کدام مستأجر، با چه نقشی».
 *
 * <p>این شیء <b>فقط</b> از توکن JWT ساخته می‌شود و در کل چرخه پردازش
 * درخواست تغییرناپذیر می‌ماند. هیچ‌گاه از بدنه درخواست، پارامتر URL یا
 * هدر خوانده نمی‌شود: اگر مهاجم بتواند مستأجر جاری را از ورودی تعیین
 * کند، کل مدل جداسازی بی‌معنا می‌شود (تهدید T11 در
 * {@code docs/security.md}).
 *
 * @param userId     شناسه کاربر؛ برای {@link #platform()} می‌تواند
 *                   {@code null} باشد (عملیات سیستمی)
 * @param tenantId   شناسه مستأجر جاری؛ {@code null} یعنی «دامنه پلتفرم» —
 *                   نه «همه مستأجرها». تفاوتش حیاتی است: {@code null}
 *                   به‌معنای دسترسی به داده‌ی هیچ مستأجری نیست، نه
 *                   دسترسی به همه.
 * @param planId     شناسه پلن جاری، مطابق claim {@code plan_id}. این
 *                   مقدار <b>فقط یک راهنماست</b> و ممکن است تا ۱۵ دقیقه
 *                   کهنه باشد؛ تصمیم نهایی درباره دسترسی به Feature
 *                   باید در هر درخواست از سرویس اشتراک گرفته شود
 *                   (تهدید T6).
 * @param roles      کدهای نقش سطح پلتفرم از {@code user_roles}
 *                   (مثل {@code SUPER_ADMIN}، {@code TEACHER})
 * @param tenantRole نقش کاربر <i>داخل</i> مستأجر جاری؛ برای کاربر
 *                   پلتفرمی {@code null} است
 * @param locale     زبان ترجیحی کاربر برای پیام‌ها
 */
public record TenantPrincipal(
        UUID userId,
        UUID tenantId,
        UUID planId,
        Set<String> roles,
        MemberRole tenantRole,
        String locale
) {

    /** زبان پیش‌فرض سامانه. */
    public static final String DEFAULT_LOCALE = "fa";

    public TenantPrincipal {
        roles = (roles == null) ? Set.of() : Collections.unmodifiableSet(Set.copyOf(roles));
        locale = (locale == null || locale.isBlank()) ? DEFAULT_LOCALE : locale;
    }

    /**
     * کاربر ناشناس — بدون ورود. برای مسیرهای عمومی مثل ورود و وب‌هوک.
     *
     * <p>هیچ دسترسی‌ای به داده مستأجر نمی‌دهد.
     */
    public static TenantPrincipal anonymous() {
        return new TenantPrincipal(null, null, null, Set.of(), null, DEFAULT_LOCALE);
    }

    /**
     * زمینه سیستمی سطح پلتفرم — بدون کاربر و بدون مستأجر.
     *
     * <p>برای Job‌های زمان‌بندی‌شده، مهاجرت داده و وب‌هوک درگاه پرداخت.
     * یکی از سه مسیر مجاز عبور از جداسازی است.
     *
     * <p><b>⚠️ چرا متد کمکی {@code runAsSystem} نداریم؟</b> عمدی است.
     * با نبودنش، هر عبور از جداسازی در کد به‌شکل صریح
     * {@code TenantPrincipal.platform()} نوشته می‌شود و با یک
     * {@code grep} می‌توان <i>همه</i> نقاط عبور را شمرد و بازبینی کرد.
     * متد کمکی این نقاط را پنهان می‌کرد.
     */
    public static TenantPrincipal platform() {
        return new TenantPrincipal(null, null, null, Set.of(), null, DEFAULT_LOCALE);
    }

    /** کاربر واردشده با نقش‌های پلتفرمی، بدون زمینه مستأجر. */
    public static TenantPrincipal ofPlatformUser(UUID userId, Set<String> roles, String locale) {
        return new TenantPrincipal(userId, null, null, roles, null, locale);
    }

    /** کاربر واردشده در یک مستأجر مشخص — حالت عادی Mini App. */
    public static TenantPrincipal ofTenantUser(UUID userId,
                                               UUID tenantId,
                                               UUID planId,
                                               Set<String> roles,
                                               MemberRole tenantRole,
                                               String locale) {
        return new TenantPrincipal(userId, tenantId, planId, roles, tenantRole, locale);
    }

    /** آیا مستأجری در زمینه جاری هست؟ */
    public boolean hasTenant() {
        return tenantId != null;
    }

    /** آیا کاربر احراز هویت شده است؟ */
    public boolean isAuthenticated() {
        return userId != null;
    }

    /** آیا کاربر نقش سوپر‌ادمین پلتفرم را دارد؟ */
    public boolean isSuperAdmin() {
        return roles.contains("SUPER_ADMIN");
    }

    /**
     * بررسی نقش سطح پلتفرم.
     *
     * @param roleCode کد نقش، مثل {@code ADMIN}
     */
    public boolean hasRole(String roleCode) {
        return roleCode != null && roles.contains(roleCode);
    }

    /**
     * بررسی نقش داخل مستأجر جاری.
     *
     * @param role نقش مورد انتظار
     */
    public boolean hasTenantRole(MemberRole role) {
        return Objects.equals(tenantRole, role);
    }
}
