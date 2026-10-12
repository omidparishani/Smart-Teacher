package ir.omid.smartteacher.ai.internal.application;

import ir.omid.smartteacher.ai.AiProvider;
import ir.omid.smartteacher.ai.internal.MockAiProvider;
import ir.omid.smartteacher.ai.internal.domain.AiModelEntity;
import ir.omid.smartteacher.ai.internal.domain.AiModelRepository;
import ir.omid.smartteacher.ai.internal.domain.AiProviderAssignment;
import ir.omid.smartteacher.ai.internal.domain.AiProviderAssignmentRepository;
import ir.omid.smartteacher.ai.internal.domain.AiProviderEntity;
import ir.omid.smartteacher.ai.internal.domain.AiProviderRepository;
import ir.omid.smartteacher.ai.internal.domain.AiScopeType;
import ir.omid.smartteacher.ai.internal.domain.ResolutionContext;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * رزولوشن شش‌سطحی Provider — stub با مسیر Mock کارآمد.
 *
 * <p>ترتیب: USER → CLASS → TENANT → PLAN → FEATURE → GLOBAL.
 * اگر تخصیصی در DB نباشد، به اولین {@link AiProvider} در دسترس
 * (معمولاً MOCK) برمی‌گردد.
 */
@Component
public class ProviderResolver {

    private static final Logger log = LoggerFactory.getLogger(ProviderResolver.class);

    private static final List<AiScopeType> ORDER = List.of(
            AiScopeType.USER,
            AiScopeType.CLASS,
            AiScopeType.TENANT,
            AiScopeType.PLAN,
            AiScopeType.FEATURE,
            AiScopeType.GLOBAL);

    private final AiProviderAssignmentRepository assignments;
    private final AiProviderRepository providers;
    private final AiModelRepository models;
    private final AiProviderRegistry registry;

    public ProviderResolver(AiProviderAssignmentRepository assignments,
                            AiProviderRepository providers,
                            AiModelRepository models,
                            AiProviderRegistry registry) {
        this.assignments = assignments;
        this.providers = providers;
        this.models = models;
        this.registry = registry;
    }

