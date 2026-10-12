package ir.omid.smartteacher.eitaa.internal.application;

import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.eitaa.EitaaIdentity;
import ir.omid.smartteacher.eitaa.EitaaIdentityRepository;
import ir.omid.smartteacher.eitaa.EitaaUserData;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.Tenant;
import ir.omid.smartteacher.tenant.TenantMember;
import ir.omid.smartteacher.tenant.TenantMemberRepository;
import ir.omid.smartteacher.tenant.TenantRepository;
import ir.omid.smartteacher.tenant.TenantType;
import ir.omid.smartteacher.user.User;
import ir.omid.smartteacher.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ساخت یا یافتن حساب متناظر یک کاربر ایتا.
 *
 * <p>نخستین ورود یک معلم باید <i>بدون هیچ فرمی</i> حساب بسازد. این کلاس
 * همان کار را می‌کند: کاربر، مستأجر شخصی، عضویت مالک، و پیوند ایتا.
 *
 * <p><b>🔴 ترتیب ساخت مهم است و قابل‌جابه‌جایی نیست:</b>
 * {@code User} → {@code Tenant} → {@code TenantMember} →
 * {@code EitaaIdentity}. هر کدام به شناسه قبلی نیاز دارد. اگر
 * {@code User} با {@code save} ذخیره نشود، شناسه‌اش تولید نمی‌شود و
 * بقیه رکوردها به شناسه خالی اشاره می‌کنند.
 *
 * <p><b>⚠️ فقط معلم ساخته می‌شود — و این یک کمبود آگاهانه است.</b>
 * در فاز ۲ هر ورود تازه یک مستأجر {@code TEACHER} می‌سازد، چون ثبت‌نام
 * دانش‌آموز در کلاس به «ثبت‌نام» نیاز دارد و آن در فاز ۵ می‌آید. یعنی
 * اگر دانش‌آموزی <i>الآن</i> برنامک را باز کند، مستأجر شخصی معلمی
 * می‌گیرد که استفاده‌اش نمی‌کند. جایگزینش جعل یک مسیر ثبت‌نام بود که
 * وجود ندارد. این مورد در {@code docs/verification-log.md} ثبت شده است.
 */
@Component
public class EitaaAccountProvisioner {

    private static final Logger log = LoggerFactory.getLogger(EitaaAccountProvisioner.class);

    /**
     * پیشوند نام مستأجر شخصی — مطابق نمونه {@code docs/api.md} بخش ۵:
     * {@code "name": "کلاس‌های علی احمدی"}.
     */
    private static final String PERSONAL_TENANT_PREFIX = "کلاس‌های ";

    /**
     * پیشوند slug مستأجر.
     *
     * <p>slug از شناسه کاربر ساخته می‌شود، نه از نام او: نام فارسی
     * slug قابل‌خوانی نمی‌دهد، و نام ممکن است تکراری باشد. شناسه کاربر
     * یکتاست، پس slug هم یکتاست و نیازی به حلقه «تولید و بررسی تکرار»
     * ندارد.
     *
     * <p>⚠️ این طراحی یک فرض دارد: هر کاربر فقط یک مستأجر شخصی دارد.
     * این فرض با تعریف {@code TenantType.TEACHER} هم‌خوان است
     * («معلم مستقل — یک نفر، بدون زیرمجموعه»).
     */
    private static final String TENANT_SLUG_PREFIX = "t-";

    /** بلندترین نام کاربری ممکن — مطابق طول ستون {@code username}. */
    private static final int MAX_USERNAME_LENGTH = 100;

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final EitaaIdentityRepository eitaaIdentityRepository;

