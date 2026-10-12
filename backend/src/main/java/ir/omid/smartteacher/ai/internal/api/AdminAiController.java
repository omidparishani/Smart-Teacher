package ir.omid.smartteacher.ai.internal.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.omid.smartteacher.ai.AiProvider;
import ir.omid.smartteacher.ai.internal.api.dto.AiModelResponse;
import ir.omid.smartteacher.ai.internal.api.dto.AiProviderResponse;
import ir.omid.smartteacher.ai.internal.api.dto.CreateAiProviderRequest;
import ir.omid.smartteacher.ai.internal.api.dto.ProviderTestResponse;
import ir.omid.smartteacher.ai.internal.api.dto.ProviderResolveTraceResponse;
import ir.omid.smartteacher.ai.internal.api.dto.ProviderResolveTraceStepResponse;
import ir.omid.smartteacher.ai.internal.application.AdminAiCatalogService;
import ir.omid.smartteacher.ai.internal.application.AiProviderRegistry;
import ir.omid.smartteacher.ai.internal.application.ProviderResolver;
import ir.omid.smartteacher.ai.internal.domain.ResolutionContext;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * مدیریت Provider/Model در پنل ادمین — {@code /api/v1/admin/ai/...}.
 *
 * <p>داخل ماژول {@code ai} است (نه {@code admin}) تا مخزن‌ها از مرز
 * Modulith بیرون نروند. دسترسی همان {@code SUPER_ADMIN}/{@code ADMIN}.
 *
 * <p><b>🔴 پاسخ هرگز کلید خام ندارد</b> — فقط {@code tokenHint}.
 */
@RestController
@RequestMapping("/api/v1/admin/ai")
@Tag(name = "AI ادمین", description = "کاتالوگ Provider و Model — بدون افشای کلید")
public class AdminAiController {

    /** تکرار آگاهانهٔ ثابت ادمین — ارجاع به {@code admin.internal} ممنوع است. */
    static final String ADMIN_ONLY = "hasAnyAuthority('SUPER_ADMIN','ADMIN')";

    private final AdminAiCatalogService catalogService;
    private final AiProviderRegistry providerRegistry;
    private final ProviderResolver providerResolver;

    public AdminAiController(AdminAiCatalogService catalogService,
                             AiProviderRegistry providerRegistry,
                             ProviderResolver providerResolver) {
        this.catalogService = catalogService;
        this.providerRegistry = providerRegistry;
        this.providerResolver = providerResolver;
    }

    @GetMapping("/providers")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "فهرست Providerها",
            description = "فقط tokenHint برمی‌گردد؛ هرگز apiKey خام.")
    public List<AiProviderResponse> listProviders() {
        return catalogService.listProviders();
    }

    @PostMapping("/providers")
    @PreAuthorize(ADMIN_ONLY)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "افزودن Provider",
            description = "کلید در دیتابیس رمز می‌شود؛ در پاسخ فقط tokenHint است.")
    public AiProviderResponse createProvider(@Valid @RequestBody CreateAiProviderRequest request) {
        return catalogService.createProvider(request);
    }

    @PutMapping("/providers/{id}")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "ویرایش Provider")
    public AiProviderResponse updateProvider(@PathVariable UUID id,
                                             @Valid @RequestBody ir.omid.smartteacher.ai.internal.api.dto.UpdateAiProviderRequest request) {
        return catalogService.updateProvider(id, request);
    }

    @DeleteMapping("/providers/{id}")
    @PreAuthorize(ADMIN_ONLY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "حذف Provider")
    public void deleteProvider(@PathVariable UUID id) {
        catalogService.deleteProvider(id);
    }

    @PostMapping("/providers/{id}/test")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "تست اتصال Provider",
            description = "برای MOCK سلامت ساختگی؛ برای OPENAI_COMPATIBLE/CUSTOM با کلید API، health واقعی.")
    public ProviderTestResponse testProvider(@PathVariable UUID id) {
        AiProviderResponse meta = catalogService.getProvider(id);
        Instant start = Instant.now();
        AiProvider runtime = providerRegistry.find(meta.code()).orElse(null);
        if (runtime == null || !runtime.isAvailable()) {
            // مسیر Mock ذخیره‌شده در DB بدون bean — اگر type=MOCK باشد OK ساختگی
            if ("MOCK".equalsIgnoreCase(meta.type().name())
                    || meta.code().toUpperCase().startsWith("MOCK")) {
                return new ProviderTestResponse(
                        true, meta.code(), "Mock OK", Duration.between(start, Instant.now()).toMillis());
            }
            throw ResourceNotFoundException.of("Provider runtime");
        }
        AiProvider.ProviderHealth health = runtime.health();
        return new ProviderTestResponse(
                health.healthy(),
                meta.code(),
                health.message() == null ? "OK" : health.message(),
                health.latency() == null
                        ? Duration.between(start, Instant.now()).toMillis()
                        : health.latency().toMillis());
    }

    @GetMapping("/models")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "فهرست مدل‌ها")
    public List<AiModelResponse> listModels() {
        return catalogService.listModels();
    }

    @PostMapping("/models")
    @PreAuthorize(ADMIN_ONLY)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "ساخت مدل جدید هوش مصنوعی")
    public AiModelResponse createModel(@Valid @RequestBody ir.omid.smartteacher.ai.internal.api.dto.CreateAiModelRequest request) {
        return catalogService.createModel(request);
    }

    @PutMapping("/models/{id}")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "ویرایش مدل هوش مصنوعی")
    public AiModelResponse updateModel(@PathVariable UUID id,
                                       @Valid @RequestBody ir.omid.smartteacher.ai.internal.api.dto.UpdateAiModelRequest request) {
        return catalogService.updateModel(id, request);
    }

    @DeleteMapping("/models/{id}")
    @PreAuthorize(ADMIN_ONLY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "حذف مدل هوش مصنوعی")
    public void deleteModel(@PathVariable UUID id) {
        catalogService.deleteModel(id);
    }

    @GetMapping("/resolve-trace")
    @PreAuthorize(ADMIN_ONLY)
    @Operation(summary = "Trace رزولوشن Provider برای فیچر")
    public ProviderResolveTraceResponse resolveTrace(
            @RequestParam String feature,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID planId) {
        ResolutionContext ctx = new ResolutionContext(tenantId, userId, classId, planId, feature);
        var steps = providerResolver.traceResolution(feature, ctx).stream()
                .map(s -> new ProviderResolveTraceStepResponse(
                        s.level().name(),
                        s.found(),
                        s.providerId(),
                        s.providerCode(),
                        s.providerName(),
                        s.detail()))
                .toList();
        String selected = providerResolver.resolveChain(feature, ctx).stream()
                .findFirst()
                .map(ProviderResolver.ResolvedProvider::providerCode)
                .orElse(null);
        return new ProviderResolveTraceResponse(feature, steps, selected);
    }
}
