package ir.omid.smartteacher.admin.internal.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * ایجاد یا جایگزینی بازنویسی امکان برای مستأجر یا کاربر.
 *
 * @param scope       {@code TENANT} یا {@code USER}
 * @param tenantId    الزامی وقتی scope=TENANT
 * @param userId      الزامی وقتی scope=USER
 * @param featureCode کد امکان
 * @param enabled     فعال/غیرفعال
 * @param reason      دلیل اختیاری برای حسابرسی
 * @param expiresAt   انقضا؛ {@code null} یعنی دائمی
 */
public record CreateFeatureOverrideRequest(

        @NotBlank(message = "حوزه بازنویسی الزامی است")
        @Pattern(regexp = "TENANT|USER", message = "حوزه باید TENANT یا USER باشد")
        String scope,

        UUID tenantId,

        UUID userId,

        @NotBlank(message = "کد امکان الزامی است")
        @Size(max = 60, message = "کد امکان نباید بیش از ۶۰ نویسه باشد")
        String featureCode,

        @NotNull(message = "وضعیت فعال بودن الزامی است")
        Boolean enabled,

        @Size(max = 500, message = "دلیل نباید بیش از ۵۰۰ نویسه باشد")
        String reason,

        Instant expiresAt
) {

    @AssertTrue(message = "برای حوزه TENANT باید tenantId و برای USER باید userId مشخص شود")
    public boolean isScopeTargetValid() {
        if ("TENANT".equals(scope)) {
            return tenantId != null && userId == null;
        }
        if ("USER".equals(scope)) {
            return userId != null && tenantId == null;
        }
        return false;
    }
}
