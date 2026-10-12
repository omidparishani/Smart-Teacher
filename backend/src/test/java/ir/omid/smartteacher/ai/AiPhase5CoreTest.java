package ir.omid.smartteacher.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ir.omid.smartteacher.ai.internal.MockAiProvider;
import ir.omid.smartteacher.ai.internal.SecondaryMockAiProvider;
import ir.omid.smartteacher.ai.internal.application.PromptRenderer;
import ir.omid.smartteacher.ai.internal.application.PromptResolver;
import ir.omid.smartteacher.ai.internal.application.ProviderBroker;
import ir.omid.smartteacher.ai.internal.domain.AiScopeType;
import ir.omid.smartteacher.ai.internal.domain.PromptVersionEntity;
import ir.omid.smartteacher.ai.internal.domain.ResolutionContext;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.security.jwt.JwtService;
import ir.omid.smartteacher.security.jwt.TokenScope;
import ir.omid.smartteacher.tenant.MemberRole;
import ir.omid.smartteacher.tenant.TenantPrincipal;
import java.util.Map;
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
 * فاز ۵ — Resolver، Failover، Publish/Rollback، عدم نشت کلید.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AiPhase5CoreTest {

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
    private ObjectMapper objectMapper;
    @Autowired
    private PromptResolver promptResolver;
    @Autowired
    private PromptRenderer promptRenderer;
    @Autowired
    private ProviderBroker providerBroker;
    @Autowired
    private MockAiProvider mockAiProvider;
    @Autowired
    private SecondaryMockAiProvider secondaryMockAiProvider;

    @Test
    @DisplayName("teacher_cannotModifyGlobalProviderConfig — معلم ۴۰۳ روی POST /admin/ai/providers")
    void teacher_cannotModifyGlobalProviderConfig() throws Exception {
        UUID teacherId = UuidV7Generator.generate();
        UUID tenantId = insertTeacherTenant(teacherId);
        String teacherToken = jwtService.createAccessToken(
                TenantPrincipal.ofTenantUser(
                        teacherId, tenantId, null, Set.of(), MemberRole.TEACHER, "fa"),
                TokenScope.MINIAPP);

        mockMvc.perform(post("/api/v1/admin/ai/providers")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"x","name":"x","type":"MOCK","apiKey":"secret"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("student_cannotAccessTeacherEndpoints — دانش‌آموز به chat و کلاس دسترسی ندارد")
    void student_cannotAccessTeacherEndpoints() throws Exception {
        UUID studentUserId = UuidV7Generator.generate();
        UUID tenantId = insertTeacherTenant(studentUserId);
        String studentToken = jwtService.createAccessToken(
                TenantPrincipal.ofTenantUser(
                        studentUserId, tenantId, null, Set.of(), MemberRole.STUDENT, "fa"),
                TokenScope.MINIAPP);

        mockMvc.perform(post("/api/v1/ai/chat")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"سلام\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/classes")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PromptResolver — GLOBAL برای TEACHER_CHAT پیدا می‌شود")
    void promptResolver_globalLevel() {
        UUID tenantId = UuidV7Generator.generate();
        ResolutionContext ctx = new ResolutionContext(
                tenantId, null, null, null, "TEACHER_CHAT");
        PromptResolver.ResolvedPrompt resolved =
                promptResolver.resolve("TEACHER_CHAT", ctx);
        assertThat(resolved.scope()).isEqualTo(AiScopeType.GLOBAL);
        assertThat(resolved.version().getSystemPrompt()).isNotBlank();
    }

    @Test
    @DisplayName("PromptResolver — نبود پرامپت Fail Loud")
    void promptResolver_failLoud() {
        ResolutionContext ctx = new ResolutionContext(
                UuidV7Generator.generate(), null, null, null, "NO_SUCH_FEATURE");
        assertThatThrownBy(() -> promptResolver.resolve("NO_SUCH_FEATURE", ctx))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("PromptRenderer — quoteReplacement و UNTRUSTED")
    void promptRenderer_quoteReplacement() {
        PromptVersionEntity version = new PromptVersionEntity(
                UuidV7Generator.generate(), 1,
                "سیستم",
                "پاسخ: {{answer}} و کد $1");
        version.setVariables("""
                {"answer":{"type":"UNTRUSTED_TEXT","required":true,"maxLength":500}}
                """);

        var rendered = promptRenderer.render(version, Map.of("answer", "مقدار $1 خطرناک"));
        assertThat(rendered.userPrompt()).contains("<student_answer>");
        assertThat(rendered.userPrompt()).contains("مقدار $1 خطرناک");
        assertThat(rendered.userPrompt()).doesNotContain("</student_answer></student_answer>");
    }

    @Test
    @DisplayName("ProviderBroker failover بین دو Mock")
    void providerBroker_failoverBetweenTwoMocks() {
        mockAiProvider.armFailure();
        ResolutionContext ctx = new ResolutionContext(
                UuidV7Generator.generate(), null, null, null, "TEACHER_CHAT");
        AiProvider.AiResponse response = providerBroker.execute(
                "TEACHER_CHAT",
                ctx,
                AiProvider.AiRequest.ofUserPrompt("mock-chat", "failover-test"));
        assertThat(response.content()).contains("failover-test");
        assertThat(response.rawMetadata()).containsEntry("secondary", true);
    }

    @Test
    @DisplayName("ادمین — publish/rollback و تست نسخه؛ کلید نشت نمی‌کند")
    void admin_publishRollbackAndTest_noKeyLeak() throws Exception {
        String username = "prompt-admin-" + unique();
        UUID adminId = insertSuperAdmin(username);
        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        MvcResult created = mockMvc.perform(post("/api/v1/admin/prompts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"TEST_PROMPT_%s",
                                  "name":"تست",
                                  "featureCode":"TEST_FEATURE_%s",
                                  "description":"d"
                                }
                                """.formatted(unique(), unique())))
                .andExpect(status().isCreated())
                .andReturn();

        String promptId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("id").asString();

        MvcResult v1 = mockMvc.perform(post("/api/v1/admin/prompts/" + promptId + "/versions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "systemPrompt":"سیستم v1",
                                  "userPrompt":"{{message}}",
                                  "variables":"{\\"message\\":{\\"type\\":\\"STRING\\",\\"required\\":true}}"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String v1Id = objectMapper.readTree(v1.getResponse().getContentAsString()).path("id").asString();

        mockMvc.perform(post("/api/v1/admin/prompts/versions/" + v1Id + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        MvcResult v2 = mockMvc.perform(post("/api/v1/admin/prompts/" + promptId + "/versions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "systemPrompt":"سیستم v2",
                                  "userPrompt":"{{message}}",
                                  "variables":"{\\"message\\":{\\"type\\":\\"STRING\\",\\"required\\":true}}"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String v2Id = objectMapper.readTree(v2.getResponse().getContentAsString()).path("id").asString();

        mockMvc.perform(post("/api/v1/admin/prompts/versions/" + v2Id + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/admin/prompts/" + promptId + "/versions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + v1Id + "')].status").value("ARCHIVED"));

        mockMvc.perform(post("/api/v1/admin/prompts/versions/" + v1Id + "/rollback")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(post("/api/v1/admin/prompts/versions/" + v1Id + "/test")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variables\":{\"message\":\"سلام playground\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawResponse").isString())
                .andExpect(jsonPath("$.renderedUserPrompt").value("سلام playground"));

        String rawKey = "sk-phase5-NEVER-LEAK-" + unique();
        MvcResult provider = mockMvc.perform(post("/api/v1/admin/ai/providers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"mock-p5-%s",
                                  "name":"Mock P5",
                                  "type":"MOCK",
                                  "apiKey":"%s",
                                  "enabled":true
                                }
                                """.formatted(unique(), rawKey)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode providerJson = objectMapper.readTree(provider.getResponse().getContentAsString());
        assertThat(provider.getResponse().getContentAsString()).doesNotContain(rawKey);
        String providerId = providerJson.path("id").asString();

        mockMvc.perform(post("/api/v1/admin/ai/providers/" + providerId + "/test")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    @DisplayName("حذف نرم کاربر — خودحذف ممنوع؛ مدارس CRUD")
    void softDeleteUser_andSchools() throws Exception {
        String adminUser = "sd-admin-" + unique();
        UUID adminId = insertSuperAdmin(adminUser);
        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        mockMvc.perform(delete("/api/v1/admin/users/" + adminId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        UUID victim = insertLoginableUser("victim-" + unique());
        mockMvc.perform(delete("/api/v1/admin/users/" + victim)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        Integer deleted = jdbc.queryForObject(
                "select count(*) from users where id = ? and deleted_at is not null",
                Integer.class, victim);
        assertThat(deleted).isEqualTo(1);

        mockMvc.perform(post("/api/v1/admin/schools")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"مدرسه تست",
                                  "slug":"school-%s",
                                  "ownerUserId":"%s"
                                }
                                """.formatted(unique(), adminId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("مدرسه تست"));

        mockMvc.perform(get("/api/v1/admin/schools")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        mockMvc.perform(get("/api/v1/admin/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.headerName").isString());
    }

    private UUID insertSuperAdmin(String username) {
        UUID userId = UuidV7Generator.generate();
        jdbc.update("""
                insert into users (id, username, password_hash, full_name, status)
                values (?, ?, ?, ?, 'ACTIVE')
                """,
                userId, username, "{noop}unused", "ادمین " + unique());
        jdbc.update("""
                insert into user_roles (id, user_id, role_id, tenant_id)
                values (?, ?, ?, null)
                """,
                UuidV7Generator.generate(), userId, ROLE_SUPER_ADMIN);
        return userId;
    }

    private UUID insertLoginableUser(String username) {
        UUID userId = UuidV7Generator.generate();
        jdbc.update("""
                insert into users (id, username, password_hash, full_name, status)
                values (?, ?, ?, ?, 'ACTIVE')
                """,
                userId, username, "{noop}unused", "کاربر " + unique());
        return userId;
    }

    private UUID insertTeacherTenant(UUID userId) {
        jdbc.update("insert into users (id, full_name) values (?, ?)",
                userId, "کاربر " + unique());
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
