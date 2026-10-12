package ir.omid.smartteacher.admin.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * درخواست ورود ادمین با نام کاربری و رمز (+ کد 2FA اختیاری).
 */
public record AdminLoginRequest(

        @NotBlank(message = "نام کاربری الزامی است")
        @Size(max = 100, message = "نام کاربری بیش از حد بلند است")
        String username,

        @NotBlank(message = "رمز عبور الزامی است")
        @Size(max = 200, message = "رمز عبور بیش از حد بلند است")
        String password,

        /** کد ۶ رقمی TOTP — وقتی 2FA فعال باشد الزامی است. */
        @Size(max = 10)
        String totpCode
) {

    public AdminLoginRequest(String username, String password) {
        this(username, password, null);
    }

    @Override
    public String toString() {
        return "AdminLoginRequest{username=" + username
                + ", password=[REDACTED], totpCode=[REDACTED]}";
    }
}
