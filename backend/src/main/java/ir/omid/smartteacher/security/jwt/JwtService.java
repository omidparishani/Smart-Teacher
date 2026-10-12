package ir.omid.smartteacher.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import ir.omid.smartteacher.common.error.InvalidTokenException;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * صدور و اعتبارسنجی Access Token.
 *
 * <p>ساختار claimها مطابق {@code docs/security.md} بخش ۳ است. از آن
 * سند فقط یک claim اضافه شده: {@code tenant_role}. دلیلش این است که
 * نقش داخل مستأجر (TEACHER در مدرسه الف) با نقش سطح پلتفرم
 * (SUPER_ADMIN) دو چیز متفاوت‌اند و سند فقط اولی را پوشش داده بود.
 *
 * <p><b>چرا HS256؟</b> چون تک‌سرویسی هستیم: همان سرویسی که امضا می‌کند
 * اعتبارسنجی هم می‌کند، پس کلید متقارن کافی است. RS256 وقتی لازم
 * می‌شود که سرویس‌های دیگری بخواهند بدون داشتن کلید امضا، توکن را
 * <i>بررسی</i> کنند — که در معماری فعلی وجود ندارد.
 *
 * <p><b>⚠️ آنچه این کلاس انجام نمی‌دهد:</b> باطل‌سازی. Access Token
 * امضاشده تا لحظه انقضا معتبر است و در دیتابیس نیست. کوتاهی عمر (۱۵
 * دقیقه) پنجره سوءاستفاده را محدود می‌کند و باطل‌سازی واقعی از مسیر
 * Refresh Token انجام می‌شود. پس تصمیم‌های حساس نباید فقط به معتبربودن
 * امضا تکیه کنند.
 */
@Component
public class JwtService {

    private static final String CLAIM_TENANT_ID = "tenant_id";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PLAN_ID = "plan_id";
    private static final String CLAIM_TOKEN_USE = "token_use";
    private static final String CLAIM_TENANT_ROLE = "tenant_role";
    private static final String CLAIM_LOCALE = "locale";
    private static final String CLAIM_SCOPE = "scope";

