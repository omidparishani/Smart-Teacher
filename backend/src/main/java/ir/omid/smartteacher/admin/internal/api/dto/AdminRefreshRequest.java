package ir.omid.smartteacher.admin.internal.api.dto;

import jakarta.validation.constraints.Size;

/**
 * تازه‌سازی نشست ادمین — توکن در بدنه اختیاری است اگر کوکی {@code ST_ADMIN_REFRESH} باشد.
 *
 * @param refreshToken توکن refresh خام؛ برای سازگاری با تست‌های Bearer
 */
public record AdminRefreshRequest(

        @Size(max = 512, message = "توکن تازه‌سازی معتبر نیست")
        String refreshToken
) {

    @Override
    public String toString() {
        return "AdminRefreshRequest{refreshToken=[REDACTED]}";
    }
}
