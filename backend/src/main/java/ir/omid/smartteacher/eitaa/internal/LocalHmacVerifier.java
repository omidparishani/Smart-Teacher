package ir.omid.smartteacher.eitaa.internal;

import ir.omid.smartteacher.eitaa.EitaaHashVerifier;
import ir.omid.smartteacher.eitaa.EitaaProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * اعتبارسنجی امضای {@code initData} با HMAC-SHA256 محلی — پیاده‌سازی
 * پیش‌فرض ({@code docs/eitaa.md} بخش ۷.۲).
 *
 * <p><b>🔴 دام جهت کلید — مهم‌ترین نکته این کلاس:</b>
 * <pre>
 *   secret_key = HMAC_SHA256(key = "WebAppData", message = &lt;توکن ربات&gt;)
 *   computed   = HMAC_SHA256(key = secret_key,  message = data_check_string)
 * </pre>
 * یعنی در مرحله اول <b>کلید رشته ثابت {@code WebAppData} است و پیام،
 * توکن ربات</b>. برعکس‌کردن این دو، یک اشتباه بسیار رایج است: کد
 * کامپایل می‌شود، اجرا می‌شود، و <i>همیشه</i> ناموفق است بدون هیچ
 * سرنخی. بردار آزمون مستندشده در
 * {@code EitaaSecretKeyDerivationTest} ثابت شده تا این جهت هرگز
 * برنگردد.
 *
 * <p><b>⚠️ مقایسه زمان-ثابت.</b> از {@link MessageDigest#isEqual} استفاده
 * می‌شود، نه {@code String.equals}. مقایسه معمولی در اولین بایت
 * متفاوت برمی‌گردد و زمان اجرایش به مقدار امضا وابسته می‌شود؛ با
 * اندازه‌گیری زمان می‌شود بایت‌به‌بایت امضا را حدس زد. این حمله در
 * شبکه‌های واقعی سخت ولی ممکن است، و بستنش یک خط کد است.
 */
@Component
@Primary
public class LocalHmacVerifier implements EitaaHashVerifier {

    /**
     * رشته ثابت ایتا برای استخراج کلید — <b>کلید است، نه پیام</b>.
     *
     * <p>این مقدار در سند رسمی ایتا ثابت است و راز نیست. راز، توکن
     * رباتی است که در مرحله <i>پیام</i> می‌آید.
     */
    private static final String WEBAPP_DATA_KEY = "WebAppData";

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final EitaaProperties properties;

    public LocalHmacVerifier(EitaaProperties properties) {
        this.properties = properties;

        if (properties.remoteVerifyEnabled()) {
            // ⚠️ Fail-Fast و نه نادیده‌گرفتن. اگر این پرچم روشن باشد و
            // ما بی‌صدا HMAC محلی را ادامه دهیم، کسی که پرچم را روشن
            // کرده فکر می‌کند یک لایه تأیید اضافه دارد که در واقع وجود
            // ندارد. شکست در بالا آمدن، صادقانه‌تر از یک امنیت خیالی است.
            throw new IllegalStateException(
                    "اعتبارسنجی از راه دور ایتا در این فاز پیاده‌سازی نشده است. "
                            + "APP_EITAA_REMOTE_VERIFY_ENABLED را false بگذارید — "
                            + "HMAC محلی همان تضمین امنیتی را می‌دهد "
                            + "(docs/eitaa.md بخش ۷.۳).");
        }
    }

    @Override
    public boolean verify(String dataCheckString, String receivedHash) {
        if (dataCheckString == null || receivedHash == null || receivedHash.isBlank()) {
            return false;
        }

        byte[] computed = hmac(deriveSecretKey(requireBotToken()), dataCheckString);

        // ⚠️ مقایسه زمان-ثابت — هرگز String.equals.
        return MessageDigest.isEqual(
                HexFormat.of().formatHex(computed).getBytes(StandardCharsets.UTF_8),
                receivedHash.trim().toLowerCase(java.util.Locale.ROOT)
                        .getBytes(StandardCharsets.UTF_8));
    }

    /**
     * استخراج کلید امضا از توکن ربات — همان «مرحله ۵» الگوریتم.
     *
     * <p>عمومی است چون آزمون بردار مستندشده باید <i>همین</i> مسیر
     * Production را بسنجد، نه یک کپی موازی. افشا نمی‌کند: ورودی‌اش خودِ
     * توکن ربات است، پس کسی که توکن را ندارد نمی‌تواند از آن چیزی
     * بسازد.
     *
     * @param botToken توکن ربات ایتا
     * @return کلید امضا به‌صورت hex — <b>هرگز در Log نرود</b>
     */
    public static String deriveSecretKeyHex(String botToken) {
        return HexFormat.of().formatHex(deriveSecretKey(botToken));
    }

    private static byte[] deriveSecretKey(String botToken) {
        return hmac(
                WEBAPP_DATA_KEY.getBytes(StandardCharsets.UTF_8),
                botToken.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] hmac(byte[] key, String message) {
        return hmac(key, message.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] hmac(byte[] key, byte[] message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(message);
        } catch (GeneralSecurityException e) {
            // HmacSHA256 در هر JVM استانداردی هست؛ رسیدن به اینجا یعنی
            // محیط اجرا خراب است، نه ورودی کاربر.
            throw new IllegalStateException("الگوریتم HmacSHA256 در این محیط در دسترس نیست", e);
        }
    }

    private String requireBotToken() {
        if (!properties.hasBotToken()) {
            throw new IllegalStateException(
                    "توکن ربات ایتا تنظیم نشده است؛ بدون آن اعتبارسنجی initData ممکن نیست. "
                            + "متغیر محیطی APP_EITAA_BOT_TOKEN را مقدار بدهید.");
        }
        return properties.botToken();
    }
}
