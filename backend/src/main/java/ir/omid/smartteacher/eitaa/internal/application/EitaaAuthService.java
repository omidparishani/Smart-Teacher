package ir.omid.smartteacher.eitaa.internal.application;

import ir.omid.smartteacher.audit.ActorType;
import ir.omid.smartteacher.common.error.EitaaInitDataException;
import ir.omid.smartteacher.common.error.InvalidTokenException;
import ir.omid.smartteacher.eitaa.EitaaInitData;
import ir.omid.smartteacher.eitaa.EitaaInitDataValidator;
import ir.omid.smartteacher.eitaa.internal.api.dto.AuthResponse;
import ir.omid.smartteacher.eitaa.internal.api.dto.AuthSubscriptionView;
import ir.omid.smartteacher.eitaa.internal.api.dto.AuthTenantView;
import ir.omid.smartteacher.eitaa.internal.api.dto.AuthUserView;
import ir.omid.smartteacher.eitaa.internal.api.dto.CurrentUserResponse;
import ir.omid.smartteacher.security.audit.SecurityAuditLog;
import ir.omid.smartteacher.security.audit.SecurityEvent;
import ir.omid.smartteacher.security.jwt.AuthTokenService;
import ir.omid.smartteacher.security.jwt.TokenPair;
import ir.omid.smartteacher.security.jwt.TokenScope;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.Tenant;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import ir.omid.smartteacher.user.User;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * منطق احراز هویت برنامک ایتا — ورود، پروفایل، تازه‌سازی، خروج.
 *
 * <p>این کلاس هماهنگ‌کننده است: اعتبارسنجی {@code initData}، ساخت حساب،
 * و مکانیک توکن هر کدام کلاس خودشان را دارند. اینجا فقط ترتیب و تصمیم
 * درست انجام می‌شود.
 *
 * <p><b>🔴 چرا در ماژول {@code eitaa} است و نه یک ماژول {@code auth}
 * جدا؟</b> چون {@code docs/architecture.md} بخش ۴ ماژول {@code eitaa} را
 * این‌طور تعریف کرده: «اتصال به ایتا (Adapter دو-مسیره، <b>Mini App
 * Auth</b>، initData)». ساختن ماژولی که در معماری مصوب نیست، یعنی یک
 * موجودیت جدید که هیچ‌کدام از اسناد از آن خبر ندارند.
 */
@Component
public class EitaaAuthService {

    private static final Logger log = LoggerFactory.getLogger(EitaaAuthService.class);

    /**
     * نقش‌های پلتفرمی یک کاربر برنامک.
     *
     * <p>خالی است و باید هم خالی بماند: نقش‌های پلتفرمی مثل
     * {@code SUPER_ADMIN} برای پنل مدیریت‌اند، نه برای معلم و دانش‌آموز.
     * نقش واقعی کاربر در <b>مستأجر</b> است و در {@code tenantRole} می‌آید.
     */
    private static final Set<String> NO_PLATFORM_ROLES = Set.of();

    private final EitaaInitDataValidator initDataValidator;
    private final EitaaAccountProvisioner provisioner;
    private final AuthTokenService tokenService;
    private final SecurityAuditLog securityAuditLog;

    public EitaaAuthService(EitaaInitDataValidator initDataValidator,
                            EitaaAccountProvisioner provisioner,
                            AuthTokenService tokenService,
                            SecurityAuditLog securityAuditLog) {
        this.initDataValidator = initDataValidator;
        this.provisioner = provisioner;
        this.tokenService = tokenService;
        this.securityAuditLog = securityAuditLog;
    }

    // ─────────────────────────── ورود ───────────────────────────

    /**
     * ورود با {@code initData} ایتا.
     *
     * <p>اگر کاربر تازه باشد، حساب و مستأجر شخصی‌اش ساخته می‌شود.
     *
     * @param rawInitData رشته خام از برنامک
     * @param clientInfo  اطلاعات کلاینت
     * @return توکن‌ها و پروفایل
     */
    @Transactional
    public AuthResponse login(String rawInitData, AuthTokenService.ClientInfo clientInfo) {
        EitaaInitData initData = validateInitData(rawInitData);

        EitaaAccountProvisioner.ProvisionedAccount account =
                provisioner.findOrCreate(initData.user());

        UUID userId = account.user().getId();
        UUID tenantId = (account.tenant() == null) ? null : account.tenant().getId();

        TenantPrincipal principal = principalFor(
                userId, tenantId, account.tenantRole(), account.user().getPreferredLocale());

        AuthTokenService.IssuedTokens issued = tokenService.issue(
                principal,
                AuthTokenService.RefreshSession.newSession(userId, tenantId),
                TokenScope.MINIAPP,
                clientInfo);

        log.debug("ورود موفق کاربر {} (کاربر تازه: {})", userId, account.newlyCreated());

        return buildResponse(
                issued.pair(),
                account.user(),
                account.tenant(),
                account.tenantRole(),
                account.newlyCreated());
    }

