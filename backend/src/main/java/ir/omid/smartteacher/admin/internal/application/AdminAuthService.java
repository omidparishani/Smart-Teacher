package ir.omid.smartteacher.admin.internal.application;

import ir.omid.smartteacher.admin.internal.api.dto.AdminAuthResponse;
import ir.omid.smartteacher.admin.internal.api.dto.AdminAuthResponse.AdminUserView;
import ir.omid.smartteacher.audit.ActorType;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.InvalidTokenException;
import ir.omid.smartteacher.security.audit.SecurityAuditLog;
import ir.omid.smartteacher.security.audit.SecurityEvent;
import ir.omid.smartteacher.security.jwt.AuthTokenService;
import ir.omid.smartteacher.security.jwt.TokenScope;
import ir.omid.smartteacher.security.totp.TotpService;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import ir.omid.smartteacher.user.User;
import ir.omid.smartteacher.user.UserRepository;
import ir.omid.smartteacher.user.UserStatus;
import java.time.Duration;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ورود ادمین با نام کاربری و رمز — Argon2id و قفل حساب.
 *
 * <p>مطابق {@code docs/security.md} بخش ۵: پس از ۵ تلاش ناموفق، قفل
 * ۱۵ دقیقه‌ای. شمارنده در ستون‌های {@code users.failed_login_attempts}
 * و {@code locked_until} نگه داشته می‌شود.
 *
 * <p><b>🔴 چرا ثبت تلاش ناموفق در تراکنش مستقل است؟</b> چون پس از آن
 * {@code INVALID_CREDENTIALS} پرتاب می‌شود و تراکنش ورود را برمی‌گرداند.
 * اگر شمارنده و قفل داخل همان تراکنش نوشته می‌شدند، با برگشت تراکنش
 * پاک می‌شدند و قفل حساب هرگز رخ نمی‌داد — دقیقاً همان تله‌ای که
 * {@code AuthTokenService} برای ابطال خانواده توکن حل کرده است.
 *
 * <p><b>⚠️ رمز عبور هرگز Log نمی‌شود</b> — نه در پیام خطا، نه در
 * metadata حسابرسی، نه در {@code toString} درخواست.
 */
