package ir.omid.smartteacher.ai.internal.domain;

import ir.omid.smartteacher.common.jpa.AuditableEntity;
import ir.omid.smartteacher.security.crypto.EncryptedStringConverter;
import ir.omid.smartteacher.security.crypto.SecretEncryptor;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * پیکربندی Provider ذخیره‌شده — {@code ai_providers}.
 *
 * <p><b>🔴 کلید API هرگز در DTO/Log/toString نمی‌آید.</b> ستون
 * {@code api_key_encrypted} با {@link EncryptedStringConverter} رمز
 * می‌شود و پاسخ API فقط {@code tokenHint} دارد.
 */
@Entity
@Table(name = "ai_providers")
public class AiProviderEntity extends AuditableEntity {

    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private AiProviderType type;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "api_key_encrypted")
    private String apiKey;

    @Column(name = "api_key_hint", length = 20)
    private String apiKeyHint;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    protected AiProviderEntity() {
    }

    public AiProviderEntity(String code, String name, AiProviderType type) {
        this.code = code;
        this.name = name;
        this.type = type;
    }

    public void setApiKey(String rawApiKey) {
        this.apiKey = rawApiKey;
        this.apiKeyHint = SecretEncryptor.mask(rawApiKey);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AiProviderType getType() {
        return type;
    }

    public void setType(AiProviderType type) {
        this.type = type;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** فقط برای فراخوانی داخلی Adapter — هرگز به پاسخ HTTP نرود. */
    public String getApiKey() {
        return apiKey;
    }

    public String getApiKeyHint() {
        return apiKeyHint;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "AiProviderEntity{id=" + getId()
                + ", code=" + code
                + ", type=" + type
                + ", enabled=" + enabled
                + ", apiKey=***}";
    }
}
