package ir.omid.smartteacher.security.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * پیکربندی محدودیت نرخ ورود ادمین.
 *
 * @param enabled     روشن/خاموش — در تست می‌توان خاموش کرد
 * @param maxRequests سقف درخواست در هر پنجره (پیش‌فرض ۱۰)
 * @param window      طول پنجره (پیش‌فرض ۱ دقیقه)
 */
@ConfigurationProperties(prefix = "app.rate-limit.login")
public record LoginRateLimitProperties(
        boolean enabled,
        int maxRequests,
        Duration window
) {

    public LoginRateLimitProperties {
        if (maxRequests <= 0) {
            maxRequests = 10;
        }
        if (window == null) {
            window = Duration.ofMinutes(1);
        }
    }
}
