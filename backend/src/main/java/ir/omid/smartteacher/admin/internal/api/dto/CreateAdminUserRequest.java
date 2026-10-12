package ir.omid.smartteacher.admin.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * ساخت کاربر ادمین/ستاد با رمز — فقط نقش‌های پلتفرمی.
 *
 * @param username نام کاربری یکتا
 * @param password رمز خام — هرگز در Log
 * @param fullName نام کامل
 * @param email    ایمیل اختیاری
 * @param mobile   موبایل اختیاری
 * @param role     کد نقش پلتفرمی: {@code SUPER_ADMIN} یا {@code ADMIN}
 */
public record CreateAdminUserRequest(

        @NotBlank(message = "نام کاربری الزامی است")
        @Size(min = 3, max = 100, message = "نام کاربری باید بین ۳ تا ۱۰۰ نویسه باشد")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "نام کاربری فقط حروف لاتین، رقم، نقطه، خط تیره و زیرخط مجاز است")
        String username,

        @NotBlank(message = "رمز عبور الزامی است")
        @Size(min = 8, max = 200, message = "رمز عبور باید بین ۸ تا ۲۰۰ نویسه باشد")
        String password,

        @NotBlank(message = "نام کامل الزامی است")
        @Size(max = 200, message = "نام کامل نباید بیش از ۲۰۰ نویسه باشد")
        String fullName,

        @Size(max = 255, message = "ایمیل نباید بیش از ۲۵۵ نویسه باشد")
        String email,

        @Size(max = 20, message = "موبایل نباید بیش از ۲۰ نویسه باشد")
        String mobile,

        @NotBlank(message = "نقش الزامی است")
        @Pattern(regexp = "SUPER_ADMIN|ADMIN",
                message = "نقش باید SUPER_ADMIN یا ADMIN باشد")
        String role
) {

    @Override
    public String toString() {
        return "CreateAdminUserRequest{username=" + username
                + ", password=[REDACTED], role=" + role + '}';
    }
}
