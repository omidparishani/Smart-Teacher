package ir.omid.smartteacher.ai.internal.application;

import ir.omid.smartteacher.ai.AiProvider;
import ir.omid.smartteacher.ai.internal.MockAiProvider;
import ir.omid.smartteacher.ai.internal.api.dto.CreatePromptRequest;
import ir.omid.smartteacher.ai.internal.api.dto.CreatePromptVersionRequest;
import ir.omid.smartteacher.ai.internal.api.dto.PromptResponse;
import ir.omid.smartteacher.ai.internal.api.dto.PromptVersionResponse;
import ir.omid.smartteacher.ai.internal.api.dto.PromptVersionCompareResponse;
import ir.omid.smartteacher.ai.internal.api.dto.PromptVersionTestRequest;
import ir.omid.smartteacher.ai.internal.api.dto.PromptVersionTestResponse;
import ir.omid.smartteacher.ai.internal.domain.AiScopeType;
import ir.omid.smartteacher.ai.internal.domain.PromptAssignment;
import ir.omid.smartteacher.ai.internal.domain.PromptAssignmentRepository;
import ir.omid.smartteacher.ai.internal.domain.PromptEntity;
import ir.omid.smartteacher.ai.internal.domain.PromptRepository;
import ir.omid.smartteacher.ai.internal.domain.PromptVersionEntity;
import ir.omid.smartteacher.ai.internal.domain.PromptVersionRepository;
import ir.omid.smartteacher.ai.internal.domain.PromptVersionStatus;
import ir.omid.smartteacher.ai.internal.domain.ResolutionContext;
import ir.omid.smartteacher.audit.ActorType;
import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.DuplicateResourceException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import ir.omid.smartteacher.security.audit.SecurityAuditLog;
import ir.omid.smartteacher.security.audit.SecurityEvent;
import ir.omid.smartteacher.tenant.TenantContext;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * CRUD قالب/نسخه پرامپت + انتشار/بازگشت + playground Mock.
 */
@Service
public class AdminPromptService {

    private static final Logger log = LoggerFactory.getLogger(AdminPromptService.class);
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    private final PromptRepository prompts;
    private final PromptVersionRepository versions;
    private final PromptAssignmentRepository assignments;
    private final PromptRenderer renderer;
    private final ProviderBroker broker;
    private final SecurityAuditLog auditLog;
    private final ObjectMapper objectMapper;

    public AdminPromptService(PromptRepository prompts,
                              PromptVersionRepository versions,
                              PromptAssignmentRepository assignments,
                              PromptRenderer renderer,
                              ProviderBroker broker,
                              SecurityAuditLog auditLog,
                              ObjectMapper objectMapper) {
        this.prompts = prompts;
        this.versions = versions;
        this.assignments = assignments;
        this.renderer = renderer;
        this.broker = broker;
        this.auditLog = auditLog;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PromptResponse> list() {
        return prompts.findAllByOrderByCodeAsc().stream()
                .map(AdminPromptService::toPrompt)
                .toList();
    }

    @Transactional
    public void delete(UUID promptId) {
        PromptEntity prompt = prompts.findById(promptId)
                .orElseThrow(() -> ResourceNotFoundException.of("قالب پرامپت"));
        assignments.deleteByPromptId(promptId);
        versions.deleteAll(versions.findByPromptIdOrderByVersionNumberDesc(promptId));
        prompts.delete(prompt);
        audit("PROMPT_DELETED", promptId, Map.of("code", prompt.getCode()));
        log.info("قالب پرامپت {} حذف شد", promptId);
    }

    @Transactional
    public PromptResponse create(CreatePromptRequest request) {
        String code = request.code().trim();
        if (prompts.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("پرامپت با این کد از قبل وجود دارد.");
        }
        PromptEntity entity = new PromptEntity(
                code, request.name().trim(), request.featureCode().trim());
        entity.setDescription(blankToNull(request.description()));
        PromptEntity saved = prompts.save(entity);

        // تخصیص GLOBAL پیش‌فرض تا Resolver بتواند پیدا کند (پس از publish)
        PromptAssignment asg = new PromptAssignment(AiScopeType.GLOBAL, saved.getId());
        asg.setPriority(100);
        assignments.save(asg);

        audit("PROMPT_CREATED", saved.getId(), Map.of("code", code));
        return toPrompt(saved);
    }

    @Transactional(readOnly = true)
    public List<PromptVersionResponse> listVersions(UUID promptId) {
        requirePrompt(promptId);
        return versions.findByPromptIdOrderByVersionNumberDesc(promptId).stream()
                .map(AdminPromptService::toVersion)
                .toList();
    }

    @Transactional
    public PromptVersionResponse addVersion(UUID promptId, CreatePromptVersionRequest request) {
        requirePrompt(promptId);
        int next = versions.findMaxVersionNumber(promptId) + 1;
        PromptVersionEntity version = new PromptVersionEntity(
                promptId, next, request.systemPrompt(), request.userPrompt());
        version.setVariables(request.variables() == null || request.variables().isBlank()
                ? "{}" : request.variables().trim());
        version.setResponseSchema(blankToNull(request.responseSchema()));
        version.setTemperature(request.temperature());
        version.setMaxTokens(request.maxTokens());
        version.setChangeNote(blankToNull(request.changeNote()));
        version.setStatus(PromptVersionStatus.DRAFT);

        PromptVersionEntity saved = versions.save(version);
        audit("PROMPT_VERSION_CREATED", saved.getId(), Map.of(
                "promptId", promptId.toString(),
                "versionNumber", next));
        return toVersion(saved);
    }

    @Transactional
    public PromptVersionResponse publish(UUID versionId) {
        PromptVersionEntity version = requireVersion(versionId);
        if (version.getStatus() == PromptVersionStatus.PUBLISHED) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "این نسخه از قبل منتشر شده است.");
        }
        validateForPublish(version);

