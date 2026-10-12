package ir.omid.smartteacher.admin.internal.api.dto;

import ir.omid.smartteacher.tenant.TenantType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * ساخت مستأجر توسط ادمین.
 *
 * @param type           نوع
 * @param name           نام نمایشی
 * @param slug           نامک یکتا (حروف کوچک، رقم، خط تیره)
 * @param ownerUserId    مالک — باید کاربر موجود باشد
 * @param parentTenantId والد اختیاری
 * @param settings       JSON اختیاری؛ پیش‌فرض {@code {}}
 */
public record CreateAdminTenantRequest(

        @NotNull(message = "نوع مستأجر الزامی است")
        TenantType type,

        @NotBlank(message = "نام مستأجر الزامی است")
        @Size(max = 200, message = "نام نباید بیش از ۲۰۰ نویسه باشد")
        String name,

        @NotBlank(message = "نامک الزامی است")
        @Size(max = 100, message = "نامک نباید بیش از ۱۰۰ نویسه باشد")
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "نامک فقط حروف کوچک لاتین، رقم و خط تیره مجاز است")
        String slug,

        @NotNull(message = "مالک مستأجر الزامی است")
        UUID ownerUserId,

        UUID parentTenantId,

        @Size(max = 4000, message = "تنظیمات بیش از حد بلند است")
        String settings
) {
}
