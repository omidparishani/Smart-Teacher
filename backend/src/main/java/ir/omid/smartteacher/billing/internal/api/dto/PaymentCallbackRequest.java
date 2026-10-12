package ir.omid.smartteacher.billing.internal.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentCallbackRequest(
        @NotBlank String authority
) {
}
