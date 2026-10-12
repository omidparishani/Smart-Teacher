package ir.omid.smartteacher.admin.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * تغییر نقش پلتفرمی کاربر.
 *
 * @param role کد نقش: {@code SUPER_ADMIN} یا {@code ADMIN}
 */
public record ChangeRoleRequest(

        @NotBlank(message = "نقش الزامی است")
        @Pattern(regexp = "SUPER_ADMIN|ADMIN",
                message = "نقش باید SUPER_ADMIN یا ADMIN باشد")
        String role
) {
}