    /**
     * اعتبارسنجی {@code initData}، با ثبت رویداد امنیتی در شکست.
     *
     * <p><b>⚠️ چرا ثبت رویداد اینجاست و نه در
     * {@link EitaaInitDataValidator}؟</b> چون آن کلاس در ماژول
     * {@code eitaa} است و ماژول امنیت به آن وابسته نیست. اگر اعتبارسنج
     * خودش Audit می‌نوشت، وابستگی {@code eitaa → security.audit} را
     * برعکس می‌کرد. پس اعتبارسنج خالص می‌ماند و ثبت رویداد اینجا
     * انجام می‌شود.
     *
     * <p>⚠️ در {@code metadata} فقط <i>نوع</i> خطا می‌آید، نه خود
     * {@code initData} — که یک راز است.
     */
    private EitaaInitData validateInitData(String rawInitData) {
        try {
            return initDataValidator.validate(rawInitData);
        } catch (EitaaInitDataException e) {
            securityAuditLog.record(
                    SecurityEvent.INVALID_INIT_DATA,
                    null,
                    null,
                    ActorType.API,
                    "EitaaInitData",
                    null,
                    Map.of("reason", e.getClass().getSimpleName()));
            throw e;
        }
    }

    // ─────────────────────────── پروفایل ───────────────────────────

    /**
     * پروفایل کاربر واردشده.
     *
     * <p><b>⚠️ چرا از دیتابیس می‌خواند و به توکن تکیه نمی‌کند؟</b> چون
     * توکن تا ۱۵ دقیقه معتبر است. اگر معلمی از مدرسه حذف شود، تا آن
     * زمان توکنش هنوز می‌گوید عضو است. خواندن از دیتابیس یعنی پاسخ
     * «الآن چه کسی هستی» درست است، نه «۱۵ دقیقه پیش چه کسی بودی».
     */
    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser() {
        TenantPrincipal principal = TenantContext.requirePrincipal();
        UUID userId = principal.userId();

        if (userId == null) {
            // ⚠️ نمی‌تواند رخ دهد: مسیر با anyRequest().authenticated()
            // محافظت شده و فیلتر، هویت ناشناس را با ۴۰۱ رد می‌کند. اگر
            // اینجا برسیم یعنی پیکربندی امنیتی عوض شده — پس صریح شکست
            // می‌خواهیم، نه یک پاسخ خالی گمراه‌کننده.
            throw InvalidTokenException.malformed("درخواست بدون هویت به مسیر محافظت‌شده رسید");
        }

        User user = provisioner.requireUsableUser(userId);

        UUID tenantId = principal.tenantId();
        EitaaAccountProvisioner.TenantMembership membership =
                provisioner.findMembership(tenantId, userId).orElse(null);

        if (tenantId != null && membership == null) {
            // عضویت باطل شده ولی توکن هنوز معتبر است. کاربر همچنان
            // احراز هویت شده است، ولی مستأجری ندارد.
            log.info("کاربر {} توکن معتبر دارد ولی عضویتش در مستأجر {} معتبر نیست",
                    userId, tenantId);
        }

        return new CurrentUserResponse(
                userView(user, (membership == null) ? null : membership.role()),
                (membership == null) ? null : tenantView(membership.tenant()),
                subscriptionView());
    }

    // ─────────────────────────── تازه‌سازی ───────────────────────────

    /**
     * چرخش توکن تازه‌سازی.
     *
     * <p>توکن قبلی باطل و توکن تازه‌ای در همان خانواده صادر می‌شود. اگر
     * توکن ورودی قبلاً مصرف شده باشد، سرقت تشخیص داده می‌شود.
     *
     * @param rawRefreshToken توکن خام کلاینت
     * @param clientInfo      اطلاعات کلاینت
     * @return توکن‌های تازه
     */
    @Transactional
    public AuthResponse refresh(String rawRefreshToken, AuthTokenService.ClientInfo clientInfo) {
        AuthTokenService.ConsumedRefresh consumed = tokenService.consume(rawRefreshToken);

        UUID userId = consumed.session().userId();
        UUID tenantId = consumed.session().tenantId();

        // ⚠️ حساب ممکن است در این فاصله غیرفعال شده باشد. بدون این
        // بررسی، یک حساب مسدودشده می‌توانست تا ۳۰ روز با تازه‌کردن
        // توکن، به کارش ادامه دهد.
        User user = provisioner.requireUsableUser(userId);

        // 🔴 عضویت دوباره بررسی می‌شود. نقش <i>هرگز</i> از توکن خوانده
        // نمی‌شود: اگر کاربری در این فاصله از مدیر به دانش‌آموز تغییر
        // کرده باشد، توکن قدیمی نقش قدیمی را یدک می‌کشد.
        EitaaAccountProvisioner.TenantMembership membership =
                provisioner.findMembership(tenantId, userId).orElse(null);

        if (tenantId != null && membership == null) {
            securityAuditLog.record(
                    SecurityEvent.ACCESS_DENIED,
                    tenantId,
                    userId,
                    ActorType.USER,
                    "RefreshToken",
                    consumed.tokenId(),
                    Map.of("reason", "عضویت در مستأجر معتبر نیست"));
            throw InvalidTokenException.revoked("عضویت در این مستأجر دیگر معتبر نیست");
        }

        Tenant tenant = (membership == null) ? null : membership.tenant();
        MemberRole role = (membership == null) ? null : membership.role();

        TenantPrincipal principal = principalFor(
                userId, tenantId, role, user.getPreferredLocale());

        AuthTokenService.IssuedTokens issued = tokenService.issue(
                principal,
                // همان خانواده، با پیوند به توکن مصرف‌شده
                consumed.session().next(consumed.tokenId()),
                consumed.scope(),
                clientInfo);

        return buildResponse(issued.pair(), user, tenant, role, false);
    }

