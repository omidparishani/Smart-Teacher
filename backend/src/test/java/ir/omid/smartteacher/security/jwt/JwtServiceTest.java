package ir.omid.smartteacher.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.InvalidTokenException;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * آزمون صدور و اعتبارسنجی توکن.
 *
 * <p>تمرکز این آزمون روی <b>رد کردن</b> است، نه پذیرفتن. توکنی که درست
 * ساخته شود و درست خوانده شود، حداقل انتظار است؛ آنچه امنیت را می‌سازد
 * این است که توکن دست‌کاری‌شده، منقضی، با امضای دیگر، یا از صادرکننده
 * دیگر پذیرفته <i>نشود</i>. هر یک از این‌ها یک تست جدا دارد با دلیل
 * اینکه چه حمله‌ای را می‌بندد.
 *
 * <p>هیچ‌کدام از کلیدهای این فایل راز واقعی نیستند.
 */
class JwtServiceTest {

    /** Base64 از «0123456789abcdef0123456789abcdef» (۳۲ بایت). */
    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    /** Base64 از «fedcba9876543210fedcba9876543210» (۳۲ بایت). */
    private static final String OTHER_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-aaaa-7aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-7111-8111-111111111111");
    private static final UUID PLAN_ID = UUID.fromString("33333333-3333-7333-8333-333333333333");

    private final JwtService jwtService = service(new JwtProperties(SECRET, null, null, null, null, null));

    private static JwtService service(JwtProperties properties) {
        return new JwtService(properties);
    }

    private static TenantPrincipal fullPrincipal() {
        return TenantPrincipal.ofTenantUser(
                USER_ID, TENANT_ID, PLAN_ID, Set.of("TEACHER", "ADMIN"), MemberRole.TEACHER, "fa");
    }

    // ─────────────────────── رفت و برگشت ───────────────────────

    @Nested
    @DisplayName("رفت و برگشت")
    class RoundTrip {

        @Test
        @DisplayName("همه claim‌ها سالم برمی‌گردند")
        void preservesAllClaims() {
            String token = jwtService.createAccessToken(fullPrincipal(), TokenScope.MINIAPP);

            TenantPrincipal parsed = jwtService.parseAccessToken(token);

            assertThat(parsed.userId()).isEqualTo(USER_ID);
            assertThat(parsed.tenantId()).isEqualTo(TENANT_ID);
            assertThat(parsed.planId()).isEqualTo(PLAN_ID);
            assertThat(parsed.roles()).containsExactlyInAnyOrder("TEACHER", "ADMIN");
            assertThat(parsed.tenantRole()).isEqualTo(MemberRole.TEACHER);
            assertThat(parsed.locale()).isEqualTo("fa");
        }

        @Test
        @DisplayName("هویت کامل برابر با اصل است")
        void roundTripsToEqualPrincipal() {
            TenantPrincipal original = fullPrincipal();

            String token = jwtService.createAccessToken(original, TokenScope.MINIAPP);

            assertThat(jwtService.parseAccessToken(token)).isEqualTo(original);
        }

        @Test
        @DisplayName("هویت سطح پلتفرم بدون مستأجر هم کار می‌کند")
        void platformUserWithoutTenant() {
            TenantPrincipal platformUser = TenantPrincipal.ofPlatformUser(
                    USER_ID, Set.of("SUPER_ADMIN"), "fa");

            String token = jwtService.createAccessToken(platformUser, TokenScope.ADMIN);
            TenantPrincipal parsed = jwtService.parseAccessToken(token);

            assertThat(parsed.userId()).isEqualTo(USER_ID);
            assertThat(parsed.tenantId()).isNull();
            assertThat(parsed.planId()).isNull();
            assertThat(parsed.isSuperAdmin()).isTrue();
            assertThat(parsed.hasTenant()).isFalse();
        }

        @Test
        @DisplayName("دامنه توکن قابل استخراج است")
        void extractsScope() {
            String miniappToken = jwtService.createAccessToken(fullPrincipal(), TokenScope.MINIAPP);
            String adminToken = jwtService.createAccessToken(fullPrincipal(), TokenScope.ADMIN);

            assertThat(jwtService.extractScope(miniappToken)).isEqualTo(TokenScope.MINIAPP);
            assertThat(jwtService.extractScope(adminToken)).isEqualTo(TokenScope.ADMIN);
        }

        @Test
        @DisplayName("عمر توکن گزارش می‌شود")
        void reportsTtls() {
            assertThat(jwtService.accessTokenTtlSeconds()).isEqualTo(900);
            assertThat(jwtService.refreshTokenTtl()).isEqualTo(Duration.ofDays(30));
        }

        @Test
        @DisplayName("دو توکن پیاپی مقدار یکسان ندارند (jti متمایز)")
        void generatesDistinctTokens() {
            String first = jwtService.createAccessToken(fullPrincipal(), TokenScope.MINIAPP);
            String second = jwtService.createAccessToken(fullPrincipal(), TokenScope.MINIAPP);

            assertThat(first).isNotEqualTo(second);
        }
    }

    // ─────────────────────── رد کردن ───────────────────────

    @Nested
    @DisplayName("رد کردن توکن نامعتبر")
    class Rejection {

