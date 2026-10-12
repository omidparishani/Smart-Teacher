package ir.omid.smartteacher.ai.internal.api.dto;

import java.util.Map;

/** درخواست تست playground نسخه. */
public record PromptVersionTestRequest(
        Map<String, Object> variables,
        String model
) {
}