    // ─────────────────────────── خروج ───────────────────────────

    /**
     * خروج — ابطال کل خانواده توکن.
     *
     * <p><b>⚠️ همیشه موفق است، حتی با توکن ناشناخته.</b> اگر خطا
     * برمی‌گرداندیم، کلاینت ممکن است توکن را پاک نکند و کاربر فکر کند
     * هنوز وارد است. خروجی که گاهی شکست بخورد، از خروجی که همیشه کار
     * می‌کند بدتر است.
     *
     * <p><b>⚠️ چرا رویداد امنیتی ثبت نمی‌شود؟</b> چون مجموعه
     * {@code SecurityEvent} هیچ رویداد «خروج» ندارد. ثبتش زیر
     * {@code TOKEN_FAMILY_REVOKED} غلط می‌بود — آن رویداد مخصوص ابطال
     * <i>پس از تشخیص سرقت</i> است و قاطی‌کردنشان هشدارهای امنیتی واقعی
     * را در نویز گم می‌کند. افزودن رویداد تازه به مجموعه، تصمیم فاز ۱۲
     * است.
     *
     * @param rawRefreshToken توکن خام کلاینت
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        int revoked = tokenService.revokeFamily(rawRefreshToken, "خروج کاربر");
        log.debug("خروج انجام شد؛ {} توکن باطل شد", revoked);
    }

    // ─────────────────────────── ساخت پاسخ ───────────────────────────

    /**
     * ساخت هویت جاری از شناسه‌ها.
     *
     * <p>نقش مستأجر به {@code roles} پاسخ اضافه می‌شود تا کلاینت بتواند
     * رابط کاربری را درست بچیند (معلم در برابر دانش‌آموز). این نقش در
     * توکن هم به‌صورت جداگانه ({@code tenantRole}) می‌آید؛ اینجا فقط
     * برای راحتی کلاینت تکرار می‌شود.
     */
    private static TenantPrincipal principalFor(UUID userId,
                                                UUID tenantId,
                                                MemberRole tenantRole,
                                                String locale) {
        if (tenantId == null || tenantRole == null) {
            // کاربر احراز هویت شده ولی بدون مستأجر. تنها حالت واقعی‌اش
            // عضویت باطل‌شده است؛ مسیرهای نیازمند مستأجر بعداً با
            // TenantGuard رد می‌شوند.
            return TenantPrincipal.ofPlatformUser(userId, NO_PLATFORM_ROLES, locale);
        }

        return TenantPrincipal.ofTenantUser(
                userId,
                tenantId,
                // ⚠️ planId در فاز ۲ همیشه null است — جدول پلن‌ها و
                // اشتراک‌ها در فاز ۱۰ ساخته می‌شود. مقدار جعلی اینجا یعنی
                // توکنی که ادعای پلنی می‌کند که وجود ندارد.
                null,
                NO_PLATFORM_ROLES,
                tenantRole,
                locale);
    }

    private static AuthResponse buildResponse(TokenPair pair,
                                              User user,
                                              Tenant tenant,
                                              MemberRole tenantRole,
                                              boolean isNewUser) {
        return new AuthResponse(
                pair.accessToken(),
                pair.refreshToken(),
                pair.tokenType(),
                pair.expiresIn(),
                pair.refreshExpiresIn(),
                userView(user, tenantRole),
                (tenant == null) ? null : tenantView(tenant),
                subscriptionView(),
                isNewUser);
    }

    private static AuthUserView userView(User user, MemberRole tenantRole) {
        Set<String> roles = new LinkedHashSet<>(NO_PLATFORM_ROLES);
        if (tenantRole != null) {
            roles.add(tenantRole.name());
        }
        return new AuthUserView(user.getId(), user.getFullName(), Set.copyOf(roles));
    }

    private static AuthTenantView tenantView(Tenant tenant) {
        return new AuthTenantView(
                tenant.getId(), tenant.getName(), tenant.getType().name());
    }

    /**
     * اشتراک — در فاز ۲ همیشه {@code null}.
     *
     * <p>دلیل کاملش در {@link AuthSubscriptionView} آمده: جدول‌های اشتراک
     * در فاز ۱۰ ساخته می‌شوند و جعل پلن {@code FREE} یعنی ادعایی که
     * پشتوانه‌ای در دیتابیس ندارد.
     */
    private static AuthSubscriptionView subscriptionView() {
        return null;
    }
}