        @Test
        @DisplayName("توکن با امضای کلید دیگر رد می‌شود")
        void rejectsTokenSignedWithAnotherKey() {
            JwtService attackerService =
                    service(new JwtProperties(OTHER_SECRET, null, null, null, null, null));
            String forged = attackerService.createAccessToken(fullPrincipal(), TokenScope.MINIAPP);

            // مهم‌ترین تست این فایل: اگر اینجا شکست بخورد، هر کسی با
            // هر کلیدی می‌تواند توکن بسازد و کل احراز هویت بی‌معناست.
            assertThatThrownBy(() -> jwtService.parseAccessToken(forged))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("توکن با صادرکننده دیگر رد می‌شود")
        void rejectsTokenFromAnotherIssuer() {
            JwtService otherIssuer = service(new JwtProperties(
                    SECRET, "some-other-service", null, null, null, null));
            String foreign = otherIssuer.createAccessToken(fullPrincipal(), TokenScope.MINIAPP);

            assertThatThrownBy(() -> jwtService.parseAccessToken(foreign))
                    .as("توکن سرویس دیگر نباید در این سرویس معتبر باشد")
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("توکن با مخاطب دیگر رد می‌شود")
        void rejectsTokenForAnotherAudience() {
            JwtService otherAudience = service(new JwtProperties(
                    SECRET, null, "another-audience", null, null, null));
            String foreign = otherAudience.createAccessToken(fullPrincipal(), TokenScope.MINIAPP);

            assertThatThrownBy(() -> jwtService.parseAccessToken(foreign))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("توکن منقضی با کد خطای مشخص رد می‌شود")
        void rejectsExpiredToken() {
            JwtService expiredIssuer = service(new JwtProperties(
                    SECRET, null, null, Duration.ofMinutes(-5), null, Duration.ZERO));
            String expired = expiredIssuer.createAccessToken(fullPrincipal(), TokenScope.MINIAPP);

            // کد خطا مهم است: کلاینت با دیدن TOKEN_EXPIRED سراغ Refresh
            // Token می‌رود، ولی با UNAUTHENTICATED او را به ورود می‌فرستد.
            assertThatThrownBy(() -> jwtService.parseAccessToken(expired))
                    .isInstanceOf(InvalidTokenException.class)
                    .extracting(e -> ((InvalidTokenException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("رشته بی‌معنا رد می‌شود")
        void rejectsGarbage() {
            assertThatThrownBy(() -> jwtService.parseAccessToken("not-a-jwt"))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("توکن خالی رد می‌شود")
        void rejectsEmpty() {
            assertThatThrownBy(() -> jwtService.parseAccessToken(""))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("توکن با بخش امضا برداشته‌شده رد می‌شود")
        void rejectsUnsignedToken() {
            String token = jwtService.createAccessToken(fullPrincipal(), TokenScope.MINIAPP);
            String headerAndPayload = token.substring(0, token.lastIndexOf('.'));

            // کلاسیک‌ترین حمله: امضا را بردار و alg را none کن.
            assertThatThrownBy(() -> jwtService.parseAccessToken(headerAndPayload))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("تغییر یک کاراکتر در بدنه توکن رد می‌شود")
        void rejectsTamperedPayload() {
            String token = jwtService.createAccessToken(fullPrincipal(), TokenScope.MINIAPP);
            String[] parts = token.split("\\.");
            char first = parts[1].charAt(0);
            char replacement = (first == 'e') ? 'f' : 'e';
            String tampered = parts[0] + "." + replacement + parts[1].substring(1) + "." + parts[2];

            assertThatThrownBy(() -> jwtService.parseAccessToken(tampered))
                    .as("امضا باید هر تغییری در بدنه را بگیرد")
                    .isInstanceOf(InvalidTokenException.class);
        }
    }

    // ─────────────────────── پیکربندی ───────────────────────

    @Nested
    @DisplayName("پیکربندی کلید")
    class KeyConfiguration {

        @Test
        @DisplayName("کلید کوتاه‌تر از ۲۵۶ بیت پذیرفته نمی‌شود")
        void rejectsShortSecret() {
            String shortSecret = Base64.getEncoder().encodeToString(new byte[16]);

            assertThatThrownBy(() -> service(new JwtProperties(shortSecret, null, null, null, null, null)))
                    .as("HS256 به کلید حداقل ۳۲ بایتی نیاز دارد")
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("نبود کلید باعث شکست بالا آمدن می‌شود، نه کلید پیش‌فرض")
        void rejectsMissingSecret() {
            assertThatThrownBy(() -> service(new JwtProperties(null, null, null, null, null, null)))
                    .as("نبود کلید باید اپلیکیشن را متوقف کند — رفتار درست")
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> service(new JwtProperties("  ", null, null, null, null, null)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("پیام خطای کلید، خود کلید را لو نمی‌دهد")
        void errorMessageDoesNotLeakSecret() {
            String shortSecret = Base64.getEncoder().encodeToString(new byte[16]);

            assertThatThrownBy(() -> service(new JwtProperties(shortSecret, null, null, null, null, null)))
                    .hasMessageNotContaining(shortSecret);
        }

        @Test
        @DisplayName("toString کلید را پنهان می‌کند")
        void toStringMasksSecret() {
            JwtProperties properties = new JwtProperties(SECRET, null, null, null, null, null);

            assertThat(properties.toString())
                    .doesNotContain(SECRET)
                    .contains("secret=***");
        }
    }
}
