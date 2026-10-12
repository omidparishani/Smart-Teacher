package ir.omid.smartteacher.ai.internal.api.dto;

import java.util.UUID;

/** یک گام در trace رزولوشن Provider. */
public record ProviderResolveTraceStepResponse(
        String level,
        boolean found,
        UUID providerId,
        String providerCode,
        String providerName,
        String detail
) {
}
