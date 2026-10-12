package ir.omid.smartteacher.eitaa.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * درخواست تازه‌سازی یا خروج — هر دو یک ورودی دارند: توکن refresh.
 *
 * @param refreshToken توکن refresh خام همان‌طور که کلاینت دریافت کرده
 */
public record RefreshTokenRequest(

        @NotBlank(message = "توکن تازه‌سازی الزامی است")
        @Size(max = 512, message = "توکن تازه‌سازی معتبر نیست")
        String refreshToken
) {

    /**
     * ⚠️ عمداً بازنویسی شده: توکن refresh یک راز است و نباید در Log
     * بیفتد.
     */
    @Override
    public String toString() {
        return "RefreshTokenRequest{refreshToken=[REDACTED]}";
    }
}
