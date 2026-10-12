package ir.omid.smartteacher.admin.internal.api.dto;

import jakarta.validation.constraints.Size;

/**
 * به‌روزرسانی امکان — پیش‌فرض فعال بودن و فراداده نمایشی.
 *
 * @param defaultEnabled پیش‌فرض؛ {@code null} یعنی بدون تغییر
 * @param name           نام؛ {@code null} یعنی بدون تغییر
 * @param description    توضیح؛ {@code null} یعنی بدون تغییر
 */
public record UpdateFeatureRequest(

        Boolean defaultEnabled,

        @Size(max = 200, message = "نام نباید بیش از ۲۰۰ نویسه باشد")
        String name,

        @Size(max = 2000, message = "توضیح نباید بیش از ۲۰۰۰ نویسه باشد")
        String description
) {
}
