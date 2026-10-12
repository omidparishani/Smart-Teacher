package ir.omid.smartteacher.ai.internal.api.dto;

/** پاسخ تست اتصال Provider. */
public record ProviderTestResponse(
        boolean ok,
        String providerCode,
        String message,
        long latencyMs
) {
}
