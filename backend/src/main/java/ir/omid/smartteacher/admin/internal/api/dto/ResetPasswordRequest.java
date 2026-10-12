package ir.omid.smartteacher.admin.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * بازنشانی رمز کاربر توسط ادمین.
 *
 * @param newPassword رمز جدید خام — هرگز در Log یا حسابرسی
 */
public record ResetPasswordRequest(

        @NotBlank(message = "رمز جدید الزامی است")
        @Size(min = 8, max = 200, message = "رمز عبور باید بین ۸ تا ۲۰۰ نویسه باشد")
        String newPassword
) {

    @Override
    public String toString() {
        return "ResetPasswordRequest{newPassword=[REDACTED]}";
    }
}
