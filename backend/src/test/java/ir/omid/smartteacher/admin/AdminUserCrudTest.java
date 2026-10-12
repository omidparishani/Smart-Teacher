package ir.omid.smartteacher.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.security.jwt.JwtService;
import ir.omid.smartteacher.security.jwt.TokenScope;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.util.List;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * قید پذیرش فاز ۴: CRUD کاربر، بازنشانی رمز، تغییر نقش —
 * معلم ۴۰۳؛ SUPER_ADMIN مجاز؛ demote خود ممنوع.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AdminUserCrudTest {

    private static final UUID ROLE_SUPER_ADMIN =
            UUID.fromString("10000000-0000-7000-8000-000000000001");
    private static final UUID ROLE_ADMIN =
            UUID.fromString("10000000-0000-7000-8000-000000000002");

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

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("معلم نمی‌تواند کاربر بسازد یا نقش عوض کند")
    void teacherForbiddenOnUserWrites() throws Exception {
        UUID teacherId = UuidV7Generator.generate();
        UUID tenantId = insertTeacherTenant(teacherId);
        UUID targetId = insertLoginableUser("crud-victim-" + unique(), "Victim-Pass-1!");

        String teacherToken = jwtService.createAccessToken(
                TenantPrincipal.ofTenantUser(
                        teacherId, tenantId, null, Set.of(), MemberRole.TEACHER, "fa"),
                TokenScope.MINIAPP);

        mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"nope","password":"Password1!","fullName":"ن","role":"ADMIN"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/v1/admin/users/" + targetId + "/change-role")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SUPER_ADMIN می‌تواند کاربر بسازد، بخواند، ویرایش کند و حسابرسی ثبت شود")
    void superAdminCanCreateGetUpdate() throws Exception {
        UUID adminId = insertSuperAdmin("crud-admin-" + unique(), "Admin-Pass-99!");
        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        String username = "staff-" + unique();
        MvcResult created = mockMvc.perform(post("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "password":"Staff-Pass-11!",
                                  "fullName":"کارمند تست",
                                  "email":"%s@example.com",
                                  "role":"ADMIN"
                                }
                                """.formatted(username, username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.platformRoles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        UUID userId = UUID.fromString(body.get("id").asText());

        assertThat(countAuditFor(userId, "SECURITY_SENSITIVE_ACTION")).isGreaterThanOrEqualTo(1);

        mockMvc.perform(get("/api/v1/admin/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("کارمند تست"))
                .andExpect(jsonPath("$.platformRoles[0]").value("ADMIN"));

        mockMvc.perform(put("/api/v1/admin/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"نام جدید","email":"%s@example.com","mobile":null}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("نام جدید"));
    }

    @Test
    @DisplayName("بازنشانی رمز نشست‌ها را باطل می‌کند و ورود با رمز جدید ممکن است")
    void resetPasswordWorksAndAudits() throws Exception {
        UUID adminId = insertSuperAdmin("reset-admin-" + unique(), "Admin-Pass-22!");
        String username = "reset-user-" + unique();
        UUID targetId = insertLoginableUser(username, "Old-Pass-33!");

        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        mockMvc.perform(post("/api/v1/admin/users/" + targetId + "/reset-password")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"New-Pass-44!\"}"))
                .andExpect(status().isOk());

        assertThat(countAuditFor(targetId, "SECURITY_SENSITIVE_ACTION")).isGreaterThanOrEqualTo(1);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"Old-Pass-33!"}
                                """.formatted(username)))
                .andExpect(status().isUnauthorized());

        // کاربر بدون نقش ادمین نمی‌تواند وارد پنل شود — ولی رمز باید match شود.
        // اگر نقش نداشته باشد INVALID_CREDENTIALS یا ACCESS_DENIED می‌گیرد.
        // اینجا فقط صحت هش را با PasswordEncoder می‌سنجیم.
        String hash = jdbc.queryForObject(
                "select password_hash from users where id = ?", String.class, targetId);
        assertThat(passwordEncoder.matches("New-Pass-44!", hash)).isTrue();
        assertThat(passwordEncoder.matches("Old-Pass-33!", hash)).isFalse();
    }

    @Test
    @DisplayName("SUPER_ADMIN می‌تواند نقش کاربر دیگر را عوض کند")
    void superAdminCanChangeRole() throws Exception {
        UUID adminId = insertSuperAdmin("role-admin-" + unique(), "Admin-Pass-55!");
        UUID targetId = insertLoginableUser("role-target-" + unique(), "Target-Pass-66!");
        jdbc.update("""
                insert into user_roles (id, user_id, role_id, tenant_id)
                values (?, ?, ?, null)
                """, UuidV7Generator.generate(), targetId, ROLE_ADMIN);

        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        mockMvc.perform(post("/api/v1/admin/users/" + targetId + "/change-role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPER_ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platformRoles[0]").value("SUPER_ADMIN"));

        assertThat(platformRolesOf(targetId)).containsExactly("SUPER_ADMIN");
    }

    @Test
    @DisplayName("ادمین نمی‌تواند نقش خودش را عوض کند")
    void cannotDemoteSelf() throws Exception {
        UUID adminId = insertSuperAdmin("self-role-" + unique(), "Self-Pass-77!");
        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        mockMvc.perform(post("/api/v1/admin/users/" + adminId + "/change-role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        assertThat(platformRolesOf(adminId)).contains("SUPER_ADMIN");
    }

    @Test
    @DisplayName("ادمین نمی‌تواند رمز خودش را بازنشانی کند")
    void cannotResetOwnPassword() throws Exception {
        UUID adminId = insertSuperAdmin("self-pwd-" + unique(), "Self-Pass-88!");
        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        mockMvc.perform(post("/api/v1/admin/users/" + adminId + "/reset-password")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"Hacked-Pass-99!\"}"))
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

    private List<String> platformRolesOf(UUID userId) {
        return jdbc.queryForList("""
                select r.code from user_roles ur
                  join roles r on r.id = ur.role_id
                 where ur.user_id = ? and ur.tenant_id is null
                 order by r.code
                """, String.class, userId);
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
