package ir.omid.smartteacher.security.jwt;

import ir.omid.smartteacher.audit.ActorType;
import ir.omid.smartteacher.common.error.InvalidTokenException;
import ir.omid.smartteacher.common.error.TokenReuseDetectedException;
import ir.omid.smartteacher.common.util.Hashing;
import ir.omid.smartteacher.security.audit.SecurityAuditLog;
import ir.omid.smartteacher.security.audit.SecurityEvent;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * صدور، چرخش و ابطال توکن‌های نشست.
 *
 * <p>این کلاس فقط <i>مکانیک توکن</i> را می‌داند: تولید، هش، ذخیره، چرخش،
 * و تشخیص استفاده مجدد. نمی‌داند کاربر از کجا آمده، مستأجرش چیست، یا
 * نقشش چیست — آن‌ها را به‌صورت {@link RefreshSession} می‌گیرد. همین
 * تفکیک باعث می‌شود همین کلاس برای ورود ایتا و ورود پنل ادمین یکسان
 * باشد — هر دو فقط {@link TokenScope} متفاوت می‌فرستند.
 *
 * <p><b>🔴 تشخیص استفاده مجدد — و یک تله که اینجا حل شده:</b> وقتی
 * توکنی که قبلاً مصرف شده دوباره استفاده شود، باید <i>کل خانواده</i>
 * باطل شود و بعد استثنا پرتاب شود. ولی پرتاب استثنا تراکنش را برمی‌گرداند
 * — یعنی ابطال خانواده هم برمی‌گشت و حمله‌کننده می‌توانست همان توکن
 * دزدیده‌شده را بی‌نهایت بار امتحان کند. پس ابطال خانواده در یک تراکنش
 * <b>مستقل</b> ({@code REQUIRES_NEW}) انجام می‌شود تا برگشت‌ناپذیر
 * باشد. بدون این، مکانیزم امنیتی ظاهر داشت ولی کار نمی‌کرد.
 */
