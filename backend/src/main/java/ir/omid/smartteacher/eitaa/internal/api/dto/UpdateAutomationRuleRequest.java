package ir.omid.smartteacher.eitaa.internal.api.dto;

public record UpdateAutomationRuleRequest(
        String name,
        String triggerConfig,
        String actionType,
        String actionConfig,
        Boolean enabled
) {
}