    /** مقدار {@code token_use} برای توکن دسترسی. */
    private static final String USE_ACCESS = "access";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = buildSigningKey(properties.secret());
    }

    private static SecretKey buildSigningKey(String base64Secret) {
        if (base64Secret == null || base64Secret.isBlank()) {
            throw new IllegalStateException(
                    "کلید امضای JWT تنظیم نشده است؛ متغیر محیطی JWT_SECRET را مقدار بدهید");
        }

        byte[] decoded;
        try {
            decoded = Decoders.BASE64.decode(base64Secret.trim());
        } catch (RuntimeException e) {
            // ⚠️ مقدار کلید در پیام نمی‌آید.
            throw new IllegalStateException("کلید امضای JWT باید Base64 معتبر باشد");
        }

        if (decoded.length < JwtProperties.MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "کلید امضای JWT باید حداقل " + JwtProperties.MIN_SECRET_BYTES
                            + " بایت باشد (HS256) اما " + decoded.length + " بایت است");
        }

        // Keys.hmacShaKeyFor خودش طول را بررسی می‌کند؛ بررسی بالا فقط
        // برای دادن پیام فارسی روشن‌تر است.
        return Keys.hmacShaKeyFor(decoded);
    }

    // ─────────────────────────── صدور ───────────────────────────

    /**
     * صدور Access Token برای یک هویت.
     *
     * @param principal هویت جاری
     * @param scope     دامنه کاربرد — {@code MINIAPP} یا {@code ADMIN}
     * @return توکن امضاشده
     */
    public String createAccessToken(TenantPrincipal principal, TokenScope scope) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.accessTokenTtl());

        var builder = Jwts.builder()
                .issuer(properties.issuer())
                .audience().add(properties.audience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                // jti یکتا با UUIDv7 — زمان‌مرتب، پس در Log قابل ترتیب است.
                .id(UuidV7Generator.generate().toString())
                .claim(CLAIM_TOKEN_USE, USE_ACCESS)
                .claim(CLAIM_SCOPE, scope.name())
                .claim(CLAIM_LOCALE, principal.locale());

        // claimهای اختیاری: ننوشتنشان بهتر از نوشتن null است، چون null
        // در JWT به‌صورت claim تهی سریال می‌شود و رفتار کتابخانه‌های
        // مختلف در برابرش یکسان نیست.
        if (principal.userId() != null) {
            builder.subject(principal.userId().toString());
        }
        if (principal.tenantId() != null) {
            builder.claim(CLAIM_TENANT_ID, principal.tenantId().toString());
        }
        if (principal.planId() != null) {
            builder.claim(CLAIM_PLAN_ID, principal.planId().toString());
        }
        if (!principal.roles().isEmpty()) {
            builder.claim(CLAIM_ROLES, List.copyOf(principal.roles()));
        }
        if (principal.tenantRole() != null) {
            builder.claim(CLAIM_TENANT_ROLE, principal.tenantRole().name());
        }

        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }

    // ─────────────────────────── اعتبارسنجی ───────────────────────────

    /**
     * اعتبارسنجی توکن و بازسازی هویت.
     *
     * <p>امضا، صادرکننده، مخاطب و انقضا همه بررسی می‌شوند. هر خطایی به
     * {@link InvalidTokenException} تبدیل می‌شود تا جزئیات فنی به کاربر
     * نشت نکند.
     *
     * @param token توکن خام (بدون پیشوند {@code Bearer })
     * @return هویت بازسازی‌شده
     * @throws InvalidTokenException اگر توکن معتبر نباشد یا منقضی شده باشد
     */
    public TenantPrincipal parseAccessToken(String token) {
        Claims claims = parse(token);

        if (!USE_ACCESS.equals(claims.get(CLAIM_TOKEN_USE, String.class))) {
            // یک Refresh Token نباید به‌عنوان Access Token پذیرفته شود.
            throw InvalidTokenException.malformed("نوع توکن access نیست");
        }

        return new TenantPrincipal(
                parseUuid(claims.getSubject()),
                parseUuid(claims.get(CLAIM_TENANT_ID, String.class)),
                parseUuid(claims.get(CLAIM_PLAN_ID, String.class)),
                readRoles(claims),
                parseEnum(MemberRole.class, claims.get(CLAIM_TENANT_ROLE, String.class)),
                claims.get(CLAIM_LOCALE, String.class));
    }

    /**
     * خواندن claim دامنه بدون اعتبارسنجی هویت کامل.
     *
     * @param token توکن خام
     * @return دامنه توکن
     */
    public TokenScope extractScope(String token) {
        String raw = parse(token).get(CLAIM_SCOPE, String.class);
        return parseEnum(TokenScope.class, raw);
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .requireAudience(properties.audience())
                    .clockSkewSeconds(properties.clockSkew().toSeconds())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // ترتیب مهم است: ExpiredJwtException خودش JwtException است.
            throw InvalidTokenException.expired();
        } catch (JwtException | IllegalArgumentException e) {
            throw InvalidTokenException.malformed("اعتبارسنجی توکن شکست خورد");
        }
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw InvalidTokenException.malformed("شناسه داخل توکن معتبر نیست");
        }
    }

    private static Set<String> readRoles(Claims claims) {
        Object raw = claims.get(CLAIM_ROLES);
        if (!(raw instanceof List<?> list)) {
            return Set.of();
        }
        Set<String> roles = new LinkedHashSet<>();
        for (Object item : list) {
            if (item instanceof String role && !role.isBlank()) {
                roles.add(role);
            }
        }
        return roles;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException e) {
            throw InvalidTokenException.malformed("مقدار شمارشی داخل توکن معتبر نیست");
        }
    }

    // ─────────────────────────── دسترسی‌دهنده‌ها ───────────────────────────

    /** عمر Access Token به ثانیه — برای ساخت {@link TokenPair}. */
    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    /** عمر Refresh Token — برای ساخت {@link TokenPair}. */
    public Duration refreshTokenTtl() {
        return properties.refreshTokenTtl();
    }
}
