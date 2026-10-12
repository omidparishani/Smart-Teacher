package ir.omid.smartteacher.billing.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreatePlanRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotNull BigDecimal price,
        String currency,
        String billingPeriod,
        Integer trialDays,
        Boolean isPublic,
        Boolean isDefault,
        Integer sortOrder
) {}
