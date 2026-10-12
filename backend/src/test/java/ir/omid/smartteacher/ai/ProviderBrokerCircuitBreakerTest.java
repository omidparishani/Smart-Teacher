package ir.omid.smartteacher.ai;

import static org.assertj.core.api.Assertions.assertThat;

import ir.omid.smartteacher.ai.internal.MockAiProvider;
import ir.omid.smartteacher.ai.internal.application.ProviderBroker;
import ir.omid.smartteacher.ai.internal.application.ProviderCircuitBreaker;
import ir.omid.smartteacher.ai.internal.application.ProviderCircuitBreakerRegistry;
import ir.omid.smartteacher.ai.internal.domain.ResolutionContext;
import ir.omid.smartteacher.common.util.UuidV7Generator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Circuit Breaker روی {@link ProviderBroker} — fail-fast و failover به Mock ثانویه.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestPropertySource(properties = {
        "app.ai.circuit-breaker.failure-threshold=3",
        "app.ai.circuit-breaker.cooldown-ms=60000"
})
class ProviderBrokerCircuitBreakerTest {

    static final String POSTGRES_IMAGE =
            System.getProperty("test.postgres.image", "postgres:17-alpine");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE));

    @Autowired
    private ProviderBroker providerBroker;
    @Autowired
    private MockAiProvider mockAiProvider;
    @Autowired
    private ProviderCircuitBreakerRegistry circuitBreakers;

    @BeforeEach
    void reset() {
        mockAiProvider.disarmPersistentFailure();
        mockAiProvider.resetInvocationCountForTest();
        circuitBreakers.resetAllForTest();
    }

    @Test
    @DisplayName("پس از N شکست Circuit OPEN می‌شود و primary fail-fast — secondary موفق")
    void circuitOpensAfterFailures_failFastThenSecondary() {
        mockAiProvider.armPersistentFailure();
        ResolutionContext ctx = new ResolutionContext(
                UuidV7Generator.generate(), null, null, null, "TEACHER_CHAT");
        AiProvider.AiRequest request =
                AiProvider.AiRequest.ofUserPrompt("mock-chat", "circuit-test");

        for (int i = 0; i < 3; i++) {
            AiProvider.AiResponse response = providerBroker.execute("TEACHER_CHAT", ctx, request);
            assertThat(response.content()).contains("circuit-test");
        }

        ProviderCircuitBreaker breaker = circuitBreakers.forProvider(MockAiProvider.CODE);
        assertThat(breaker.state()).isEqualTo(ProviderCircuitBreaker.State.OPEN);

        int invocationsBefore = mockAiProvider.generateInvocationCount();
        AiProvider.AiResponse afterOpen = providerBroker.execute("TEACHER_CHAT", ctx, request);
        assertThat(afterOpen.rawMetadata()).containsEntry("secondary", true);
        assertThat(mockAiProvider.generateInvocationCount()).isEqualTo(invocationsBefore);
    }
}
