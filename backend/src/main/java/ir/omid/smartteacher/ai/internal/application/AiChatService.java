package ir.omid.smartteacher.ai.internal.application;

import ir.omid.smartteacher.ai.AiCostCalculator;
import ir.omid.smartteacher.ai.AiProvider;
import ir.omid.smartteacher.ai.internal.MockAiProvider;
import ir.omid.smartteacher.ai.internal.api.dto.AiChatRequest;
import ir.omid.smartteacher.ai.internal.api.dto.AiChatResponse;
import ir.omid.smartteacher.ai.internal.domain.AiUsageRecord;
import ir.omid.smartteacher.ai.internal.domain.AiUsageRecordRepository;
import ir.omid.smartteacher.ai.internal.domain.ResolutionContext;
import ir.omid.smartteacher.billing.EntitlementService;
import ir.omid.smartteacher.billing.UsageMetering;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.tenant.TenantContext;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * گفت‌وگوی دستیار معلم — PromptResolver + ProviderBroker + ثبت مصرف.
 */
@Service
@ConditionalOnBean(AiProvider.class)
public class AiChatService {

    public static final String FEATURE_TEACHER_CHAT = "TEACHER_CHAT";

    private final PromptResolver promptResolver;
    private final PromptRenderer promptRenderer;
    private final ProviderBroker providerBroker;
    private final AiCostCalculator costCalculator;
    private final AiUsageRecordRepository usageRecords;
    private final ObjectProvider<EntitlementService> entitlementService;
    private final ObjectProvider<UsageMetering> usageMetering;

    public AiChatService(PromptResolver promptResolver,
                         PromptRenderer promptRenderer,
                         ProviderBroker providerBroker,
                         AiCostCalculator costCalculator,
                         AiUsageRecordRepository usageRecords,
                         ObjectProvider<EntitlementService> entitlementService,
                         ObjectProvider<UsageMetering> usageMetering) {
        this.promptResolver = promptResolver;
        this.promptRenderer = promptRenderer;
        this.providerBroker = providerBroker;
        this.costCalculator = costCalculator;
        this.usageRecords = usageRecords;
        this.entitlementService = entitlementService;
        this.usageMetering = usageMetering;
    }

    @Transactional
    public AiChatResponse chat(AiChatRequest request) {
        UUID tenantId = TenantContext.currentTenantId().orElseThrow(
                () -> new AppException(ErrorCode.INVALID_REQUEST, "مستأجر برای chat لازم است."));
        UUID userId = TenantContext.currentUserId().orElse(null);

        ResolutionContext ctx = new ResolutionContext(
                tenantId, userId, null, null, FEATURE_TEACHER_CHAT);

        PromptResolver.ResolvedPrompt resolved = promptResolver.resolve(FEATURE_TEACHER_CHAT, ctx);
        PromptRenderer.RenderedPrompt rendered = promptRenderer.render(
                resolved.version(),
                Map.of("message", request.message()));

        String model = request.model() == null || request.model().isBlank()
                ? MockAiProvider.DEFAULT_MODEL
                : request.model().trim();

        EntitlementService entitlement = entitlementService.getIfAvailable();
        if (entitlement != null) {
            entitlement.requireFeature("AI_CHAT");
        }
        UsageMetering metering = usageMetering.getIfAvailable();
        if (metering != null) {
            metering.consume(UsageMetering.METRIC_AI_REQUESTS, 1);
        }

        AiProvider.AiRequest aiRequest = new AiProvider.AiRequest(
                model,
                rendered.systemPrompt(),
                rendered.userPrompt(),
                java.util.List.of(),
                resolved.version().getTemperature() == null
                        ? null
                        : resolved.version().getTemperature().doubleValue(),
                resolved.version().getMaxTokens(),
                null,
                java.util.List.of(),
                resolved.version().getResponseSchema(),
                null,
                null,
                Map.of());

        AiProvider.AiResponse aiResponse = providerBroker.execute(
                FEATURE_TEACHER_CHAT, ctx, aiRequest);

        BigDecimal cost = costCalculator.calculateOrZero(aiResponse);
        int promptTokens = aiResponse.inputTokens() == null ? 0 : aiResponse.inputTokens();
        int completionTokens = aiResponse.outputTokens() == null ? 0 : aiResponse.outputTokens();

        AiUsageRecord usage = usageRecords.save(new AiUsageRecord(
                FEATURE_TEACHER_CHAT,
                aiResponse.modelId() == null ? model : aiResponse.modelId(),
                promptTokens,
                completionTokens,
                cost));

        String providerCode = aiResponse.rawMetadata() != null
                && Boolean.TRUE.equals(aiResponse.rawMetadata().get("secondary"))
                ? "MOCK_SECONDARY"
                : MockAiProvider.CODE;

        return new AiChatResponse(
                aiResponse.content(),
                aiResponse.modelId(),
                providerCode,
                promptTokens,
                completionTokens,
                cost,
                usage.getId());
    }
}
