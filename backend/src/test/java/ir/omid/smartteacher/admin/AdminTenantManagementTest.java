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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * مدیریت مستأجر — معلم ۴۰۳؛ SUPER_ADMIN داده واقعی از tenants می‌بیند.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AdminTenantManagementTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("معلم به /admin/tenants دسترسی ندارد")
    void teacherCannotAccessTenants() throws Exception {
        UUID teacherId = UuidV7Generator.generate();
        UUID tenantId = insertTeacherTenant(teacherId);

        String teacherToken = jwtService.createAccessToken(
                TenantPrincipal.ofTenantUser(
                        teacherId, tenantId, null, Set.of(), MemberRole.TEACHER, "fa"),
                TokenScope.MINIAPP);

        mockMvc.perform(get("/api/v1/admin/tenants")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/v1/admin/tenants")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"TEACHER","name":"x","slug":"x-%s","ownerUserId":"%s"}
                                """.formatted(unique(), teacherId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SUPER_ADMIN می‌تواند مستأجر بسازد، بخواند و ویرایش کند")
    void superAdminCanManageTenants() throws Exception {
        UUID adminId = insertSuperAdmin("tenant-admin-" + unique(), "Admin-Pass-11!");
        UUID ownerId = insertLoginableUser("owner-" + unique(), "Owner-Pass-22!");

        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        String slug = "school-" + unique();
        MvcResult created = mockMvc.perform(post("/api/v1/admin/tenants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type":"SCHOOL",
                                  "name":"مدرسه تست",
                                  "slug":"%s",
                                  "ownerUserId":"%s",
                                  "settings":"{\\"locale\\":\\"fa\\"}"
                                }
                                """.formatted(slug, ownerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value(slug))
                .andExpect(jsonPath("$.type").value("SCHOOL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        UUID tenantId = UUID.fromString(body.get("id").asText());

        mockMvc.perform(get("/api/v1/admin/tenants/" + tenantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("مدرسه تست"));

        mockMvc.perform(put("/api/v1/admin/tenants/" + tenantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"مدرسه به‌روز","status":"SUSPENDED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("مدرسه به‌روز"))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        assertThat(jdbc.queryForObject(
                "select status from tenants where id = ?", String.class, tenantId))
                .isEqualTo("SUSPENDED");

        mockMvc.perform(get("/api/v1/admin/tenants")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='%s')].slug", tenantId)
                        .value(org.hamcrest.Matchers.hasItem(slug)));
    }

    @Test
    @DisplayName("مستأجر موجود در فهرست ادمین دیده می‌شود — داده واقعی نه جعلی")
    void existingTenantAppearsInList() throws Exception {
        UUID adminId = insertSuperAdmin("list-admin-" + unique(), "Admin-Pass-33!");
        UUID ownerId = insertLoginableUser("list-owner-" + unique(), "Owner-Pass-44!");
        UUID tenantId = UuidV7Generator.generate();
        String slug = "existing-" + unique();
        jdbc.update("""
                insert into tenants (id, type, name, slug, owner_user_id)
                values (?, 'TEACHER', ?, ?, ?)
                """, tenantId, "مستأجر موجود", slug, ownerId);

        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        mockMvc.perform(get("/api/v1/admin/tenants/" + tenantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug))
                .andExpect(jsonPath("$.ownerUserId").value(ownerId.toString()));
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
