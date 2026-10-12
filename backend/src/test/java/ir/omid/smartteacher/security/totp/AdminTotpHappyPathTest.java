package ir.omid.smartteacher.security.totp;

import static org.assertj.core.api.Assertions.assertThat;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import ir.omid.smartteacher.admin.internal.api.dto.AdminAuthResponse;
import ir.omid.smartteacher.admin.internal.application.AdminAuthService;
import ir.omid.smartteacher.admin.internal.application.AdminTotpService;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.security.jwt.AuthTokenService;
import ir.omid.smartteacher.tenant.TenantContext;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import ir.omid.smartteacher.user.User;
import ir.omid.smartteacher.user.UserRepository;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * مسیر خوش‌حال 2FA با secret ثابت تولیدشده توسط کتابخانه.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class AdminTotpHappyPathTest {

    private static final UUID ROLE_SUPER_ADMIN =
            UUID.fromString("10000000-0000-7000-8000-000000000001");

    static final String POSTGRES_IMAGE =
            System.getProperty("test.postgres.image", "postgres:17-alpine");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AdminTotpService adminTotpService;

    @Autowired
    private AdminAuthService adminAuthService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TotpService totpService;

    @Test
    @DisplayName("setup + confirm + login با کد TOTP معتبر")
    void totp_enable_and_login() {
        String username = "totp-admin-" + unique();
        String password = "TotpPass-" + unique();
        UUID userId = seedSuperAdmin(username, password);

        TenantPrincipal principal = TenantPrincipal.ofPlatformUser(
                userId, Set.of("SUPER_ADMIN"), "fa");

        AdminTotpService.TotpSetupResponse setup = TenantContext.callAs(principal,
                adminTotpService::beginSetup);
        assertThat(setup.secret()).isNotBlank();

        int code = new GoogleAuthenticator().getTotpPassword(setup.secret());
        TenantContext.callAs(principal, () -> {
            adminTotpService.confirmSetup(String.valueOf(code));
            return null;
        });

        User user = userRepository.findById(userId).orElseThrow();
        assertThat(user.isTotpEnabled()).isTrue();
        assertThat(totpService.isEnforcementEnabled()).isTrue();

        int loginCode = new GoogleAuthenticator().getTotpPassword(user.getTotpSecret());
        AdminAuthResponse auth = adminAuthService.login(
                username, password, String.valueOf(loginCode),
                new AuthTokenService.ClientInfo("127.0.0.1", "test"));
        assertThat(auth.accessToken()).isNotBlank();
    }

    private UUID seedSuperAdmin(String username, String password) {
        UUID userId = UuidV7Generator.generate();
        jdbc.update("""
                insert into users (id, username, password_hash, full_name, status)
                values (?, ?, ?, ?, 'ACTIVE')
                """,
                userId, username, passwordEncoder.encode(password), "ادمین TOTP");
        jdbc.update("""
                insert into user_roles (id, user_id, role_id, tenant_id)
                values (?, ?, ?, null)
                """,
                UuidV7Generator.generate(), userId, ROLE_SUPER_ADMIN);
        return userId;
    }

    private static String unique() {
        return SEQ.incrementAndGet() + UUID.randomUUID().toString().substring(0, 6);
    }
}
