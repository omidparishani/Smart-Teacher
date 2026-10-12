package ir.omid.smartteacher.eitaa;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * داده آزمون ایتا — ساخت {@code initData} امضاشده و پیکربندی.
 *
 * <p><b>⚠️ چرا این کلاس امضای خودش را حساب می‌کند و از
 * {@code LocalHmacVerifier} استفاده نمی‌کند؟</b> این مهم‌ترین تصمیم این
 * فایل است. اگر fixture از همان کدی امضا می‌ساخت که اعتبارسنجی می‌کند،
 * جهت اشتباه کلید (نگاه کنید به {@code LocalHmacVerifier}) در هر دو طرف
 * تکرار می‌شد و آزمون‌ها <i>سبز</i> می‌شدند در حالی که Production با
 * ایتای واقعی هرگز کار نمی‌کرد.
 *
 * <p>پس اینجا الگوریتم مستقل و از روی سند رسمی پیاده شده است. تنها
 * جای پیوند، بردار آزمون مستندشده در
 * {@link EitaaSecretKeyDerivationTest} است که <i>هر دو</i> پیاده‌سازی را
 * با یک مقدار بیرونی مقایسه می‌کند.
 *
 * <p>توکن‌های این فایل، راز واقعی نیستند.
 */
public final class EitaaTestData {

    /**
     * توکن نمونه — همان توکنی که {@code docs/eitaa.md} بخش ۷.۲ به‌عنوان
     * بردار آزمون ثبت کرده است.
     */
    public static final String BOT_TOKEN = "5768337691:AAGDAe6rjxu1cUgxK4BizYi--Utc3J9v5AU";

    /** توکن دیگر — برای آزمون «امضای توکن دیگر نباید بخواند». */
    public static final String OTHER_BOT_TOKEN =
            "9876543210:AAFotherbottokenthathasnothingtodowiththefirstone";

    /** توکن برنامک نمونه — مسیر ب. */
    public static final String APP_TOKEN = "app-token-for-tests-not-a-real-secret";

    /**
     * کلید مشتق‌شده از {@link #BOT_TOKEN} — مقدار <b>مستندشده</b>.
     *
     * <p>به‌صورت مستقل هم بازمحاسبه شد: HMAC-SHA256 با کلید
     * {@code "WebAppData"} و پیام = توکن، همین مقدار را می‌دهد.
     */
    public static final String DOCUMENTED_SECRET_KEY_HEX =
            "aa492a44bdf019c759defb1698c1d77690189973945491a756051cdc1207a449";

    /**
     * همان HMAC با دو ورودی <b>جابه‌جاشده</b> — کلید = توکن، پیام =
     * {@code "WebAppData"}.
     *
     * <p>وجود این ثابت، آزمون منفی را ممکن می‌کند: اگر روزی کسی جهت
     * کلید را برگرداند، مقدار مشتق‌شده <i>این</i> می‌شود و آزمون با
     * پیامی روشن می‌افتد.
     */
    public static final String REVERSED_DIRECTION_HEX =
            "7a5b484595b5ac6a6d0697ff0dd2f17525faad9d72a6995b738da8cea793cb7e";

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    /** پنجره تازگی پیش‌فرض آزمون — همان پیش‌فرض Production. */
    public static final Duration MAX_AGE = Duration.ofMinutes(5);

    private EitaaTestData() {
        throw new AssertionError("کلاس ابزاری نباید نمونه‌سازی شود");
    }

    // ─────────────────────────── پیکربندی ───────────────────────────

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * پیکربندی ایتا برای آزمون.
     *
     * <p>فقط توکن‌ها و عمر {@code initData} صریح‌اند؛ بقیه {@code null}
     * می‌مانند تا مقدار پیش‌فرض خودشان بیاید. این عمدی است: اگر آزمون
     * همه مقادیر را دستی بگذارد، دیگر نمی‌فهمد که پیش‌فرض‌ها چه هستند.
     */
    public static EitaaProperties properties(String botToken, String appToken) {
        return new EitaaProperties(
                botToken,
                appToken,
                null,   // baseUrl — پیش‌فرض
                null,   // appBaseUrl — پیش‌فرض
                null,   // getMeMethod — پیش‌فرض
                null,   // sendMessageMethod — پیش‌فرض
                null,   // sendFileMethod — پیش‌فرض
                null,   // connectTimeout — پیش‌فرض
                null,   // readTimeout — پیش‌فرض
                MAX_AGE,
                false,  // remoteVerifyEnabled
                4096,   // maxMessageLength
                // ⚠️ نرخ بالا و بدون سقف تلاش: آزمون‌ها نباید به‌خاطر
                // محدودکننده نرخ کند یا شکننده شوند. این مقادیر فقط برای
                // آزمون‌اند و هیچ ارتباطی با پیکربندی Production ندارند.
                1000,
                1,
                false); // trustForwardedFor
    }

    /** پیکربندی فقط با توکن ربات — مسیر الف. */
    public static EitaaProperties botProperties() {
        return properties(BOT_TOKEN, null);
    }

    /** پیکربندی با هر دو توکن. */
    public static EitaaProperties fullProperties() {
        return properties(BOT_TOKEN, APP_TOKEN);
    }

    /** پیکربندی بدون هیچ توکنی. */
    public static EitaaProperties emptyProperties() {
        return properties(null, null);
    }

