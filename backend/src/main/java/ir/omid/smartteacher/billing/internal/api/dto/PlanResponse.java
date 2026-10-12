package ir.omid.smartteacher.billing.internal.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanResponse(
        UUID id,
        String code,
        String name,
        String description,
        BigDecimal price,
        String currency,
        String billingPeriod,
        Integer trialDays,
        boolean isPublic,
        boolean isDefault,
        int sortOrder,
        boolean active
) {}
