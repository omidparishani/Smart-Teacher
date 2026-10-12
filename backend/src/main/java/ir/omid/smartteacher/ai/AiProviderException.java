package ir.omid.smartteacher.ai;

/**
 * خطای Provider — اگر {@link #isFallbackEligible()} true باشد،
 * {@link ir.omid.smartteacher.ai.internal.application.ProviderBroker}
 * به Provider بعدی می‌رود.
 */
public class AiProviderException extends RuntimeException {

    private final boolean fallbackEligible;
    private final String providerCode;

    public AiProviderException(String providerCode, String message, boolean fallbackEligible) {
        super(message);
        this.providerCode = providerCode;
        this.fallbackEligible = fallbackEligible;
    }

    public AiProviderException(String providerCode, String message,
                               boolean fallbackEligible, Throwable cause) {
        super(message, cause);
        this.providerCode = providerCode;
        this.fallbackEligible = fallbackEligible;
    }

    public boolean isFallbackEligible() {
        return fallbackEligible;
    }

    public String providerCode() {
        return providerCode;
    }
}
