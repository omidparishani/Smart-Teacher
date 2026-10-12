package ir.omid.smartteacher.security.totp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * پیکربندی TOTP — {@code app.security.totp.*}.
 *
 * @param enabled اگر false باشد ورود بدون کد 2FA هم قبول می‌شود (تست)
 * @param issuer  نام صادرکننده در otpauth URL
 */
@ConfigurationProperties(prefix = "app.security.totp")
public record TotpProperties(boolean enabled, String issuer) {

    public TotpProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "SmartTeacher";
        }
    }
}
