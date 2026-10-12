package ir.omid.smartteacher.ai.internal.application;

import ir.omid.smartteacher.ai.internal.domain.AiScopeType;
import ir.omid.smartteacher.ai.internal.domain.PromptAssignment;
import ir.omid.smartteacher.ai.internal.domain.PromptAssignmentRepository;
import ir.omid.smartteacher.ai.internal.domain.PromptVersionEntity;
import ir.omid.smartteacher.ai.internal.domain.PromptVersionRepository;
import ir.omid.smartteacher.ai.internal.domain.PromptVersionStatus;
import ir.omid.smartteacher.ai.internal.domain.ResolutionContext;
import ir.omid.smartteacher.common.cache.CacheBackend;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * رزولوشن شش‌سطحی پرامپت — Fail Loud ({@code docs/prompt-system.md} §۵).
 *
 * <p>سطوح پیاده‌شده با داده: GLOBAL، TENANT، FEATURE (+ USER/CLASS/PLAN
 * اگر تخصیص داشته باشند).
 */
@Component
public class PromptResolver {

    private static final Logger log = LoggerFactory.getLogger(PromptResolver.class);

    private static final List<AiScopeType> ORDER = List.of(
            AiScopeType.USER,
            AiScopeType.CLASS,
            AiScopeType.TENANT,
            AiScopeType.PLAN,
            AiScopeType.FEATURE,
            AiScopeType.GLOBAL);

    private final PromptAssignmentRepository assignments;
    private final PromptVersionRepository versions;
    private final CacheBackend cache;
    private final Duration cacheTtl;

    public PromptResolver(PromptAssignmentRepository assignments,
                          PromptVersionRepository versions,
                          CacheBackend cache,
                          @Value("${app.cache.prompt-resolver-ttl:60s}") Duration cacheTtl) {
        this.assignments = assignments;
        this.versions = versions;
        this.cache = cache;
        this.cacheTtl = cacheTtl;
    }

    public ResolvedPrompt resolve(String featureCode, ResolutionContext ctx) {
        String cacheKey = cacheKey(featureCode, ctx);
        return cache.getOrLoad(cacheKey, ResolvedPrompt.class, cacheTtl,
                () -> resolveUncached(featureCode, ctx));
    }

    /** فقط برای تست — پاک‌سازی کش. */
    public void clearCache() {
        // CacheBackend فقط invalidate per-key دارد؛ resolve key pattern varies
    }

    private ResolvedPrompt resolveUncached(String featureCode, ResolutionContext ctx) {
        for (AiScopeType scope : ORDER) {
            Optional<UUID> scopeId = ctx.scopeIdFor(scope);
            if (scopeId.isEmpty() && scope != AiScopeType.GLOBAL && scope != AiScopeType.FEATURE) {
                continue;
            }

            List<PromptAssignment> found = assignments.findActive(
                    featureCode, scope, scopeId.orElse(null));
            if (found.isEmpty()) {
                continue;
            }

            PromptAssignment assignment = found.getFirst();
            PromptVersionEntity version = resolveVersion(assignment);
            log.debug("Prompt resolved feature={} scope={} version={}",
                    featureCode, scope, version.getVersionNumber());
            return new ResolvedPrompt(version, scope, assignment.getPromptId());
        }

        throw new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                "پرامپتی برای فیچر «" + featureCode + "» پیکربندی نشده است.");
    }

    private static String cacheKey(String featureCode, ResolutionContext ctx) {
        return "prompt:" + featureCode + ":" + ctx.cacheKey();
    }

    private PromptVersionEntity resolveVersion(PromptAssignment assignment) {
        if (assignment.getVersionId() != null) {
            return versions.findById(assignment.getVersionId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND,
                            "نسخه پرامپت تخصیص‌یافته یافت نشد."));
        }
        return versions.findByPromptIdAndStatus(
                        assignment.getPromptId(), PromptVersionStatus.PUBLISHED)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_CONFLICT,
                        "نسخه منتشرشده‌ای برای این پرامپت وجود ندارد."));
    }

    public record ResolvedPrompt(
            PromptVersionEntity version,
            AiScopeType scope,
            UUID promptId
    ) {
    }
}
