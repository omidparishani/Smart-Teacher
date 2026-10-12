package ir.omid.smartteacher.admin.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * ساخت مدرسه — روی مستأجر با نوع {@code SCHOOL} نگاشت می‌شود.
 *
 * @param name           نام مدرسه
 * @param slug           نامک یکتا
 * @param ownerUserId    مالک
 * @param parentTenantId والد اختیاری (مثلاً سازمان)
 * @param settings       JSON اختیاری
 */
public record CreateAdminSchoolRequest(

        @NotBlank(message = "نام مدرسه الزامی است")
        @Size(max = 200, message = "نام نباید بیش از ۲۰۰ نویسه باشد")
        String name,

        @NotBlank(message = "نامک الزامی است")
        @Size(max = 100, message = "نامک نباید بیش از ۱۰۰ نویسه باشد")
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "نامک فقط حروف کوچک لاتین، رقم و خط تیره مجاز است")
        String slug,

        @NotNull(message = "مالک مدرسه الزامی است")
        UUID ownerUserId,

        UUID parentTenantId,

        @Size(max = 4000, message = "تنظیمات بیش از حد بلند است")
        String settings
) {
}