    /** پیکربندی با پنجره تازگی دلخواه — برای آزمون لبه‌های پنجره. */
    public static EitaaProperties withMaxAge(Duration maxAge) {
        return new EitaaProperties(
                BOT_TOKEN, null,
                null, null, null, null, null, null, null,
                maxAge,
                false, 4096, 1000, 1, false);
    }

    /**
     * همان پیکربندی، ولی رو به یک ریشه دیگر.
     *
     * <p>برای آزمون‌هایی که به یک سرور محلی نگاه می‌کنند. بدون این،
     * آزمون مجبور می‌شد به {@code eitaayar.ir} واقعی درخواست بفرستد —
     * که هم شکننده است و هم می‌تواند پیام واقعی بفرستد.
     */
    public static EitaaProperties withBaseUrl(EitaaProperties base,
                                              String baseUrl,
                                              String appBaseUrl) {
        return new EitaaProperties(
                base.botToken(), base.appToken(), baseUrl, appBaseUrl,
                base.getMeMethod(), base.sendMessageMethod(), base.sendFileMethod(),
                base.connectTimeout(), base.readTimeout(), base.initDataMaxAge(),
                base.remoteVerifyEnabled(), base.maxMessageLength(),
                base.sendRatePerSecond(), base.maxSendAttempts(), base.trustForwardedFor());
    }

    /** همان پیکربندی با سقف طول پیام دلخواه — برای آزمون تقسیم پیام. */
    public static EitaaProperties withMaxMessageLength(EitaaProperties base, int maxLength) {
        return new EitaaProperties(
                base.botToken(), base.appToken(), base.baseUrl(), base.appBaseUrl(),
                base.getMeMethod(), base.sendMessageMethod(), base.sendFileMethod(),
                base.connectTimeout(), base.readTimeout(), base.initDataMaxAge(),
                base.remoteVerifyEnabled(), maxLength,
                base.sendRatePerSecond(), base.maxSendAttempts(), base.trustForwardedFor());
    }

    // ─────────────────────────── ساخت initData ───────────────────────────

    /**
     * پارامترهای پایه به‌شکل <b>URL-encoded</b> — همان‌طور که ایتا
     * می‌فرستد و همان‌طور که باید امضا شود.
     */
    public static Map<String, String> baseParams(long eitaaUserId, Instant authDate) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(authDate.getEpochSecond()));
        params.put("query_id", "AAF" + eitaaUserId);
        params.put("user", encode(userJson(eitaaUserId)));
        params.put("chat_type", "private");
        return params;
    }

    /** همان پارامترهای پایه با {@code auth_date} همین حالا. */
    public static Map<String, String> baseParams(long eitaaUserId) {
        return baseParams(eitaaUserId, Instant.now());
    }

    /** JSON فیلد {@code user} — پیش از URL-encode. */
    public static String userJson(long eitaaUserId) {
        return "{\"id\":" + eitaaUserId
                + ",\"first_name\":\"علی\""
                + ",\"last_name\":\"احمدی\""
                + ",\"language_code\":\"fa\""
                + ",\"allows_write_to_pm\":true}";
    }

    public static String encode(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }

    /**
     * رشته {@code data_check_string} — مرتب‌شده به ترتیب الفبا و متصل
     * با {@code \n}. همان چیزی که باید امضا شود.
     */
    public static String dataCheckString(Map<String, String> encodedParams) {
        return new TreeMap<>(encodedParams).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    /**
     * ساخت {@code initData} امضاشده.
     *
     * <p>ترتیب پارامترها در رشته خروجی، همان ترتیب درج در Map است؛
     * امضا به آن وابسته نیست (روی نسخه مرتب‌شده حساب می‌شود). همین
     * ویژگی، آزمون «همان داده با ترتیب دیگر» را ممکن می‌کند.
     */
    public static String signed(Map<String, String> encodedParams, String botToken) {
        String hash = hmacHex(secretKey(botToken), dataCheckString(encodedParams));

        StringBuilder initData = new StringBuilder();
        encodedParams.forEach((key, value) ->
                initData.append(key).append('=').append(value).append('&'));
        initData.append("hash=").append(hash);

        return initData.toString();
    }

    public static String signed(Map<String, String> encodedParams) {
        return signed(encodedParams, BOT_TOKEN);
    }

    /** {@code initData} معتبر و تازه برای یک کاربر. */
    public static String validInitData(long eitaaUserId) {
        return signed(baseParams(eitaaUserId));
    }

    // ─────────────────────────── رمزنگاری ───────────────────────────

    /**
     * استخراج کلید امضا از توکن ربات.
     *
     * <p>🔴 مرحله ۵ سند رسمی: کلید = {@code "WebAppData"}، پیام = توکن.
     */
    public static byte[] secretKey(String botToken) {
        return hmac("WebAppData".getBytes(StandardCharsets.UTF_8),
                botToken.getBytes(StandardCharsets.UTF_8));
    }

    public static String secretKeyHex(String botToken) {
        return java.util.HexFormat.of().formatHex(secretKey(botToken));
    }

    public static byte[] hmac(byte[] key, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(message);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 در دسترس نیست", e);
        }
    }

    public static String hmacHex(byte[] key, String message) {
        return java.util.HexFormat.of()
                .formatHex(hmac(key, message.getBytes(StandardCharsets.UTF_8)));
    }
}
