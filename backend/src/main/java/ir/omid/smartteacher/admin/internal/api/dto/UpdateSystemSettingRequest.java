package ir.omid.smartteacher.admin.internal.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSystemSettingRequest(
        @NotNull(message = "مقدار تنظیم الزامی است")
        String value
) {
}
