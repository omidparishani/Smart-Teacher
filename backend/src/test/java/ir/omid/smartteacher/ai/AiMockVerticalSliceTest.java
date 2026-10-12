package ir.omid.smartteacher.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import ir.omid.smartteacher.ai.internal.application.AiChatService;
import ir.omid.smartteacher.billing.internal.api.dto.CheckoutRequest;
import ir.omid.smartteacher.billing.internal.application.BillingService;
import ir.omid.smartteacher.billing.internal.domain.Plan;
import ir.omid.smartteacher.billing.internal.domain.PlanRepository;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import ir.omid.smartteacher.tenant.TenantContext;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * برش عمودی فاز ۵ گام ۱ — Mock AI بدون حدس API واقعی Provider.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AiMockVerticalSliceTest {

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
    private AiProvider aiProvider;

    @Autowired
    private BillingService billingService;

    @Autowired
    private PlanRepository planRepository;

    @Test
    @DisplayName("MockAiProvider در پروفایل تست فعال است")
    void mockProviderIsAvailable() {
        assertThat(aiProvider.code()).isEqualTo("MOCK");
        assertThat(aiProvider.isAvailable()).isTrue();

        AiProvider.AiResponse response = aiProvider.chat(
                AiProvider.AiRequest.ofUserPrompt("mock-chat", "سلام معلم"));
        assertThat(response.content()).contains("سلام معلم");
        assertThat(response.inputTokens()).isPositive();
        assertThat(response.outputTokens()).isPositive();
    }

    @Test
    @DisplayName("معلم می‌تواند chat کند و رکورد مصرف ساخته می‌شود")
    void teacherCanChatAndUsageIsRecorded() throws Exception {
        UUID teacherId = UuidV7Generator.generate();
        UUID tenantId = insertTeacherTenant(teacherId);
        activateProSubscription(teacherId, tenantId);
        String teacherToken = teacherToken(teacherId, tenantId);

        long before = countUsage(tenantId);

        MvcResult result = mockMvc.perform(post("/api/v1/ai/chat")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"یک سؤال آزمایشی درباره کسر","model":"mock-chat"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isString())
                .andExpect(jsonPath("$.provider").value("MOCK"))
                .andExpect(jsonPath("$.model").value("mock-chat"))
                .andExpect(jsonPath("$.promptTokens").isNumber())
                .andExpect(jsonPath("$.completionTokens").isNumber())
                .andExpect(jsonPath("$.usageRecordId").isString())
                .andReturn();

        String usageId = JsonPath.read(result.getResponse().getContentAsString(), "$.usageRecordId");
        assertThat(countUsage(tenantId)).isEqualTo(before + 1);

        Integer rows = jdbc.queryForObject(
                """
                select count(*) from ai_usage_records
                where id = ?::uuid and tenant_id = ? and feature = ?
                """,
                Integer.class,
                usageId, tenantId, AiChatService.FEATURE_TEACHER_CHAT);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    @DisplayName("دانش‌آموز به /ai/chat دسترسی ندارد")
    void studentCannotChat() throws Exception {
        UUID studentUserId = UuidV7Generator.generate();
        UUID tenantId = insertTeacherTenant(studentUserId);

        String studentToken = jwtService.createAccessToken(
                TenantPrincipal.ofTenantUser(
                        studentUserId, tenantId, null, Set.of(), MemberRole.STUDENT, "fa"),
                TokenScope.MINIAPP);

        mockMvc.perform(post("/api/v1/ai/chat")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"تلاش دانش‌آموز"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("apiKeys_neverLeak_inAnyResponse — کلید خام در پاسخ ادمین نیست")
    void apiKeys_neverLeak_inAnyResponse() throws Exception {
        String username = "ai-admin-" + unique();
        UUID adminId = insertSuperAdmin(username);
        String adminToken = jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);

        String rawKey = "sk-test-NEVER-LEAK-THIS-KEY-XYZ9";

        MvcResult create = mockMvc.perform(post("/api/v1/admin/ai/providers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"mock-leak-%s",
                                  "name":"Mock Leak Check",
                                  "type":"MOCK",
                                  "baseUrl":"https://example.invalid/mock",
                                  "apiKey":"%s",
                                  "enabled":true
                                }
                                """.formatted(unique(), rawKey)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenHint").isString())
                .andExpect(jsonPath("$.apiKey").doesNotExist())
                .andExpect(jsonPath("$.api_key").doesNotExist())
                .andExpect(jsonPath("$.apiKeyEncrypted").doesNotExist())
                .andReturn();

        String createBody = create.getResponse().getContentAsString();
        assertThat(createBody).doesNotContain(rawKey);
        assertThat(createBody).doesNotContain("NEVER-LEAK-THIS-KEY");

        String hint = JsonPath.read(createBody, "$.tokenHint");
        assertThat(hint).isNotBlank();
        assertThat(hint).doesNotContain(rawKey);
        assertThat(hint).endsWith("XYZ9");

        MvcResult list = mockMvc.perform(get("/api/v1/admin/ai/providers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String listBody = list.getResponse().getContentAsString();
        assertThat(listBody).doesNotContain(rawKey);
        assertThat(listBody).doesNotContain("NEVER-LEAK-THIS-KEY");
        assertThat(listBody).contains("tokenHint");
        assertThat(listBody).doesNotContain("\"apiKey\"");

        // مدل‌ها هم نباید کلید داشته باشند (فهرست خالی مجاز است)
        MvcResult models = mockMvc.perform(get("/api/v1/admin/ai/models")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(models.getResponse().getContentAsString()).doesNotContain(rawKey);

        // در دیتابیس باید رمزشده باشد، نه متن خام
        String stored = jdbc.queryForObject(
                "select api_key_encrypted from ai_providers where code like 'mock-leak-%' limit 1",
                String.class);
        assertThat(stored).isNotBlank();
        assertThat(stored).doesNotContain(rawKey);
    }

    // ─────────────────────────── کمکی ───────────────────────────

    private String teacherToken(UUID teacherId, UUID tenantId) {
        return jwtService.createAccessToken(
                TenantPrincipal.ofTenantUser(
                        teacherId, tenantId, null, Set.of(), MemberRole.TEACHER, "fa"),
                TokenScope.MINIAPP);
    }

    private long countUsage(UUID tenantId) {
        Long count = jdbc.queryForObject(
                "select count(*) from ai_usage_records where tenant_id = ?",
                Long.class,
                tenantId);
        return count == null ? 0L : count;
    }

    private UUID insertSuperAdmin(String username) {
        UUID userId = UuidV7Generator.generate();
        jdbc.update("""
                insert into users (id, username, password_hash, full_name, status)
                values (?, ?, ?, ?, 'ACTIVE')
                """,
                userId, username, "{noop}unused", "ادمین AI " + unique());

        jdbc.update("""
                insert into user_roles (id, user_id, role_id, tenant_id)
                values (?, ?, ?, null)
                """,
                UuidV7Generator.generate(), userId, ROLE_SUPER_ADMIN);
        return userId;
    }

    private void activateProSubscription(UUID userId, UUID tenantId) {
        Plan pro = planRepository.findByCode("PRO").orElseThrow();
        TenantContext.callAs(
                TenantPrincipal.ofTenantUser(
                        userId, tenantId, null, Set.of(), MemberRole.TEACHER, "fa"),
                () -> {
                    billingService.checkout(new CheckoutRequest(pro.getId()));
                    return null;
                });
    }

    private UUID insertTeacherTenant(UUID userId) {
        jdbc.update("insert into users (id, full_name) values (?, ?)",
                userId, "کاربر AI " + unique());

        UUID tenantId = UuidV7Generator.generate();
        jdbc.update("""
                insert into tenants (id, type, name, slug, owner_user_id)
                values (?, 'TEACHER', ?, ?, ?)
                """,
                tenantId, "مستأجر AI " + unique(), "ai-t-" + unique(), userId);
        return tenantId;
    }

    private static String unique() {
        return Integer.toString(SEQ.incrementAndGet());
    }
}
