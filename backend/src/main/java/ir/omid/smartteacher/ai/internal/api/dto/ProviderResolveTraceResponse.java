package ir.omid.smartteacher.ai.internal.api.dto;

import java.util.List;

/** نتیجه trace رزولوشن Provider برای ادمین. */
public record ProviderResolveTraceResponse(
        String featureCode,
        List<ProviderResolveTraceStepResponse> steps,
        String selectedProviderCode
) {
}
