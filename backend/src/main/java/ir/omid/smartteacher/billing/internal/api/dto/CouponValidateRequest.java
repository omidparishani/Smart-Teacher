package ir.omid.smartteacher.billing.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CouponValidateRequest(
        @NotBlank String code,
        @NotNull UUID planId
) {
}
