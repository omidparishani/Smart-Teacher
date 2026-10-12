package ir.omid.smartteacher.ai.internal.api.dto;

import ir.omid.smartteacher.ai.internal.domain.AiProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * ورودی ساخت Provider توسط ادمین.
 *
 * <p>کلید خام فقط در این درخواست می‌آید و در پاسخ هرگز برنمی‌گردد.
 */
public record CreateAiProviderRequest(
        @NotBlank(message = "کد Provider اجباری است.")
        @Size(max = 50, message = "کد نمی‌تواند بیش از ۵۰ نویسه باشد.")
        String code,

        @NotBlank(message = "نام Provider اجباری است.")
        @Size(max = 150, message = "نام نمی‌تواند بیش از ۱۵۰ نویسه باشد.")
        String name,

        @NotNull(message = "نوع Provider اجباری است.")
        AiProviderType type,

        @Size(max = 500, message = "آدرس پایه نمی‌تواند بیش از ۵۰۰ نویسه باشد.")
        String baseUrl,

        @Size(max = 2_000, message = "کلید API بیش از حد طولانی است.")
        String apiKey,

        Boolean enabled
) {
}
