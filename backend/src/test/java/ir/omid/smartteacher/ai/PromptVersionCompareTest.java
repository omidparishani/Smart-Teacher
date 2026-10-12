package ir.omid.smartteacher.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ir.omid.smartteacher.ai.internal.application.AdminPromptService;
import ir.omid.smartteacher.security.jwt.JwtService;
import ir.omid.smartteacher.security.jwt.TokenScope;
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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PromptVersionCompareTest {

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
    private AdminPromptService promptService;

    @Test
    @DisplayName("compareVersions — طول و diff خطی")
    void compareVersions_serviceAndEndpoint() throws Exception {
        String adminToken = adminToken();

        MvcResult created = mockMvc.perform(post("/api/v1/admin/prompts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code":"CMP_%s",
                                  "name":"Compare",
                                  "featureCode":"CMP_F_%s"
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
                                  "systemPrompt":"line1\\nline2",
                                  "userPrompt":"{{msg}}",
                                  "variables":"{}"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String v1Id = objectMapper.readTree(v1.getResponse().getContentAsString()).path("id").asString();

        MvcResult v2 = mockMvc.perform(post("/api/v1/admin/prompts/" + promptId + "/versions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "systemPrompt":"line1\\nline2 changed",
                                  "userPrompt":"{{msg}}",
                                  "variables":"{}"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String v2Id = objectMapper.readTree(v2.getResponse().getContentAsString()).path("id").asString();

        UUID left = UUID.fromString(v1Id);
        UUID right = UUID.fromString(v2Id);
        var compare = promptService.compareVersions(left, right);
        assertThat(compare.systemPrompt().lengthDelta()).isGreaterThan(0);
        assertThat(compare.systemPrompt().lineDiffs())
                .anyMatch(d -> "CHANGED".equals(d.type()));

        mockMvc.perform(get("/api/v1/admin/prompts/versions/compare")
                        .param("left", v1Id)
                        .param("right", v2Id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leftVersionId").value(v1Id))
                .andExpect(jsonPath("$.systemPrompt.lengthDelta").isNumber());
    }

    private String adminToken() {
        UUID adminId = insertSuperAdmin("cmp-admin-" + unique());
        return jwtService.createAccessToken(
                TenantPrincipal.ofPlatformUser(adminId, Set.of("SUPER_ADMIN"), "fa"),
                TokenScope.ADMIN);
    }

    private UUID insertSuperAdmin(String username) {
        UUID userId = UUID.randomUUID();
        jdbc.update("""
                insert into users (id, username, password_hash, full_name, status)
                values (?, ?, ?, ?, 'ACTIVE')
                """,
                userId, username, "{noop}unused", "Admin " + unique());
        jdbc.update("""
                insert into user_roles (id, user_id, role_id, tenant_id)
                values (?, ?, ?, null)
                """,
                UUID.randomUUID(), userId, ROLE_SUPER_ADMIN);
        return userId;
    }

    private static String unique() {
        return Integer.toString(SEQ.incrementAndGet());
    }
}
