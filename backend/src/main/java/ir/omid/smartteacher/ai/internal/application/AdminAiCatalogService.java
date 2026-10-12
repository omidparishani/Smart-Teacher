package ir.omid.smartteacher.ai.internal.application;

import ir.omid.smartteacher.ai.internal.api.dto.AiModelResponse;
import ir.omid.smartteacher.ai.internal.api.dto.AiProviderResponse;
import ir.omid.smartteacher.ai.internal.api.dto.CreateAiProviderRequest;
import ir.omid.smartteacher.ai.internal.domain.AiModelEntity;
import ir.omid.smartteacher.ai.internal.domain.AiModelRepository;
import ir.omid.smartteacher.ai.internal.domain.AiProviderEntity;
import ir.omid.smartteacher.ai.internal.domain.AiProviderRepository;
import ir.omid.smartteacher.common.error.DuplicateResourceException;
import ir.omid.smartteacher.common.error.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * مدیریت کاتالوگ Provider/Model برای ادمین — بدون افشای کلید خام.
 */
@Service
public class AdminAiCatalogService {

    private final AiProviderRepository providers;
    private final AiModelRepository models;

    public AdminAiCatalogService(AiProviderRepository providers, AiModelRepository models) {
        this.providers = providers;
        this.models = models;
    }

    @Transactional(readOnly = true)
    public List<AiProviderResponse> listProviders() {
        return providers.findAllByOrderByCodeAsc().stream()
                .map(AdminAiCatalogService::toProviderResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AiProviderResponse getProvider(UUID id) {
        return providers.findById(id)
                .map(AdminAiCatalogService::toProviderResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("Provider"));
    }

    @Transactional
    public AiProviderResponse createProvider(CreateAiProviderRequest request) {
        String code = request.code().trim();
        if (providers.existsByCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("Provider با این کد از قبل وجود دارد.");
        }

        AiProviderEntity entity = new AiProviderEntity(
                code,
                request.name().trim(),
                request.type());
        entity.setBaseUrl(blankToNull(request.baseUrl()));
        entity.setEnabled(request.enabled() == null || request.enabled());
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            entity.setApiKey(request.apiKey().trim());
        }

        return toProviderResponse(providers.save(entity));
    }

    @Transactional
    public AiProviderResponse updateProvider(UUID id, ir.omid.smartteacher.ai.internal.api.dto.UpdateAiProviderRequest request) {
        AiProviderEntity entity = providers.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Provider"));
        if (request.name() != null && !request.name().isBlank()) {
            entity.setName(request.name().trim());
        }
        if (request.type() != null) {
            entity.setType(request.type());
        }
        if (request.baseUrl() != null) {
            entity.setBaseUrl(blankToNull(request.baseUrl()));
        }
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            entity.setApiKey(request.apiKey().trim());
        }
        return toProviderResponse(providers.save(entity));
    }

    @Transactional
    public void deleteProvider(UUID id) {
        AiProviderEntity entity = providers.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Provider"));
        models.deleteAll(models.findAllByOrderByDisplayNameAsc().stream()
                .filter(m -> m.getProviderId().equals(id))
                .toList());
        providers.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<AiModelResponse> listModels() {
        return models.findAllByOrderByDisplayNameAsc().stream()
                .map(AdminAiCatalogService::toModelResponse)
                .toList();
    }

    @Transactional
    public AiModelResponse createModel(ir.omid.smartteacher.ai.internal.api.dto.CreateAiModelRequest request) {
        providers.findById(request.providerId())
                .orElseThrow(() -> ResourceNotFoundException.of("Provider"));

        AiModelEntity entity = new AiModelEntity(
                request.providerId(),
                request.code().trim(),
                request.displayName().trim());
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }
        if (request.inputCostPerMillion() != null) {
            entity.setInputCostPerMillion(request.inputCostPerMillion());
        }
        if (request.outputCostPerMillion() != null) {
            entity.setOutputCostPerMillion(request.outputCostPerMillion());
        }
        if (request.currency() != null && !request.currency().isBlank()) {
            entity.setCurrency(request.currency().trim());
        }
        return toModelResponse(models.save(entity));
    }

    @Transactional
    public AiModelResponse updateModel(UUID id, ir.omid.smartteacher.ai.internal.api.dto.UpdateAiModelRequest request) {
        AiModelEntity entity = models.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Model"));
        if (request.providerId() != null) {
            providers.findById(request.providerId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Provider"));
            // entity.providerId immutable or set via reflection if needed
        }
        if (request.code() != null && !request.code().isBlank()) {
            entity.setCode(request.code().trim());
        }
        if (request.displayName() != null && !request.displayName().isBlank()) {
            entity.setDisplayName(request.displayName().trim());
        }
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }
        if (request.inputCostPerMillion() != null) {
            entity.setInputCostPerMillion(request.inputCostPerMillion());
        }
        if (request.outputCostPerMillion() != null) {
            entity.setOutputCostPerMillion(request.outputCostPerMillion());
        }
        if (request.currency() != null && !request.currency().isBlank()) {
            entity.setCurrency(request.currency().trim());
        }
        return toModelResponse(models.save(entity));
    }

    @Transactional
    public void deleteModel(UUID id) {
        AiModelEntity entity = models.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Model"));
        models.delete(entity);
    }

    private static AiProviderResponse toProviderResponse(AiProviderEntity entity) {
        return new AiProviderResponse(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getType(),
                entity.getBaseUrl(),
                entity.getApiKeyHint() == null ? "" : entity.getApiKeyHint(),
                entity.isEnabled());
    }

    private static AiModelResponse toModelResponse(AiModelEntity entity) {
        return new AiModelResponse(
                entity.getId(),
                entity.getProviderId(),
                entity.getCode(),
                entity.getDisplayName(),
                entity.isEnabled(),
                entity.getInputCostPerMillion(),
                entity.getOutputCostPerMillion(),
                entity.getCurrency());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
