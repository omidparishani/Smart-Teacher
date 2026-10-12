package ir.omid.smartteacher.eitaa.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * درخواست ورود با ایتا.
 *
 * @param initData رشته خام {@code initData} از
 *                 {@code window.Eitaa.WebApp.initData}
 */
public record EitaaLoginRequest(

        @NotBlank(message = "initData الزامی است")
        // ⚠️ سقف طول عمدی است: بدون آن، یک درخواست چند مگابایتی
        // می‌تواند حافظه بگیرد و HMAC را روی حجم بزرگی حساب کند —
        // یک بردار DoS ساده. سقف واقعی initData ایتا بسیار کمتر از این
        // است؛ این عدد فقط یک حصار است، نه یک حد دقیق.
        @Size(max = 16_384, message = "initData بیش از حد بلند است")
        String initData
) {

    /**
     * ⚠️ عمداً بازنویسی شده: {@code initData} حاوی امضای نشست کاربر
     * است. اگر در Log بیفتد، هر کسی با دسترسی به فایل Log می‌تواند
     * تا زمان انقضا آن را بازپخش کند.
     */
    @Override
    public String toString() {
        return "EitaaLoginRequest{initData=[REDACTED]}";
    }
}
