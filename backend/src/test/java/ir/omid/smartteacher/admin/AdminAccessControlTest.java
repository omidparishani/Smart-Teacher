package ir.omid.smartteacher.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import ir.omid.smartteacher.admin.internal.application.AdminAuthService;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.security.jwt.JwtService;
import ir.omid.smartteacher.security.jwt.TokenScope;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * قید پذیرش فاز ۴ گام ۱: معلم به {@code /admin/**} دسترسی ندارد؛
 * {@code SUPER_ADMIN} دارد. ورود با رمز و قفل حساب هم اینجا ثابت می‌شود.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AdminAccessControlTest {

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
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("توکن معلم به /admin/dashboard دسترسی ندارد")
    void teacherCannotAccessAdminDashboard() throws Exception {
        UUID teacherId = UuidV7Generator.generate();
        UUID tenantId = insertTeacherTenant(teacherId);

        String teacherToken = jwtService.createAccessToken(
                TenantPrincipal.ofTenantUser(
                        teacherId, tenantId, null, Set.of(), MemberRole.TEACHER, "fa"),
                TokenScope.MINIAPP);

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("توکن معلم به /admin/users دسترسی ندارد")
    void teacherCannotAccessAdminUsers() throws Exception {
        UUID teacherId = UuidV7Generator.generate();
        UUID tenantId = insertTeacherTenant(teacherId);

        String teacherToken = jwtService.createAccessToken(
                TenantPrincipal.ofTenantUser(
                        teacherId, tenantId, null, Set.of(), MemberRole.TEACHER, "fa"),
                TokenScope.MINIAPP);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("SUPER_ADMIN به داشبورد و فهرست کاربران دسترسی دارد")
    void superAdminCanAccessAdminEndpoints() throws Exception {
        String username = "superadmin-" + unique();
        String password = "Str0ng-Admin-Pass!";
        UUID adminId = insertSuperAdmin(username, password);

        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users.total").isNumber())
                .andExpect(jsonPath("$.tenants.total").isNumber())
                .andExpect(jsonPath("$.education.classrooms").isNumber())
                .andExpect(jsonPath("$.education.students").isNumber())
                // بخش‌های پیاده‌نشده نباید با صفر جعلی بیایند
                .andExpect(jsonPath("$.revenue").doesNotExist())
                .andExpect(jsonPath("$.ai").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("ورود ادمین با رمز درست توکن ADMIN می‌دهد")
    void adminLoginSucceedsWithPassword() throws Exception {
        String username = "login-ok-" + unique();
        String password = "Correct-Pass-99!";
        insertSuperAdmin(username, password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.roles[0]").value("SUPER_ADMIN"))
                .andReturn();

        String accessToken = JsonPath.read(
                result.getResponse().getContentAsString(), "$.accessToken");

        assertThat(jwtService.extractScope(accessToken)).isEqualTo(TokenScope.ADMIN);

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ورود با رمز نادرست ۴۰۱ می‌دهد و پس از ۵ تلاش قفل می‌شود")
    void adminLoginLocksAfterFailedAttempts() throws Exception {
        String username = "lock-me-" + unique();
        String password = "Real-Pass-11!";
        UUID userId = insertSuperAdmin(username, password);

        for (int i = 0; i < AdminAuthService.MAX_FAILED_ATTEMPTS; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"%s","password":"wrong"}
                                    """.formatted(username)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        Instant lockedUntil = jdbc.query(
                "select locked_until from users where id = ?",
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Timestamp ts = rs.getTimestamp("locked_until");
                    return ts == null ? null : ts.toInstant();
                },
                userId);
        assertThat(lockedUntil).isNotNull();
        assertThat(lockedUntil).isAfter(Instant.now().minusSeconds(5));

        // حتی با رمز درست، تا پایان قفل رد می‌شود
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    @DisplayName("بدون توکن، /admin/** برابر ۴۰۱ است")
    void unauthenticatedAdminIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────── کمکی ───────────────────────────

    private UUID insertSuperAdmin(String username, String rawPassword) {
        UUID userId = UuidV7Generator.generate();
        jdbc.update("""
                insert into users (id, username, password_hash, full_name, status)
                values (?, ?, ?, ?, 'ACTIVE')
                """,
                userId, username, passwordEncoder.encode(rawPassword), "ادمین " + unique());

        jdbc.update("""
                insert into user_roles (id, user_id, role_id, tenant_id)
                values (?, ?, ?, null)
                """,
                UuidV7Generator.generate(), userId, ROLE_SUPER_ADMIN);

        return userId;
    }

    private UUID insertTeacherTenant(UUID userId) {
        jdbc.update("insert into users (id, full_name) values (?, ?)",
                userId, "معلم " + unique());

        UUID tenantId = UuidV7Generator.generate();
        jdbc.update("""
                insert into tenants (id, type, name, slug, owner_user_id)
                values (?, 'TEACHER', ?, ?, ?)
                """,
                tenantId, "مستأجر " + unique(), "t-" + unique(), userId);
        return tenantId;
    }

    private static String unique() {
        return Integer.toString(SEQ.incrementAndGet());
    }
}
