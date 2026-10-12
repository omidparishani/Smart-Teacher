package ir.omid.smartteacher.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * پیکربندی CORS — <b>پیش‌فرض بسته</b>.
 *
 * <p><b>🔴 چرا این کلاس وجود دارد؟</b> Mini App در WebView ایتا از مبدأیی
 * غیر از مبدأ API اجرا می‌شود (و در توسعه، از {@code localhost:5173} به
 * {@code localhost:8080}). مرورگر برای چنین درخواستی نخست یک درخواست
 * {@code OPTIONS} می‌فرستد و بدون پاسخ درست، تماس شبکه‌ای <i>هرگز</i>
 * انجام نمی‌شود — با خطای {@code ERR_FAILED} که هیچ ردی در لاگ سرور
 * نمی‌گذارد. پیش از این کلاس، بک‌اند هیچ تنظیم CORS نداشت و همین اتفاق
 * می‌افتاد.
 *
 * <p><b>⚠️ چرا پیش‌فرض خالی است و نه {@code *}؟</b> چون CORS یک کنترل
 * امنیتی مرورگر است، نه یک تنظیم راحتی. یک پیش‌فرض باز یعنی هر سایتی
 * می‌تواند از مرورگر کاربرِ وارد‌شده به API ما درخواست بزند. با پیش‌فرض
 * خالی، استقرار در production تا وقتی مبدأهایش را <i>صریحاً</i>
 * فهرست نکند، هیچ مبدأیی را نمی‌پذیرد — همان اصل Fail-Closed که در
 * {@code docs/security.md} برای مستأجر آمده.
 *
 * <p>⚠️ wildcard ({@code *}) عمداً پشتیبانی نمی‌شود؛
 * {@link org.springframework.web.cors.CorsConfiguration} آن را فقط در
 * حالت بدون کوکی می‌پذیرد و اجازه‌دادنش یک اشتباه رایج و پرهزینه است.
 *
 * @param allowedOrigins مبدأهای مجاز، با scheme و پورت کامل
 *                       (مثل {@code http://localhost:5173}).
 *                       پیش‌فرض: خالی — یعنی هیچ‌کدام.
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    /**
     * ⚠️ کانستراکتور فشرده عمدی است: {@code null} را به فهرست خالی تبدیل
     * می‌کند. بدون آن، نبودِ کلید در YAML یک {@code NullPointerException}
     * در زمان ساخت می‌دهد و پیامش به «تنظیم CORS نکردی» اشاره‌ای ندارد.
     */
    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
