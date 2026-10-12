package ir.omid.smartteacher.admin.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ویرایش پروفایل کاربر توسط ادمین — بدون رمز و نقش.
 *
 * @param fullName نام کامل
 * @param email    ایمیل؛ خالی یا {@code null} یعنی پاک‌کردن
 * @param mobile   موبایل؛ خالی یا {@code null} یعنی پاک‌کردن
 */
public record UpdateAdminUserRequest(

        @NotBlank(message = "نام کامل الزامی است")
        @Size(max = 200, message = "نام کامل نباید بیش از ۲۰۰ نویسه باشد")
        String fullName,

        @Size(max = 255, message = "ایمیل نباید بیش از ۲۵۵ نویسه باشد")
        String email,

        @Size(max = 20, message = "موبایل نباید بیش از ۲۰ نویسه باشد")
        String mobile
) {
}