        versions.findByPromptIdAndStatus(version.getPromptId(), PromptVersionStatus.PUBLISHED)
                .ifPresent(prev -> {
                    prev.archive();
                    versions.saveAndFlush(prev);
                });

        UUID actor = TenantContext.currentUserId().orElse(null);
        version.publish(actor);
        PromptVersionEntity saved = versions.save(version);

        audit("PROMPT_PUBLISHED", saved.getId(), Map.of(
                "promptId", version.getPromptId().toString(),
                "versionNumber", version.getVersionNumber()));
        log.info("نسخه پرامپت {} منتشر شد", versionId);
        return toVersion(saved);
    }

    @Transactional
    public PromptVersionResponse rollback(UUID versionId) {
        PromptVersionEntity target = requireVersion(versionId);
        if (target.getStatus() == PromptVersionStatus.DRAFT) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "نسخه پیش‌نویس قابل Rollback نیست؛ ابتدا منتشر کنید.");
        }
        if (target.getStatus() == PromptVersionStatus.PUBLISHED) {
            return toVersion(target);
        }

        versions.findByPromptIdAndStatus(target.getPromptId(), PromptVersionStatus.PUBLISHED)
                .ifPresent(prev -> {
                    prev.archive();
                    versions.saveAndFlush(prev);
                });

        UUID actor = TenantContext.currentUserId().orElse(null);
        target.publish(actor);
        PromptVersionEntity saved = versions.save(target);

        audit("PROMPT_ROLLBACK", saved.getId(), Map.of(
                "promptId", target.getPromptId().toString(),
                "versionNumber", target.getVersionNumber()));
        return toVersion(saved);
    }

    @Transactional(readOnly = true)
    public PromptVersionCompareResponse compareVersions(UUID leftId, UUID rightId) {
        PromptVersionEntity left = requireVersion(leftId);
        PromptVersionEntity right = requireVersion(rightId);
        if (!left.getPromptId().equals(right.getPromptId())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "دو نسخه باید از یک قالب پرامپت باشند.");
        }

        return new PromptVersionCompareResponse(
                leftId,
                rightId,
                left.getVersionNumber(),
                right.getVersionNumber(),
                diffField(left.getSystemPrompt(), right.getSystemPrompt()),
                diffField(left.getUserPrompt(), right.getUserPrompt()));
    }

    @Transactional(readOnly = true)
    public PromptVersionTestResponse testVersion(UUID versionId, PromptVersionTestRequest request) {
        PromptVersionEntity version = requireVersion(versionId);
        Map<String, Object> vars = request.variables() == null ? Map.of() : request.variables();
        PromptRenderer.RenderedPrompt rendered = renderer.render(version, vars);

        String model = request.model() == null || request.model().isBlank()
                ? MockAiProvider.DEFAULT_MODEL
                : request.model().trim();

        Instant start = Instant.now();
        AiProvider.AiRequest aiRequest = new AiProvider.AiRequest(
                model,
                rendered.systemPrompt(),
                rendered.userPrompt(),
                List.of(),
                version.getTemperature() == null ? null : version.getTemperature().doubleValue(),
                version.getMaxTokens(),
                null,
                List.of(),
                version.getResponseSchema(),
                null,
                null,
                Map.of("playground", true));

        UUID tenantId = TenantContext.currentTenantId().orElse(null);
        ResolutionContext ctx = new ResolutionContext(
                tenantId, TenantContext.currentUserId().orElse(null),
                null, null, "PROMPT_PLAYGROUND");

        AiProvider.AiResponse response = broker.execute("PROMPT_PLAYGROUND", ctx, aiRequest);
        long durationMs = Duration.between(start, Instant.now()).toMillis();

        return new PromptVersionTestResponse(
                rendered.systemPrompt(),
                rendered.userPrompt(),
                response.content(),
                response.rawMetadata() != null && Boolean.TRUE.equals(response.rawMetadata().get("secondary"))
                        ? "MOCK_SECONDARY"
                        : MockAiProvider.CODE,
                response.modelId(),
                response.inputTokens(),
                response.outputTokens(),
                durationMs);
    }

    private void validateForPublish(PromptVersionEntity version) {
        if (version.getTemperature() != null) {
            double t = version.getTemperature().doubleValue();
            if (t < 0 || t > 2) {
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "temperature باید بین ۰ و ۲ باشد.");
            }
        }

        Map<String, Boolean> untrusted = new LinkedHashMap<>();
        try {
            JsonNode root = objectMapper.readTree(
                    version.getVariables() == null ? "{}" : version.getVariables());
            root.properties().forEach(e -> {
                boolean u = "UNTRUSTED_TEXT".equalsIgnoreCase(e.getValue().path("type").asString());
                untrusted.put(e.getKey(), u);
            });
        } catch (Exception e) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "variables نامعتبر است.");
        }

        // UNTRUSTED در system ممنوع
        Matcher sys = PLACEHOLDER.matcher(version.getSystemPrompt());
        while (sys.find()) {
            String key = sys.group(1);
            if (Boolean.TRUE.equals(untrusted.get(key))) {
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "متغیر UNTRUSTED_TEXT «" + key + "» نباید در system_prompt باشد.");
            }
        }

        // همه placeholderها باید در variables تعریف شده باشند (یا خالی)
        for (String text : List.of(version.getSystemPrompt(), version.getUserPrompt())) {
            Matcher m = PLACEHOLDER.matcher(text);
            while (m.find()) {
                String key = m.group(1);
                if (!untrusted.containsKey(key) && !version.getVariables().contains("\"" + key + "\"")) {
                    // اگر variables خالی است و فقط {{message}} ساده — برای seed مجاز؛
                    // ولی اگر تعریف هست و کلید نیست → خطا
                    if (version.getVariables() != null
                            && !version.getVariables().isBlank()
                            && !"{}".equals(version.getVariables().trim())) {
                        throw new AppException(ErrorCode.VALIDATION_FAILED,
                                "متغیر «" + key + "» در variables تعریف نشده است.");
                    }
                }
            }
        }
    }

    private PromptEntity requirePrompt(UUID id) {
        return prompts.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("پرامپت"));
    }

    private PromptVersionEntity requireVersion(UUID id) {
        return versions.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("نسخه پرامپت"));
    }

    private void audit(String detail, UUID entityId, Map<String, Object> meta) {
        Map<String, Object> m = new LinkedHashMap<>(meta);
        m.put("detail", detail);
        auditLog.record(
                SecurityEvent.SENSITIVE_ACTION,
                null,
                TenantContext.currentUserId().orElse(null),
                ActorType.USER,
                "Prompt",
                entityId,
                m);
    }

    private static PromptResponse toPrompt(PromptEntity e) {
        return new PromptResponse(
                e.getId(), e.getCode(), e.getName(), e.getFeatureCode(),
                e.getDescription(), e.isSystem());
    }

    private static PromptVersionResponse toVersion(PromptVersionEntity v) {
        return new PromptVersionResponse(
                v.getId(), v.getPromptId(), v.getVersionNumber(),
                v.getSystemPrompt(), v.getUserPrompt(), v.getVariables(),
                v.getResponseSchema(), v.getTemperature(), v.getMaxTokens(),
                v.getStatus().name(), v.getPublishedAt(), v.getChangeNote());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static PromptVersionCompareResponse.FieldDiff diffField(String left, String right) {
        String l = left == null ? "" : left;
        String r = right == null ? "" : right;
        return new PromptVersionCompareResponse.FieldDiff(
                l.length(),
                r.length(),
                r.length() - l.length(),
                lineDiff(l, r));
    }

    private static List<PromptVersionCompareResponse.LineDiff> lineDiff(String left, String right) {
        String[] leftLines = left.split("\n", -1);
        String[] rightLines = right.split("\n", -1);
        int max = Math.max(leftLines.length, rightLines.length);
        List<PromptVersionCompareResponse.LineDiff> diffs = new ArrayList<>();

        for (int i = 0; i < max; i++) {
            String ll = i < leftLines.length ? leftLines[i] : null;
            String rl = i < rightLines.length ? rightLines[i] : null;
            if (java.util.Objects.equals(ll, rl)) {
                if (ll != null) {
                    diffs.add(new PromptVersionCompareResponse.LineDiff(
                            i + 1, "UNCHANGED", ll, rl));
                }
            } else if (ll == null) {
                diffs.add(new PromptVersionCompareResponse.LineDiff(
                        i + 1, "ADDED", null, rl));
            } else if (rl == null) {
                diffs.add(new PromptVersionCompareResponse.LineDiff(
                        i + 1, "REMOVED", ll, null));
            } else {
                diffs.add(new PromptVersionCompareResponse.LineDiff(
                        i + 1, "CHANGED", ll, rl));
            }
        }
        return List.copyOf(diffs);
    }
}
