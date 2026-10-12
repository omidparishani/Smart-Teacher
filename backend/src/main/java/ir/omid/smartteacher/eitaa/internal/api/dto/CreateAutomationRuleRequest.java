package ir.omid.smartteacher.eitaa.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAutomationRuleRequest(
        @NotBlank String name,
        @NotBlank String triggerType,
        String triggerConfig,
        @NotBlank String actionType,
        String actionConfig,
        Boolean enabled
) {
}
