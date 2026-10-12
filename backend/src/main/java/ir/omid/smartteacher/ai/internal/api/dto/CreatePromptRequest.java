package ir.omid.smartteacher.ai.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ساخت قالب پرامپت.
 *
 * @param code        کد یکتا
 * @param name        نام نمایشی
 * @param featureCode کد فیچر
 * @param description توضیح اختیاری
 */
public record CreatePromptRequest(

        @NotBlank(message = "کد پرامپت الزامی است")
        @Size(max = 80)
        String code,

        @NotBlank(message = "نام پرامپت الزامی است")
        @Size(max = 200)
        String name,

        @NotBlank(message = "کد فیچر الزامی است")
        @Size(max = 80)
        String featureCode,

        @Size(max = 4000)
        String description
) {
}
