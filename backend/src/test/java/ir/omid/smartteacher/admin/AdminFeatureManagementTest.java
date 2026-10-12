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
 * Feature Flags — فهرست، به‌روزرسانی پیش‌فرض، override مستأجر/کاربر.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AdminFeatureManagementTest {

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
    @DisplayName("معلم به /admin/features دسترسی ندارد")
    void teacherCannotAccessFeatures() throws Exception {
        UUID teacherId = UuidV7Generator.generate();
        UUID tenantId = insertTeacherTenant(teacherId);

        String teacherToken = jwtService.createAccessToken(
                TenantPrincipal.ofTenantUser(
                        teacherId, tenantId, null, Set.of(), MemberRole.TEACHER, "fa"),
                TokenScope.MINIAPP);

        mockMvc.perform(get("/api/v1/admin/features")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/admin/features/AI_CHAT")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"defaultEnabled\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SUPER_ADMIN فهرست امکانات seedشده را می‌بیند")
    void superAdminListsSeededFeatures() throws Exception {
        UUID adminId = insertSuperAdmin("feat-list-" + unique(), "Admin-Pass-11!");
        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        mockMvc.perform(get("/api/v1/admin/features")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code=='AI_CHAT')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.code=='MULTIPLE_CLASSES')].defaultEnabled")
                        .value(org.hamcrest.Matchers.hasItem(true)));
    }

    @Test
    @DisplayName("SUPER_ADMIN می‌تواند پیش‌فرض امکان را عوض کند و حسابرسی ثبت شود")
    void superAdminCanUpdateFeatureDefault() throws Exception {
        UUID adminId = insertSuperAdmin("feat-upd-" + unique(), "Admin-Pass-22!");
        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        mockMvc.perform(put("/api/v1/admin/features/OCR")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"defaultEnabled\":true,\"name\":\"OCR به‌روز\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OCR"))
                .andExpect(jsonPath("$.defaultEnabled").value(true))
                .andExpect(jsonPath("$.name").value("OCR به‌روز"));

        assertThat(jdbc.queryForObject(
                "select default_enabled from features where code = 'OCR'", Boolean.class))
                .isTrue();

        Long audits = jdbc.queryForObject("""
                select count(*) from audit_logs
                 where action = 'SECURITY_SENSITIVE_ACTION'
                   and metadata::text like '%FEATURE_UPDATED%'
                """, Long.class);
        assertThat(audits).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("SUPER_ADMIN می‌تواند override مستأجر و کاربر بسازد")
    void superAdminCanCreateOverrides() throws Exception {
        UUID adminId = insertSuperAdmin("feat-ov-" + unique(), "Admin-Pass-33!");
        UUID ownerId = insertLoginableUser("feat-owner-" + unique(), "Owner-Pass-44!");
        UUID tenantId = UuidV7Generator.generate();
        jdbc.update("""
                insert into tenants (id, type, name, slug, owner_user_id)
                values (?, 'TEACHER', ?, ?, ?)
                """, tenantId, "مستأجر فیچر", "feat-" + unique(), ownerId);

        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        mockMvc.perform(post("/api/v1/admin/features/overrides")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scope":"TENANT",
                                  "tenantId":"%s",
                                  "featureCode":"AI_CHAT",
                                  "enabled":true,
                                  "reason":"آزمایش پنل"
                                }
                                """.formatted(tenantId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("TENANT"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.featureCode").value("AI_CHAT"));

        mockMvc.perform(post("/api/v1/admin/features/overrides")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scope":"USER",
                                  "userId":"%s",
                                  "featureCode":"AI_CHAT",
                                  "enabled":false,
                                  "reason":"محدودیت فردی"
                                }
                                """.formatted(ownerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("USER"))
                .andExpect(jsonPath("$.enabled").value(false));

        assertThat(jdbc.queryForObject("""
                select enabled from feature_overrides
                 where tenant_id = ? and feature_code = 'AI_CHAT'
                """, Boolean.class, tenantId)).isTrue();

        assertThat(jdbc.queryForObject("""
                select enabled from user_feature_overrides
                 where user_id = ? and feature_code = 'AI_CHAT'
                """, Boolean.class, ownerId)).isFalse();
    }

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

    private static String unique() {
        return Integer.toString(SEQ.incrementAndGet());
    }
}
