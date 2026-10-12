package ir.omid.smartteacher.admin.internal.api.dto;

import ir.omid.smartteacher.tenant.TenantStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ویرایش مدرسه — نام، وضعیت و تنظیمات.
 *
 * @param name     نام نمایشی
 * @param status   وضعیت
 * @param settings JSON؛ {@code null} یعنی بدون تغییر
 */
public record UpdateAdminSchoolRequest(

        @NotBlank(message = "نام مدرسه الزامی است")
        @Size(max = 200, message = "نام نباید بیش از ۲۰۰ نویسه باشد")
        String name,

        TenantStatus status,

        @Size(max = 4000, message = "تنظیمات بیش از حد بلند است")
        String settings
) {
}