@Component
public class AuthTokenService {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenService.class);

    /**
     * ۳۲ بایت آنتروپی = ۲۵۶ بیت.
     *
     * <p>توکن refresh برخلاف Access Token امضا نمی‌شود؛ امنیتش کاملاً به
     * غیرقابل‌حدس‌بودنش وابسته است. پس آنتروپی باید در سطح کلید
     * رمزنگاری باشد، نه در سطح شناسه.
     */
    private static final int REFRESH_TOKEN_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** طول ستون {@code user_agent} — مقادیر بلندتر بریده می‌شوند. */
    private static final int MAX_USER_AGENT_LENGTH = 500;

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityAuditLog securityAuditLog;

    /**
     * تراکنش مستقل — فقط برای ابطال خانواده هنگام تشخیص سرقت.
     *
     * <p>ساخته‌شدنش عیناً مثل الگوی {@code SecurityAuditLog} است، چون
     * همان نیاز را برآورده می‌کند: کاری که باید حتی پس از برگشت‌خوردن
     * تراکنش اصلی باقی بماند.
     */
    private final TransactionTemplate requiresNewTransaction;

    public AuthTokenService(JwtService jwtService,
                            RefreshTokenRepository refreshTokenRepository,
                            SecurityAuditLog securityAuditLog,
                            PlatformTransactionManager transactionManager) {
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.securityAuditLog = securityAuditLog;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // ─────────────────────────── مدل‌های کمکی ───────────────────────────

    /**
     * اطلاعات نشستی که توکن به آن تعلق دارد.
     *
     * @param userId          کاربر
     * @param tenantId        مستأجر فعال — می‌تواند {@code null} باشد
     * @param familyId        شناسه خانواده؛ چرخش، این را ثابت نگه می‌دارد
     * @param previousTokenId توکنی که این توکن جایگزینش می‌شود؛ برای
     *                        ورود تازه {@code null} است
     */
    public record RefreshSession(UUID userId,
                                 UUID tenantId,
                                 UUID familyId,
                                 UUID previousTokenId) {

        /**
         * نشست تازه — ابتدای یک خانواده.
         *
         * <p>خانواده در لحظه ورود ساخته می‌شود و هر چرخش آن را نگه
         * می‌دارد. پس «خانواده» یعنی «این زنجیره ورود»، و ابطالش یعنی
         * پایان همان نشست — نه همه نشست‌های کاربر روی دستگاه‌های دیگر.
         */
        public static RefreshSession newSession(UUID userId, UUID tenantId) {
            return new RefreshSession(userId, tenantId, UUID.randomUUID(), null);
        }

        /** نشست بعدی در همان خانواده، جایگزین توکن فعلی. */
        public RefreshSession next(UUID replacedTokenId) {
            return new RefreshSession(userId, tenantId, familyId, replacedTokenId);
        }
    }

    /** اطلاعات کلاینت — برای «دستگاه‌های واردشده» و عیب‌یابی. */
    public record ClientInfo(String ipAddress, String userAgent) {

        /** کلاینت ناشناس — وقتی اطلاعات درخواست در دسترس نیست. */
        public static ClientInfo unknown() {
            return new ClientInfo(null, null);
        }
    }

    /** توکن مصرف‌شده و نشستی که به آن تعلق داشت. */
    public record ConsumedRefresh(UUID tokenId,
                                  RefreshSession session,
                                  TokenScope scope) {
    }

    /** توکن‌های تازه‌صادرشده. */
    public record IssuedTokens(TokenPair pair, UUID refreshTokenId) {
    }

    // ─────────────────────────── صدور ───────────────────────────

    /**
     * صدور یک جفت توکن تازه.
     *
     * <p>{@code MANDATORY} عمدی است: این متد باید داخل تراکنش فراخوان
     * اجرا شود. اگر بدون تراکنش اجرا می‌شد، «باطل‌کردن توکن قبلی» و
     * «ذخیره توکن جدید» دو عملیات جدا می‌شدند و یک خطای میانی می‌توانست
     * کاربر را با توکن قبلیِ باطل‌شده و بدون توکن جدید رها کند — یعنی
     * خروج اجباری از حساب بدون هیچ پیام روشنی.
     *
     * @param principal کاربر و زمینه‌اش
     * @param session   نشست — برای ورود تازه از
     *                  {@link RefreshSession#newSession} بسازید
     * @param scope     دامنه توکن
     * @param clientInfo اطلاعات کلاینت؛ {@code null} مجاز است
     * @return توکن‌ها و شناسه رکورد توکن تازه‌سازی
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public IssuedTokens issue(TenantPrincipal principal,
                              RefreshSession session,
                              TokenScope scope,
                              ClientInfo clientInfo) {
        String rawRefreshToken = newRawToken();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtService.refreshTokenTtl());

        RefreshToken stored = new RefreshToken(
                session.userId(),
                session.tenantId(),
                Hashing.sha256Hex(rawRefreshToken),
                session.familyId(),
                scope,
                expiresAt);

        if (clientInfo != null) {
            stored.setIpAddress(clientInfo.ipAddress());
            stored.setUserAgent(truncate(clientInfo.userAgent(), MAX_USER_AGENT_LENGTH));
        }

        // پیوند زنجیره: توکن قبلی می‌داند چه چیزی جایش آمده. برای ردیابی
        // هنگام بررسی سرقت لازم است — بدون آن معلوم نمی‌شود کدام توکن
        // جانشین کدام شده.
        if (session.previousTokenId() != null) {
            refreshTokenRepository.findById(session.previousTokenId())
                    .ifPresent(previous -> previous.setReplacedById(stored.getId()));
        }

        refreshTokenRepository.save(stored);

        String accessToken = jwtService.createAccessToken(principal, scope);

        TokenPair pair = TokenPair.of(
                accessToken,
                rawRefreshToken,
                jwtService.accessTokenTtlSeconds(),
                jwtService.refreshTokenTtl().toSeconds());

        return new IssuedTokens(pair, stored.getId());
    }

    // ─────────────────────────── مصرف و چرخش ───────────────────────────

    /**
     * مصرف یک توکن تازه‌سازی: اعتبارسنجی، و ابطال آن برای چرخش.
     *
     * <p>اگر توکن قبلاً مصرف شده باشد، اینجا سرقت تشخیص داده می‌شود.
     *
     * @param rawToken توکن خام دریافتی از کلاینت
     * @return نشست متعلق به توکن
     * @throws InvalidTokenException    اگر توکن ناشناخته یا منقضی باشد
     * @throws TokenReuseDetectedException اگر توکن قبلاً مصرف شده باشد
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public ConsumedRefresh consume(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw InvalidTokenException.malformed("توکن تازه‌سازی خالی است");
        }

        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(Hashing.sha256Hex(rawToken))
                .orElseThrow(() -> InvalidTokenException.malformed(
                        "توکن تازه‌سازی در دیتابیس یافت نشد"));

        if (stored.isRevoked()) {
            handleReuseDetection(stored);
        }

        if (stored.isExpired()) {
            throw InvalidTokenException.expired();
        }

        // ── چرخش: توکن فعلی باطل می‌شود؛ جانشینش در مرحله صدور ثبت می‌شود ──
        stored.revoke("چرخش توکن");

        return new ConsumedRefresh(
                stored.getId(),
                new RefreshSession(stored.getUserId(), stored.getTenantId(),
                        stored.getFamilyId(), stored.getId()),
                stored.getScope());
    }

    /**
     * واکنش به استفاده مجدد از توکن باطل‌شده — نشانه قطعی سرقت.
     *
     * <p>سند {@code docs/security.md} بخش ۳: «اگر توکنی که قبلاً مصرف شده
     * دوباره استفاده شود → کل خانواده توکن باطل می‌شود و رویداد امنیتی
     * CRITICAL ثبت می‌گردد.»
     */
    private void handleReuseDetection(RefreshToken stored) {
        UUID familyId = stored.getFamilyId();

        // ⚠️ ابتدا ثبت رویداد، بعد ابطال. اگر برعکس بود و ابطال خطا
        // می‌داد، هیچ ردپایی از حمله نمی‌ماند.
        securityAuditLog.record(
                SecurityEvent.TOKEN_REUSE_DETECTED,
                stored.getTenantId(),
                stored.getUserId(),
                ActorType.USER,
                "RefreshToken",
                stored.getId(),
                Map.of("familyId", String.valueOf(familyId)));

        // 🔴 ابطال در تراکنش مستقل — چون استثنای پایین، تراکنش اصلی را
        // برمی‌گرداند و ابطالِ داخل آن از بین می‌رفت.
        Integer revoked = requiresNewTransaction.execute(status ->
                refreshTokenRepository.revokeFamily(familyId, Instant.now(),
                        "تشخیص استفاده مجدد از توکن — احتمال سرقت"));

        log.warn("استفاده مجدد از توکن باطل‌شده تشخیص داده شد؛ {} توکن در خانواده {} باطل شد",
                revoked, familyId);

        securityAuditLog.record(
                SecurityEvent.TOKEN_FAMILY_REVOKED,
                stored.getTenantId(),
                stored.getUserId(),
                ActorType.USER,
                "RefreshToken",
                stored.getId(),
                Map.of("familyId", String.valueOf(familyId),
                        "revokedCount", String.valueOf(revoked)));

        throw new TokenReuseDetectedException(familyId);
    }

    // ─────────────────────────── ابطال ───────────────────────────

    /**
     * ابطال کل خانواده یک توکن — برای خروج.
     *
     * <p>چرا کل خانواده و نه فقط همان توکن؟ چون یک خانواده یعنی یک
     * زنجیره ورود. اگر فقط توکن فعلی باطل شود، توکن‌های قبلیِ همان
     * زنجیره (که مهاجم ممکن است داشته باشد) هنوز معتبر می‌مانند و
     * «خروج» عملاً خروج نیست.
     *
     * <p>⚠️ برخلاف حالت تشخیص سرقت، اینجا ابطال در تراکنش فراخوان
     * انجام می‌شود و استثنا پرتاب نمی‌کنیم — پس نیازی به تراکنش مستقل
     * نیست.
     *
     * @param rawToken توکن خام کلاینت
     * @return تعداد توکن‌های باطل‌شده
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int revokeFamily(String rawToken, String reason) {
        if (rawToken == null || rawToken.isBlank()) {
            return 0;
        }

        return refreshTokenRepository
                .findByTokenHash(Hashing.sha256Hex(rawToken))
                .map(stored -> refreshTokenRepository.revokeFamily(
                        stored.getFamilyId(), Instant.now(), reason))
                // ⚠️ توکن ناشناخته، خطا نیست. خروج باید همیشه «موفق»
                // باشد: اگر به کاربر خطا بدهیم، کلاینت ممکن است توکن را
                // پاک نکند و فکر کند هنوز وارد است. باطل‌کردن توکن
                // ناشناخته هم بی‌معناست.
                .orElse(0);
    }

    // ─────────────────────────── کمکی ───────────────────────────

    /**
     * تولید توکن خام با آنتروپی رمزنگاری.
     *
     * <p>Base64 با الفبای URL-safe و بدون padding: نتیجه فقط کاراکترهای
     * امن برای هدر و JSON دارد، پس هیچ‌جا نیاز به encode/decode اضافه
     * نیست.
     */
    private static String newRawToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return (value.length() <= maxLength) ? value : value.substring(0, maxLength);
    }
}
