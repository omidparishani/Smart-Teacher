package ir.omid.smartteacher.eitaa.internal.api.dto;

import jakarta.validation.constraints.NotBlank;

public record EnqueueChannelPublishRequest(@NotBlank String topic) {
}