    public EitaaAccountProvisioner(UserRepository userRepository,
                                   TenantRepository tenantRepository,
                                   TenantMemberRepository tenantMemberRepository,
                                   EitaaIdentityRepository eitaaIdentityRepository) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.tenantMemberRepository = tenantMemberRepository;
        this.eitaaIdentityRepository = eitaaIdentityRepository;
    }

    /**
     * حساب متناظر کاربر ایتا را می‌یابد یا می‌سازد.
     *
     * @param userData کاربر از {@code initData} تأییدشده
     * @return حساب و مستأجر فعال
     */
    public ProvisionedAccount findOrCreate(EitaaUserData userData) {
        if (userData == null || userData.id() == null) {
            throw new IllegalArgumentException("داده کاربر ایتا نباید خالی باشد");
        }

        Optional<EitaaIdentity> existing = eitaaIdentityRepository.findByEitaaUserId(userData.id());

        if (existing.isPresent()) {
            return refreshExisting(existing.get(), userData);
        }

        return createNew(userData);
    }

    // ─────────────────────────── مسیر کاربر موجود ───────────────────────────

    /**
     * به‌روزرسانی اطلاعات کاربر موجود و یافتن مستأجر فعالش.
     */
    private ProvisionedAccount refreshExisting(EitaaIdentity identity, EitaaUserData userData) {
        // نام، نام کاربری و دسترسی ارسال ممکن است در ایتا عوض شده باشد.
        identity.refreshFrom(userData);

        User user = userRepository.findById(identity.getUserId())
                .orElseThrow(() -> {
                    // ⚠️ ناسازگاری داده: پیوندی هست که به کاربری اشاره
                    // می‌کند که وجود ندارد. این خطای <i>ما</i> است، نه
                    // کاربر. عمداً ۵۰۰ می‌دهیم و متن فنی را به کاربر
                    // نشان نمی‌دهیم.
                    log.error("پیوند ایتا {} به کاربر ناموجود {} اشاره می‌کند",
                            identity.getId(), identity.getUserId());
                    return new AppException(ErrorCode.INTERNAL_ERROR);
                });

        // ⚠️ اگر حساب غیرفعال باشد، ورود رد می‌شود. بدون این بررسی،
        // یک معلم اخراج‌شده می‌توانست با باز کردن برنامک، دوباره
        // توکن معتبر بگیرد و همه کنترل‌های مدیریتی دور زده شود.
        if (!user.isUsable()) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        TenantMembership active = findActiveMembership(user.getId()).orElse(null);

        if (active == null) {
            log.warn("کاربر {} هیچ عضویت فعالی ندارد — ورود بدون مستأجر ادامه می‌یابد",
                    user.getId());
        }

        return new ProvisionedAccount(
                user,
                (active == null) ? null : active.tenant(),
                (active == null) ? null : active.role(),
                identity,
                false);
    }

    // ─────────────────────────── مسیر کاربر تازه ───────────────────────────

    /**
     * ساخت کاربر، مستأجر شخصی، عضویت مالک و پیوند ایتا.
     *
     * <p>این متد باید در یک تراکنش اجرا شود: اگر نیمه‌کاره شکست بخورد،
     * کاربری بدون مستأجر باقی می‌ماند که هرگز نمی‌تواند کار کند و
     * ورود بعدی‌اش هم به همان حال خراب برمی‌گردد.
     */
    private ProvisionedAccount createNew(EitaaUserData userData) {
        String displayName = displayNameOf(userData);

        // ── ۱) کاربر ──
        User user = new User(displayName);
        user.setUsername(deriveUsername(userData));
        userRepository.save(user);

        // ── ۲) مستأجر شخصی ──
        Tenant tenant = new Tenant(
                TenantType.TEACHER,
                PERSONAL_TENANT_PREFIX + displayName,
                TENANT_SLUG_PREFIX + user.getId(),
                user.getId());
        tenantRepository.save(tenant);

        // ── ۳) عضویت با نقش مالک ──
        // نقش OWNER نه TEACHER: این شخص مالک مستأجر خودش است و باید
        // بتواند همه‌کار کند، از جمله مدیریت اعضای آینده.
        TenantMember membership = new TenantMember(tenant.getId(), user.getId(), MemberRole.OWNER);
        tenantMemberRepository.save(membership);

        // ── ۴) پیوند ایتا ──
        EitaaIdentity identity = new EitaaIdentity(user.getId(), userData);
        eitaaIdentityRepository.save(identity);

        log.info("حساب تازه برای کاربر ایتا {} ساخته شد (مستأجر {})",
                userData.id(), tenant.getId());

        return new ProvisionedAccount(user, tenant, MemberRole.OWNER, identity, true);
    }

    // ─────────────────────────── خواندن‌های عمومی ───────────────────────────

    /**
     * بارگذاری کاربر، با بررسی فعال‌بودن حساب.
     *
     * @param userId شناسه کاربر
     * @return کاربر
     * @throws AppException با کد {@code ACCOUNT_DISABLED} اگر حساب قابل
     *         استفاده نباشد، یا {@code INTERNAL_ERROR} اگر وجود نداشته
     *         باشد
     */
    public User requireUsableUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("کاربر {} یافت نشد — توکن به کاربر ناموجود اشاره می‌کند", userId);
                    return new AppException(ErrorCode.INTERNAL_ERROR);
                });

        if (!user.isUsable()) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        return user;
    }

    /**
     * یافتن مستأجر فعال کاربر همراه با نقشش.
     *
     * <p>فهرست می‌گیریم و اولین عضویت <i>فعال</i> را برمی‌داریم. عضویت
     * معلق یا حذف‌شده مستأجر نمی‌دهد — وگرنه کاربری که دسترسی‌اش قطع
     * شده، با ورود دوباره همان دسترسی را پس می‌گرفت.
     *
     * <p>نقش همراه مستأجر برمی‌گردد چون فراخوان تقریباً همیشه هر دو را
     * لازم دارد و خواندن دوباره عضویت، همان کوئری را تکرار می‌کرد.
     */
    public Optional<TenantMembership> findActiveMembership(UUID userId) {
        List<TenantMember> memberships = tenantMemberRepository.findByUserId(userId);

        for (TenantMember membership : memberships) {
            if (!membership.isEffective()) {
                continue;
            }
            Optional<Tenant> tenant = tenantRepository.findById(membership.getTenantId());
            if (tenant.isPresent() && tenant.get().isOperational()) {
                return Optional.of(new TenantMembership(tenant.get(), membership));
            }
        }

        return Optional.empty();
    }

    /**
     * یافتن مستأجر مشخص، <b>با بررسی عضویت</b>.
     *
     * <p><b>🔴 چرا این متد لازم است؟</b> توکن تازه‌سازی، شناسه مستأجر را
     * در خود دارد. اگر آن شناسه را بدون بررسی قبول کنیم، کاربری که از
     * مدرسه <i>حذف</i> شده تا ۳۰ روز (عمر توکن) دسترسی‌اش را حفظ
     * می‌کند — چون توکن هنوز معتبر است. پس هر بار چرخش، عضویت از نو
     * بررسی می‌شود و نقش هم از دیتابیس خوانده می‌شود، نه از توکن.
     *
     * <p>این همان اصلی است که {@code docs/security.md} بخش ۸ می‌گوید:
     * «شناسه مستأجر هرگز از ورودی کاربر گرفته نمی‌شود» — و توکن هم
     * ورودی کاربر است.
     *
     * @param tenantId مستأجر ادعاشده در توکن
     * @param userId   کاربر
     * @return مستأجر و نقش، اگر عضویت معتبر بود
     */
    public Optional<TenantMembership> findMembership(UUID tenantId, UUID userId) {
        if (tenantId == null || userId == null) {
            return Optional.empty();
        }

        Optional<TenantMember> membership =
                tenantMemberRepository.findByTenantIdAndUserId(tenantId, userId);

        if (membership.isEmpty() || !membership.get().isEffective()) {
            return Optional.empty();
        }

        Optional<Tenant> tenant = tenantRepository.findById(tenantId);

        if (tenant.isEmpty() || !tenant.get().isOperational()) {
            return Optional.empty();
        }

        return Optional.of(new TenantMembership(tenant.get(), membership.get()));
    }

    /**
     * مستأجر و عضویت کاربر در آن.
     *
     * @param tenant     مستأجر
     * @param membership عضویت — نقش از اینجا می‌آید
     */
    public record TenantMembership(Tenant tenant, TenantMember membership) {

        /** نقش کاربر در این مستأجر. */
        public MemberRole role() {
            return membership.getRole();
        }
    }

    // ─────────────────────────── کمکی ───────────────────────────

    /**
     * نام نمایشی کاربر.
     *
     * <p>همیشه مقداری برمی‌گرداند: ستون {@code full_name} نمی‌تواند خالی
     * باشد و کاربر ایتا هم ممکن است هیچ نامی نداشته باشد. در آن حالت
     * شناسه‌اش می‌آید تا رکورد ساخته شود و در فهرست‌ها قابل‌تشخیص باشد.
     */
    private static String displayNameOf(EitaaUserData userData) {
        String first = userData.firstName();
        String last = userData.lastName();

        if (first != null && !first.isBlank()) {
            return (last == null || last.isBlank()) ? first : first + " " + last;
        }
        if (userData.username() != null && !userData.username().isBlank()) {
            return userData.username();
        }
        return "کاربر ایتا " + userData.id();
    }

    /**
     * نام کاربری داخلی از نام کاربری ایتا.
     *
     * <p>⚠️ ممکن است {@code null} باشد و این مجاز است — ستون
     * {@code username} خالی‌پذیر است. عمداً چیزی از خودمان نمی‌سازیم
     * (مثل {@code user12345}): نام کاربری ساختگی یعنی کاربر بعداً
     * نمی‌تواند حدس بزند با چه نامی ثبت شده، و اگر روزی ورود با نام
     * کاربری اضافه شود، یک نام کاربری بی‌معنا در حسابش مانده است.
     *
     * <p>تضاد یکتایی هم بررسی نمی‌شود: نام کاربری ایتا در سطح ایتا
     * یکتاست و ما آن را همان‌طور که هست نگه می‌داریم. اگر روزی ورود با
     * نام کاربری برای پنل اضافه شود، آنجا باید تضاد مدیریت شود.
     */
    private static String deriveUsername(EitaaUserData userData) {
        String username = userData.username();
        if (username == null || username.isBlank()) {
            return null;
        }
        return (username.length() <= MAX_USERNAME_LENGTH)
                ? username
                : username.substring(0, MAX_USERNAME_LENGTH);
    }

    /**
     * حساب متناظر یک کاربر ایتا.
     *
     * @param user      کاربر داخلی
     * @param tenant    مستأجر فعال — می‌تواند {@code null} باشد
     * @param tenantRole نقش کاربر در آن مستأجر — می‌تواند {@code null} باشد
     * @param identity  پیوند ایتا
     * @param newlyCreated آیا همین حالا ساخته شد؟
     */
    public record ProvisionedAccount(
            User user,
            Tenant tenant,
            MemberRole tenantRole,
            EitaaIdentity identity,
            boolean newlyCreated
    ) {

        /** آیا کاربر مستأجر فعالی دارد؟ */
        public boolean hasTenant() {
            return tenant != null && tenantRole != null;
        }
    }
}