    public ResolvedProvider resolve(String featureCode, ResolutionContext ctx) {
        List<ResolvedProvider> chain = resolveChain(featureCode, ctx);
        if (chain.isEmpty()) {
            throw new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE,
                    "هیچ Provider برای «" + featureCode + "» پیکربندی نشده است.");
        }
        ResolvedProvider primary = chain.getFirst();
        log.debug("Provider resolved feature={} scope={} code={}",
                featureCode, primary.scope(), primary.providerCode());
        return primary;
    }

    /**
     * زنجیره failover — همه تخصیص‌های سطح یافت‌شده به‌علاوه Mockهای در دسترس.
     */
    public List<ResolvedProvider> failoverChain(ResolvedProvider primary) {
        List<ResolvedProvider> chain = new ArrayList<>();
        chain.add(primary);
        for (AiProvider p : registry.allAvailable()) {
            if (!p.code().equalsIgnoreCase(primary.providerCode())) {
                chain.add(new ResolvedProvider(
                        p.code(), null, primary.scope(), primary.modelCode(), List.of()));
            }
        }
        return List.copyOf(chain);
    }

    /**
     * Trace رزولوشن — گام‌های مرتب برای ادمین (بدون side-effect).
     */
    public List<ResolveTraceStep> traceResolution(String featureCode, ResolutionContext ctx) {
        List<ResolveTraceStep> steps = new ArrayList<>();

        for (AiScopeType scope : ORDER) {
            Optional<UUID> scopeId = ctx.scopeIdFor(scope);
            if (scopeId.isEmpty() && scope != AiScopeType.GLOBAL && scope != AiScopeType.FEATURE) {
                steps.add(new ResolveTraceStep(
                        scope, false, null, null, null, "scope id missing"));
                continue;
            }

            List<AiProviderAssignment> found = assignments.findActive(
                    scope, scopeId.orElse(null));
            if (found.isEmpty()) {
                steps.add(new ResolveTraceStep(scope, false, null, null, null, null));
                continue;
            }

            for (AiProviderAssignment a : found) {
                Optional<AiProviderEntity> entity = providers.findById(a.getProviderId());
                if (entity.isEmpty()) {
                    steps.add(new ResolveTraceStep(
                            scope, false, a.getProviderId(), null, null, "entity not found"));
                    continue;
                }
                AiProviderEntity pe = entity.get();
                if (!pe.isEnabled()) {
                    steps.add(new ResolveTraceStep(
                            scope, false, pe.getId(), pe.getCode(), pe.getName(), "disabled"));
                    continue;
                }
                if (registry.find(pe.getCode()).isEmpty()
                        && !pe.getCode().toUpperCase().startsWith("MOCK")) {
                    steps.add(new ResolveTraceStep(
                            scope, false, pe.getId(), pe.getCode(), pe.getName(),
                            "no runtime (کلید API یا نوع OPENAI_COMPATIBLE/CUSTOM لازم است)"));
                    continue;
                }
                steps.add(new ResolveTraceStep(
                        scope, true, pe.getId(), pe.getCode(), pe.getName(), null));
            }
        }

        registry.find(MockAiProvider.CODE)
                .filter(AiProvider::isAvailable)
                .ifPresent(p -> steps.add(new ResolveTraceStep(
                        AiScopeType.GLOBAL, true, null, p.code(), p.code(), "mock fallback")));

        return List.copyOf(steps);
    }

    public List<ResolvedProvider> resolveChain(String featureCode, ResolutionContext ctx) {
        for (AiScopeType scope : ORDER) {
            Optional<UUID> scopeId = ctx.scopeIdFor(scope);
            if (scopeId.isEmpty() && scope != AiScopeType.GLOBAL && scope != AiScopeType.FEATURE) {
                continue;
            }

            List<AiProviderAssignment> found = assignments.findActive(
                    scope, scopeId.orElse(null));
            if (!found.isEmpty()) {
                List<ResolvedProvider> result = new ArrayList<>();
                for (AiProviderAssignment a : found) {
                    toResolved(a, scope).ifPresent(result::add);
                }
                if (!result.isEmpty()) {
                    return List.copyOf(result);
                }
            }
        }

        // Fallback: Mock، سپس اولین Provider اجرایی (bean یا کاتالوگ)
        return registry.find(MockAiProvider.CODE)
                .filter(AiProvider::isAvailable)
                .map(p -> List.of(new ResolvedProvider(
                        p.code(), null, AiScopeType.GLOBAL, MockAiProvider.DEFAULT_MODEL, List.of())))
                .orElseGet(() -> registry.allAvailable().stream()
                        .findFirst()
                        .map(p -> List.of(new ResolvedProvider(
                                p.code(),
                                null,
                                AiScopeType.GLOBAL,
                                defaultModelCode(p.code()),
                                List.of())))
                        .orElse(List.of()));
    }

    private Optional<ResolvedProvider> toResolved(AiProviderAssignment a, AiScopeType scope) {
        Optional<AiProviderEntity> entity = providers.findById(a.getProviderId());
        if (entity.isEmpty() || !entity.get().isEnabled()) {
            return Optional.empty();
        }
        String code = entity.get().getCode();
        if (registry.find(code).isEmpty()) {
            // Provider در DB هست ولی runtime نیست (بدون کلید / نوع پشتیبانی‌نشده)
            if (!code.toUpperCase().startsWith("MOCK")) {
                return Optional.empty();
            }
        }
        return Optional.of(new ResolvedProvider(
                code,
                a.getProviderId(),
                scope,
                defaultModelCode(entity.get().getId(), code),
                List.of()));
    }

    private String defaultModelCode(String providerCode) {
        return providers.findByCodeIgnoreCase(providerCode)
                .map(pe -> defaultModelCode(pe.getId(), providerCode))
                .orElse(null);
    }

    private String defaultModelCode(UUID providerId, String providerCode) {
        return models.findByProviderIdOrderByCodeAsc(providerId).stream()
                .filter(AiModelEntity::isEnabled)
                .map(AiModelEntity::getCode)
                .findFirst()
                .orElseGet(() -> providerCode.toUpperCase().startsWith("MOCK")
                        ? MockAiProvider.DEFAULT_MODEL
                        : null);
    }

    /**
     * نتیجه رزولوشن.
     *
     * @param providerCode کد runtime
     * @param providerEntityId شناسه DB؛ nullable برای fallback
     * @param scope سطح یافت‌شده
     * @param modelCode مدل پیشنهادی
     * @param failures شکست‌های قبلی در زنجیره (برای لاگ)
     */
    public record ResolvedProvider(
            String providerCode,
            UUID providerEntityId,
            AiScopeType scope,
            String modelCode,
            List<String> failures
    ) {
    }

    /** گام trace رزولوشن — برای endpoint ادمین. */
    public record ResolveTraceStep(
            AiScopeType level,
            boolean found,
            UUID providerId,
            String providerCode,
            String providerName,
            String detail
    ) {
    }
}
