package ir.omid.smartteacher.ai.internal.application;

import ir.omid.smartteacher.ai.AiProvider;
import ir.omid.smartteacher.ai.AiProviderException;
import ir.omid.smartteacher.ai.internal.domain.ResolutionContext;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * اجرای درخواست AI با Circuit Breaker و failover بین Providerها.
 */
@Service
public class ProviderBroker {

    private static final Logger log = LoggerFactory.getLogger(ProviderBroker.class);

    private final ProviderResolver resolver;
    private final AiProviderRegistry registry;
    private final ProviderCircuitBreakerRegistry circuitBreakers;

    public ProviderBroker(ProviderResolver resolver,
                          AiProviderRegistry registry,
                          ProviderCircuitBreakerRegistry circuitBreakers) {
        this.resolver = resolver;
        this.registry = registry;
        this.circuitBreakers = circuitBreakers;
    }

    public AiProvider.AiResponse execute(
            String featureCode,
            ResolutionContext ctx,
            AiProvider.AiRequest request) {

        ProviderResolver.ResolvedProvider primary = resolver.resolve(featureCode, ctx);
        List<ProviderResolver.ResolvedProvider> chain = resolver.failoverChain(primary);
        List<String> failures = new ArrayList<>();

        for (ProviderResolver.ResolvedProvider rp : chain) {
            AiProvider provider = registry.find(rp.providerCode())
                    .filter(AiProvider::isAvailable)
                    .orElse(null);
            if (provider == null) {
                failures.add(rp.providerCode() + ":unavailable");
                continue;
            }

            ProviderCircuitBreaker breaker = circuitBreakers.forProvider(rp.providerCode());
            if (!breaker.allowCall()) {
                failures.add(rp.providerCode() + ":circuit_open");
                log.debug("Circuit OPEN برای {} — fail-fast", rp.providerCode());
                continue;
            }

            try {
                AiProvider.AiRequest withModel = request;
                if ((request.modelId() == null || request.modelId().isBlank())
                        && rp.modelCode() != null) {
                    withModel = new AiProvider.AiRequest(
                            rp.modelCode(),
                            request.systemPrompt(),
                            request.userPrompt(),
                            request.history(),
                            request.temperature(),
                            request.maxTokens(),
                            request.topP(),
                            request.stopSequences(),
                            request.jsonSchema(),
                            request.traceId(),
                            request.timeout(),
                            request.providerOptions());
                }
                AiProvider.AiResponse response = provider.generate(withModel);
                breaker.recordSuccess();
                return response;
            } catch (AiProviderException e) {
                breaker.recordFailure();
                failures.add(rp.providerCode() + ":" + e.getMessage());
                log.warn("Provider {} شکست: {}", rp.providerCode(), e.getMessage());
                if (!e.isFallbackEligible()) {
                    throw e;
                }
            }
        }

        throw new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE,
                "همه Providerها برای «" + featureCode + "» شکست خوردند: " + failures);
    }
}
