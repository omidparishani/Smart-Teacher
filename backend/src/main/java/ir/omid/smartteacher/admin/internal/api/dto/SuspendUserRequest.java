package ir.omid.smartteacher.admin.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * درخواست تعلیق کاربر — دلیل برای حسابرسی الزامی است.
 *
 * @param reason توضیح فارسی/انگلیسی دلیل تعلیق (حداکثر ۵۰۰ کاراکتر)
 */
public record SuspendUserRequest(

        @NotBlank(message = "دلیل تعلیق الزامی است")
        @Size(max = 500, message = "دلیل تعلیق نباید بیش از ۵۰۰ کاراکتر باشد")
        String reason
) {
}
