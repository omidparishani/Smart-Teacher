package ir.omid.smartteacher.security.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * پیکربندی توکن‌ها.
 *
 * <p><b>⚠️ {@code secret} هیچ پیش‌فرضی ندارد.</b> اگر داشت، یک استقرار
 * اشتباه در production با کلید شناخته‌شده‌ای که در مخزن گیت است بالا
 * می‌آمد و هر مهاجمی می‌توانست توکن جعل کند. نبودِ پیش‌فرض باعث می‌شود
 * اپلیکیشن <b>بالا نیاید</b> — رفتار درست.
 *
 * <p>مقادیر غیرحساس پیش‌فرض دارند تا نبودشان در یک پروفایل، به خطای
 * مبهم منجر نشود.
 *
 * @param secret          کلید امضا، Base64 و حداقل ۳۲ بایت — فقط از محیط اجرا
 * @param issuer          صادرکننده؛ در اعتبارسنجی توکن بررسی می‌شود
 * @param audience        مخاطب؛ در اعتبارسنجی توکن بررسی می‌شود
 * @param accessTokenTtl  عمر Access Token — پیش‌فرض ۱۵ دقیقه
 * @param refreshTokenTtl عمر Refresh Token — پیش‌فرض ۳۰ روز
 * @param clockSkew       اختلاف ساعت مجاز بین سرورها — پیش‌فرض ۳۰ ثانیه
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        String audience,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        Duration clockSkew
) {

    /** حداقل طول کلید امضا — HS256 به ۲۵۶ بیت نیاز دارد. */
    public static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        issuer = (issuer == null || issuer.isBlank()) ? "smart-teacher" : issuer;
        audience = (audience == null || audience.isBlank()) ? "smart-teacher-api" : audience;
        accessTokenTtl = (accessTokenTtl == null) ? Duration.ofMinutes(15) : accessTokenTtl;
        refreshTokenTtl = (refreshTokenTtl == null) ? Duration.ofDays(30) : refreshTokenTtl;
        clockSkew = (clockSkew == null) ? Duration.ofSeconds(30) : clockSkew;
    }

    /**
     * ⚠️ عمداً بازنویسی شده تا کلید امضا هرگز در Log نیفتد.
     */
    @Override
    public String toString() {
        return "JwtProperties{issuer=" + issuer + ", audience=" + audience
                + ", accessTokenTtl=" + accessTokenTtl + ", refreshTokenTtl=" + refreshTokenTtl
                + ", secret=***}";
    }
}
