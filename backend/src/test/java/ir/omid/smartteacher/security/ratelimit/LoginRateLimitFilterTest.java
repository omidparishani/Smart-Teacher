package ir.omid.smartteacher.security.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ir.omid.smartteacher.common.util.UuidV7Generator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * فیلتر محدودیت نرخ روی {@code /auth/login} — با سقف پایین برای تست.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@TestPropertySource(properties = {
        "app.rate-limit.login.enabled=true",
        "app.rate-limit.login.max-requests=3",
        "app.rate-limit.login.window=5m"
})
class LoginRateLimitFilterTest {

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
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InMemoryRateLimiter rateLimiter;

    @BeforeEach
    void clearBuckets() {
        rateLimiter.reset();
    }

    @Test
    @DisplayName("پس از ۳ تلاش ورود، درخواست چهارم ۴۲۹ می‌دهد")
    void fourthLoginIsRateLimited() throws Exception {
        String username = "rate-limit-" + unique();
        seedSuperAdmin(username, "Rate-Limit-Pass-9!");

        String body = "{\"username\":\"" + username + "\",\"password\":\"wrong-password\"}";

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("مسیرهای auth برنامک ایتا نیز با سقف ۴۲۹ محدود می‌شوند")
    void refreshEndpointIsRateLimited() throws Exception {
        String body = "{\"refreshToken\":\"dummy-token\"}";

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }

    private void seedSuperAdmin(String username, String rawPassword) {
        UUID userId = UuidV7Generator.generate();
        String hash = passwordEncoder.encode(rawPassword);
        jdbc.update(
                "insert into users (id, username, password_hash, full_name, status) values (?, ?, ?, ?, 'ACTIVE')",
                userId, username, hash, "rate-admin-" + unique());
        jdbc.update(
                "insert into user_roles (id, user_id, role_id, tenant_id) values (?, ?, ?, null)",
                UuidV7Generator.generate(), userId, ROLE_SUPER_ADMIN);
    }

    private static String unique() {
        return Integer.toString(SEQ.incrementAndGet());
    }
}
