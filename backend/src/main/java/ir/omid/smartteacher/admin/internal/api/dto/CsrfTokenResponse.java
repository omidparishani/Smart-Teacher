package ir.omid.smartteacher.admin.internal.api.dto;

/**
 * توکن CSRF برای SPA پنل ادمین.
 *
 * <p>کوکی نشست HttpOnly هنوز پیاده نشده؛ Bearer تست‌ها را نمی‌شکند.
 * این Endpoint حداقل قرارداد CSRF را برای کلاینت کوکی‌محور آماده می‌کند.
 *
 * @param token      مقدار توکن
 * @param headerName نام هدری که کلاینت باید بفرستد
 * @param parameterName نام پارامتر فرم (برای سازگاری)
 */
public record CsrfTokenResponse(
        String token,
        String headerName,
        String parameterName
) {
}