@Service
public class AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);

    /** سقف تلاش ناموفق پیش از قفل — {@code docs/security.md} بخش ۵. */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    /** مدت قفل پس از رسیدن به سقف. */
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    /** نقش‌هایی که اجازه ورود به پنل ادمین را دارند. */
    private static final Set<String> ADMIN_ROLES = Set.of("SUPER_ADMIN", "ADMIN");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService tokenService;
    private final SecurityAuditLog securityAuditLog;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate requiresNewTransaction;
    private final TotpService totpService;

    public AdminAuthService(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            AuthTokenService tokenService,
                            SecurityAuditLog securityAuditLog,
                            JdbcTemplate jdbc,
                            PlatformTransactionManager transactionManager,
                            TotpService totpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.securityAuditLog = securityAuditLog;
        this.jdbc = jdbc;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.totpService = totpService;
    }

    /**
     * ورود با نام کاربری و رمز (+ TOTP اگر فعال باشد).
     */
    @Transactional
    public AdminAuthResponse login(String username,
                                   String rawPassword,
                                   String totpCode,
                                   AuthTokenService.ClientInfo clientInfo) {
        User user = userRepository.findByUsernameIgnoreCaseAndDeletedAtIsNull(username)
                .orElse(null);

        if (user == null) {
            throw invalidCredentials();
        }

        if (isCurrentlyLocked(user)) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED,
                    "حساب کاربری موقتاً قفل شده است. کمی بعد دوباره تلاش کنید.");
        }

        if (user.getStatus() != UserStatus.ACTIVE || user.getDeletedAt() != null) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        String passwordHash = user.getPasswordHash();
        if (passwordHash == null || passwordHash.isBlank()) {
            recordLoginFailed(user.getId(), "بدون رمز عبور");
            throw invalidCredentials();
        }

        if (!passwordEncoder.matches(rawPassword, passwordHash)) {
            persistFailedAttempt(user.getId());
            throw invalidCredentials();
        }

        Set<String> roles = loadPlatformRoles(user.getId());
        if (roles.stream().noneMatch(ADMIN_ROLES::contains)) {
            securityAuditLog.record(
                    SecurityEvent.ACCESS_DENIED,
                    null,
                    user.getId(),
                    ActorType.USER,
                    "User",
                    user.getId(),
                    Map.of("reason", "ورود ادمین بدون نقش پلتفرمی"));
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        if (totpService.isEnforcementEnabled()
                && user.isTotpEnabled()
                && roles.contains("SUPER_ADMIN")) {
            if (!totpService.verify(user.getTotpSecret(), totpCode)) {
                persistFailedAttempt(user.getId());
                throw new AppException(ErrorCode.INVALID_CREDENTIALS,
                        "کد تأیید دو مرحله‌ای نامعتبر است.");
            }
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());

        TenantPrincipal principal = TenantPrincipal.ofPlatformUser(
                user.getId(), roles, user.getPreferredLocale());

        AuthTokenService.IssuedTokens issued = tokenService.issue(
                principal,
                AuthTokenService.RefreshSession.newSession(user.getId(), null),
                TokenScope.ADMIN,
                clientInfo);

        log.debug("ورود موفق ادمین {}", user.getId());

        return new AdminAuthResponse(
                issued.pair().accessToken(),
                issued.pair().refreshToken(),
                issued.pair().tokenType(),
                issued.pair().expiresIn(),
                issued.pair().refreshExpiresIn(),
                new AdminUserView(user.getId(), user.getFullName(), user.getUsername(), roles));
    }

    /** سازگاری با فراخوانی‌های قدیمی بدون totp. */
    @Transactional
    public AdminAuthResponse login(String username,
                                   String rawPassword,
                                   AuthTokenService.ClientInfo clientInfo) {
        return login(username, rawPassword, null, clientInfo);
    }

    /**
     * تازه‌سازی نشست ادمین — فقط {@link TokenScope#ADMIN}.
     */
    @Transactional
    public AdminAuthResponse refresh(String rawRefreshToken, AuthTokenService.ClientInfo clientInfo) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw InvalidTokenException.malformed("توکن تازه‌سازی خالی است");
        }

        AuthTokenService.ConsumedRefresh consumed = tokenService.consume(rawRefreshToken);
        if (consumed.scope() != TokenScope.ADMIN) {
            throw InvalidTokenException.malformed("دامنه توکن برای پنل ادمین معتبر نیست");
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(consumed.session().userId())
                .orElseThrow(() -> InvalidTokenException.revoked("کاربر یافت نشد"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        Set<String> roles = loadPlatformRoles(user.getId());
        if (roles.stream().noneMatch(ADMIN_ROLES::contains)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        TenantPrincipal principal = TenantPrincipal.ofPlatformUser(
                user.getId(), roles, user.getPreferredLocale());

        AuthTokenService.IssuedTokens issued = tokenService.issue(
                principal,
                consumed.session().next(consumed.tokenId()),
                TokenScope.ADMIN,
                clientInfo);

        return new AdminAuthResponse(
                issued.pair().accessToken(),
                issued.pair().refreshToken(),
                issued.pair().tokenType(),
                issued.pair().expiresIn(),
                issued.pair().refreshExpiresIn(),
                new AdminUserView(user.getId(), user.getFullName(), user.getUsername(), roles));
    }

    /**
     * خروج ادمین — ابطال خانواده refresh؛ همیشه موفق حتی با توکن ناشناخته.
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        int revoked = tokenService.revokeFamily(rawRefreshToken, "خروج ادمین");
        log.debug("خروج ادمین؛ {} توکن باطل شد", revoked);
    }

    /**
     * افزایش شمارنده / قفل در تراکنش مستقل تا با پرتاب استثنا پاک نشود.
     *
     * <p>با JDBC مستقیم نوشته می‌شود تا به موجودیت مدیریت‌شده‌ی تراکنش
     * بیرونی وابسته نباشد (آن موجودیت با rollback باطل می‌شود).
     */
    private void persistFailedAttempt(UUID userId) {
        Boolean locked = requiresNewTransaction.execute(status -> {
            Integer current = jdbc.queryForObject(
                    "select failed_login_attempts from users where id = ?",
                    Integer.class,
                    userId);
            int attempts = (current == null ? 0 : current) + 1;

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                Instant until = Instant.now().plus(LOCK_DURATION);
                jdbc.update("""
                        update users
                           set failed_login_attempts = 0,
                               locked_until = ?
                         where id = ?
                        """,
                        Timestamp.from(until), userId);
                return Boolean.TRUE;
            }

            jdbc.update("""
                    update users
                       set failed_login_attempts = ?
                     where id = ?
                    """,
                    attempts, userId);
            return Boolean.FALSE;
        });

        recordLoginFailed(userId, "رمز نادرست");

        if (Boolean.TRUE.equals(locked)) {
            securityAuditLog.record(
                    SecurityEvent.ACCOUNT_LOCKED,
                    null,
                    userId,
                    ActorType.USER,
                    "User",
                    userId,
                    Map.of("lockMinutes", String.valueOf(LOCK_DURATION.toMinutes())));
            log.warn("حساب {} به‌دلیل تلاش‌های ناموفق قفل شد", userId);
        }
    }

    private void recordLoginFailed(UUID userId, String reason) {
        securityAuditLog.record(
                SecurityEvent.LOGIN_FAILED,
                null,
                userId,
                ActorType.USER,
                "User",
                userId,
                Map.of("reason", reason));
    }

    private static boolean isCurrentlyLocked(User user) {
        Instant until = user.getLockedUntil();
        return until != null && until.isAfter(Instant.now());
    }

    private static AppException invalidCredentials() {
        return new AppException(ErrorCode.INVALID_CREDENTIALS);
    }

    /**
     * نقش‌های پلتفرمی کاربر ({@code user_roles.tenant_id IS NULL}).
     *
     * <p>موجودیت JPA برای {@code user_roles} هنوز نیست (RBAC کامل در
     * گام ۳ فاز ۴). JDBC اینجا کافی و بدون وابستگی مدولار اضافه است.
     */
    private Set<String> loadPlatformRoles(UUID userId) {
        List<String> codes = jdbc.queryForList("""
                select r.code
                  from user_roles ur
                  join roles r on r.id = ur.role_id
                 where ur.user_id = ?
                   and ur.tenant_id is null
                """, String.class, userId);
        return new LinkedHashSet<>(codes);
    }
}
