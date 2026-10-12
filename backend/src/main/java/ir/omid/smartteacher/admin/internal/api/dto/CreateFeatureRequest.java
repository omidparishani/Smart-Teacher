package ir.omid.smartteacher.admin.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFeatureRequest(
        @NotBlank(message = "کد امکان الزامی است")
        @Size(max = 60, message = "کد امکان حداکثر ۶۰ کاراکتر است")
        String code,
        @NotBlank(message = "نام امکان الزامی است")
        @Size(max = 200, message = "نام امکان حداکثر ۲۰۰ کاراکتر است")
        String name,
        @NotBlank(message = "دسته‌بندی امکان الزامی است")
        @Size(max = 30, message = "دسته‌بندی حداکثر ۳۰ کاراکتر است")
        String category,
        String description,
        Boolean defaultEnabled,
        Integer sortOrder
) {
}
