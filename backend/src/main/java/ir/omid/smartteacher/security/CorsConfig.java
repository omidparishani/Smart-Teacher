package ir.omid.smartteacher.security;

import ir.omid.smartteacher.config.CorsProperties;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * منبع پیکربندی CORS — <b>فهرست صریح مبدأها، پیش‌فرض بسته</b>.
 *
 * <p>نام Bean عمداً {@code corsConfigurationSource} است: زنجیره‌های امنیت
 * در {@code SecurityConfig} با {@code .cors(withDefaults())} همین نام را
 * جست‌وجو می‌کنند. تغییر نام، CORS را در زنجیره امنیت بی‌صدا خاموش
 * می‌کند.
 *
 * <p><b>🔴 چرا این Bean لازم است و {@code @CrossOrigin} کافی نیست؟</b>
 * چون درخواست {@code OPTIONS} (preflight) پیش از هر کنترلر و پیش از
 * احراز هویت می‌آید. {@code @CrossOrigin} روی کنترلر، بعد از عبور از
 * زنجیره فیلتر امنیت اجرا می‌شود — یعنی وقتی به آن می‌رسد که درخواست
 * <i>پیش‌تر</i> با ۴۰۱ رد شده. نتیجه‌اش این است که preflight هرگز پاسخ
 * درست نمی‌گیرد و تماس واقعی هرگز فرستاده نمی‌شود.
 *
 * <p>⚠️ <b>درباره {@code allowCredentials=false}:</b> برنامک با توکن
 * {@code Authorization} و {@code credentials: 'omit'} کار می‌کند
 * ({@code miniapp/src/lib/api-client.ts})، پس نیازی به اشتراک کوکی
 * ندارد. اگر روزی پنل مدیریت هم از مبدأیی غیر از API سرو شود، آن مسیر
 * کوکی HttpOnly دارد و این پرچم باید {@code true} شود — و آن یک تغییر
 * امنیتی است، نه یک تنظیم ساده.
 */
@Configuration
public class CorsConfig {

    /**
     * فقط مسیرهای API — دارایی‌های استاتیک و {@code /actuator} بیرون
     * می‌مانند. دلیلش باریک نگه داشتن سطح است: هر مسیری که اینجا
     * ثبت شود، از مرورگر قابل فراخوانی می‌شود.
     */
    private static final String API_PATH_PATTERN = "/api/**";

    private static final List<String> ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    /**
     * هدرهای مجاز — فهرست بسته، نه {@code *}.
     *
     * <p>⚠️ {@code X-XSRF-TOKEN} برای پنل مدیریت لازم است (توکن CSRF در
     * هدر می‌آید). {@code Authorization} برای برنامک.
     */
    private static final List<String> ALLOWED_HEADERS =
            List.of("Authorization", "Content-Type", "Accept", "X-XSRF-TOKEN", "X-Requested-With");

    /**
     * ⚠️ ۳۰ دقیقه: مقدار پیش‌فرض مرورگرها ۵ ثانیه است و هر درخواست
     * تغییردهنده یک preflight اضافه می‌کند. این عدد فقط کش مرورگر را
     * بلندتر می‌کند و سطح امنیتی را عوض نمی‌کند، چون فهرست مبدأها ثابت
     * است.
     */
    private static final Duration MAX_AGE = Duration.ofMinutes(30);

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration config = new CorsConfiguration();

        // ⚠️ فهرست خالی یعنی هیچ مبدأیی مجاز نیست. این عمدی است:
        // استقرار بدون تنظیم صریح، باز نمی‌شود.
        config.setAllowedOrigins(properties.allowedOrigins());
        config.setAllowedMethods(ALLOWED_METHODS);
        config.setAllowedHeaders(ALLOWED_HEADERS);
        config.setAllowCredentials(false);
        config.setMaxAge(MAX_AGE);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(API_PATH_PATTERN, config);

        return source;
    }
}
