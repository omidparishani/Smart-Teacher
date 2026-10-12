package ir.omid.smartteacher.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.security.jwt.JwtService;
import ir.omid.smartteacher.security.jwt.TokenScope;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantPrincipal;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * قید پذیرش فاز ۴ گام ۲: معلم نمی‌تواند وضعیت کاربر عوض کند؛
 * {@code SUPER_ADMIN} می‌تواند؛ کاربر غیرفعال وارد نمی‌شود.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AdminUserManagementTest {

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
    @DisplayName("معلم نمی‌تواند کاربر را غیرفعال کند")
    void teacherCannotDisableUser() throws Exception {
        UUID teacherId = UuidV7Generator.generate();
        UUID tenantId = insertTeacherTenant(teacherId);
        UUID targetId = insertPlainUser("victim-" + unique());

        String teacherToken = jwtService.createAccessToken(
                TenantPrincipal.ofTenantUser(
                        teacherId, tenantId, null, Set.of(), MemberRole.TEACHER, "fa"),
                TokenScope.MINIAPP);

        mockMvc.perform(post("/api/v1/admin/users/" + targetId + "/disable")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        assertThat(statusOf(targetId)).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("SUPER_ADMIN می‌تواند disable / enable / suspend کند و حسابرسی ثبت می‌شود")
    void superAdminCanChangeUserStatusWithAudit() throws Exception {
        UUID adminId = insertSuperAdmin("mgr-" + unique(), "Mgr-Pass-99!");
        UUID targetId = insertLoginableUser("target-" + unique(), "Target-Pass-11!");

        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        mockMvc.perform(post("/api/v1/admin/users/" + targetId + "/disable")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        assertThat(statusOf(targetId)).isEqualTo("DISABLED");
        assertThat(countAuditFor(targetId, "SECURITY_SENSITIVE_ACTION")).isGreaterThanOrEqualTo(1);

        mockMvc.perform(post("/api/v1/admin/users/" + targetId + "/enable")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/admin/users/" + targetId + "/suspend")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"تخلف از قوانین استفاده\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        assertThat(statusOf(targetId)).isEqualTo("SUSPENDED");
        assertThat(countAuditFor(targetId, "SECURITY_SENSITIVE_ACTION")).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("کاربر غیرفعال‌شده نمی‌تواند با رمز وارد شود")
    void disabledUserCannotLogin() throws Exception {
        String username = "disabled-" + unique();
        String password = "Still-Has-Pass-1!";
        UUID userId = insertLoginableUser(username, password);

        UUID adminId = insertSuperAdmin("disabler-" + unique(), "Admin-Pass-22!");
        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        mockMvc.perform(post("/api/v1/admin/users/" + userId + "/disable")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    @DisplayName("کاربر معلق‌شده نمی‌تواند با رمز وارد شود")
    void suspendedUserCannotLogin() throws Exception {
        String username = "suspended-" + unique();
        String password = "Still-Has-Pass-2!";
        UUID userId = insertLoginableUser(username, password);

        UUID adminId = insertSuperAdmin("suspender-" + unique(), "Admin-Pass-33!");
        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        mockMvc.perform(post("/api/v1/admin/users/" + userId + "/suspend")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"بررسی امنیتی\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    @DisplayName("تعلیق بدون دلیل ۴۰۰ می‌دهد")
    void suspendRequiresReason() throws Exception {
        UUID adminId = insertSuperAdmin("reason-" + unique(), "Admin-Pass-44!");
        UUID targetId = insertPlainUser("noreason-" + unique());

        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        mockMvc.perform(post("/api/v1/admin/users/" + targetId + "/suspend")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("ادمین نمی‌تواند حساب خودش را غیرفعال کند")
    void cannotDisableSelf() throws Exception {
        UUID adminId = insertSuperAdmin("self-" + unique(), "Self-Pass-55!");
        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        mockMvc.perform(post("/api/v1/admin/users/" + adminId + "/disable")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    // ─────────────────────────── کمکی ───────────────────────────

    private UUID insertSuperAdmin(String username, String rawPassword) {
        UUID userId = insertLoginableUser(username, rawPassword);
        jdbc.update("""
                insert into user_roles (id, user_id, role_id, tenant_id)
                values (?, ?, ?, null)
                """,
                UuidV7Generator.generate(), userId, ROLE_SUPER_ADMIN);
        return userId;
    }

    private UUID insertLoginableUser(String username, String rawPassword) {
        UUID userId = UuidV7Generator.generate();
        jdbc.update("""
                insert into users (id, username, password_hash, full_name, status)
                values (?, ?, ?, ?, 'ACTIVE')
                """,
                userId, username, passwordEncoder.encode(rawPassword), "کاربر " + unique());
        return userId;
    }

    private UUID insertPlainUser(String username) {
        UUID userId = UuidV7Generator.generate();
        jdbc.update("""
                insert into users (id, username, full_name, status)
                values (?, ?, ?, 'ACTIVE')
                """,
                userId, username, "کاربر ساده " + unique());
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

    private String statusOf(UUID userId) {
        return jdbc.queryForObject(
                "select status from users where id = ?", String.class, userId);
    }

    private long countAuditFor(UUID entityId, String action) {
        Long count = jdbc.queryForObject("""
                select count(*) from audit_logs
                 where entity_id = ? and action = ?
                """, Long.class, entityId, action);
        return count == null ? 0L : count;
    }

    private static String unique() {
        return Integer.toString(SEQ.incrementAndGet());
    }
}
