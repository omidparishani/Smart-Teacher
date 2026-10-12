package ir.omid.smartteacher.ai.internal.application;

import ir.omid.smartteacher.ai.AiProvider;
import ir.omid.smartteacher.ai.internal.OpenAiCompatibleProvider;
import ir.omid.smartteacher.ai.internal.domain.AiProviderEntity;
import ir.omid.smartteacher.ai.internal.domain.AiProviderRepository;
import ir.omid.smartteacher.ai.internal.domain.AiProviderType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * رجیستری runtime از beanهای {@link AiProvider} <b>به‌همراه</b> Adapterهای
 * ساخته‌شده از کاتالوگ DB.
 *
 * <p>🔴 چرا کاتالوگ لازم است؟ پنل ادمین Provider را در {@code ai_providers}
 * ذخیره می‌کند (با کلید رمزشده و baseUrl). اگر فقط beanهای Spring را
 * ببینیم، ثبت واقعی در پنل هیچ اثری روی مسیر معلم ندارد و
 * {@code AI_PROVIDER_UNAVAILABLE} می‌دهد — دقیقاً همان خطایی که با خاموش
 * کردن Mock دیده می‌شود.
 *
 * <p>Adapter کاتالوگ فقط برای {@link AiProviderType#OPENAI_COMPATIBLE}
 * (و {@code CUSTOM} با همان پروتکل Chat Completions) ساخته می‌شود —
 * بقیهٔ انواع تا Adapter اختصاصی‌شان بیاید، عمداً رد می‌شوند (نه حدس API).
 */
@Component
public class AiProviderRegistry {

    private final Map<String, AiProvider> beansByCode;
    private final AiProviderRepository providerRepository;

    public AiProviderRegistry(List<AiProvider> providers,
                              AiProviderRepository providerRepository) {
        this.beansByCode = providers.stream()
                .collect(Collectors.toMap(
                        p -> p.code().toUpperCase(),
                        Function.identity(),
                        (a, b) -> a,
                        LinkedHashMap::new));
        this.providerRepository = providerRepository;
    }

    public Optional<AiProvider> find(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        AiProvider bean = beansByCode.get(code.toUpperCase());
        if (bean != null) {
            return Optional.of(bean);
        }
        return providerRepository.findByCodeIgnoreCase(code.trim())
                .flatMap(this::toCatalogProvider);
    }

    public List<AiProvider> allAvailable() {
        Map<String, AiProvider> merged = new LinkedHashMap<>(beansByCode);
        for (AiProviderEntity entity : providerRepository.findAllByOrderByCodeAsc()) {
            if (!merged.containsKey(entity.getCode().toUpperCase())) {
                toCatalogProvider(entity).ifPresent(
                        p -> merged.put(p.code().toUpperCase(), p));
            }
        }
        return merged.values().stream()
                .filter(AiProvider::isAvailable)
                .toList();
    }

    private Optional<AiProvider> toCatalogProvider(AiProviderEntity entity) {
        if (entity == null || !entity.isEnabled()) {
            return Optional.empty();
        }
        if (entity.getType() != AiProviderType.OPENAI_COMPATIBLE
                && entity.getType() != AiProviderType.CUSTOM) {
            return Optional.empty();
        }
        String apiKey = entity.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(OpenAiCompatibleProvider.fromCatalog(entity));
    }
}
