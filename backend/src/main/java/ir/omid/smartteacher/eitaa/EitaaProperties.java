package ir.omid.smartteacher.eitaa;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * پیکربندی یکپارچه‌سازی ایتا.
 *
 * <p><b>⚠️ دو توکن، دو راز — هیچ‌کدام پیش‌فرض ندارند.</b> اگر داشتند، یک
 * استقرار اشتباه در production با توکن شناخته‌شده‌ای که در مخزن گیت است
 * بالا می‌آمد و هر کسی می‌توانست به‌جای ما پیام بفرستد.
 *
 * <p>🔴 <b>اصلاح یک ادعای نادرست در همین مستند:</b> پیش‌تر اینجا نوشته
 * بود «نبودشان باعث می‌شود برنامه بالا نیاید — رفتار درست». <b>این غلط
 * است و برای توکن ربات اندازه‌گیری شد:</b>
 * {@link ir.omid.smartteacher.eitaa.internal.EitaaYarBotAdapter} در نبود
 * توکن فقط {@code log.warn} می‌دهد و {@code LocalHmacVerifier} در
 * {@code requireBotToken()} یک {@code IllegalStateException} در
 * <b>زمان درخواست</b> می‌اندازد، نه در راه‌اندازی. یعنی برنامه بالا
 * می‌آید، {@code /actuator/health} سبز است (این مسیر به ایتا وابسته
 * نیست)، و کاربر تازه موقع اولین ارسال متوجه می‌شود.
 *
 * <p>در عمل توکن‌ها از محیط می‌آیند و مسیر عادی این است که نبودشان
 * زود — در همان راه‌اندازی — دیده شود. اینجا عمداً <b>تغییر رفتار
 * ندادیم</b> (چون استقراری که موقتاً ایتا نمی‌خواهد نباید بلاک شود)،
 * ولی این تفاوت مستند می‌شود تا کسی روی «بالا نمی‌آید» حساب نکند.
 *
 * <p>⚠️ نکته‌ای که همین اشتباه را پنهان می‌کرد: نام متغیر. متدهای
 * binding خودکار نسبت به نام کوتاه {@code EITAA_BOT_TOKEN} کورند؛ نام
 * درست {@code APP_EITAA_BOT_TOKEN} است (کلید
 * {@code app.eitaa.bot-token}). نام کوتاه فقط جایی کار می‌کند که
 * صریحاً نگاشت شده باشد. بعد از اصلاح، هر دو صورت — هم در
 * {@code application-dev.yml} و هم در {@code application-prod.yml} —
 * نگاشت شده‌اند.
 *
 * <p>در حالت توسعه ({@code app.mock.eitaa=true}) توکن لازم نیست، چون
 * {@link ir.omid.smartteacher.eitaa.internal.MockEitaaAdapter} جای
 * فراخوانی واقعی را می‌گیرد. اعتبارسنجی نبودِ توکن در همان آداپتور
 * واقعی انجام می‌شود، نه اینجا — وگرنه محیط توسعه بدون توکن هم بالا
 * نمی‌آمد.
 *
 * <p><b>⚠️ درباره مقادیری که «محتاطانه» انتخاب شده‌اند:</b>
 * {@code maxMessageLength}، {@code sendRatePerSecond} و
 * {@code sendFileMethod} با آزمون واقعی تأیید <b>نشده‌اند</b>
 * ({@code docs/eitaa.md} بخش ۱۱.۲). هر سه قابل تنظیم‌اند تا وقتی آزمون
 * واقعی با توکن واقعی انجام شد، بدون تغییر کد اصلاح شوند. این‌ها حدس
 * نیستند که به‌جای حقیقت جا بزنند؛ مقادیر محافظه‌کارانه‌ای هستند که
 * صریحاً به‌عنوان تأییدنشده علامت‌گذاری شده‌اند.
 *
 * @param botToken            توکن ربات ایتایار — مسیر الف (گروه/کانال).
 *                            از {@code APP_EITAA_BOT_TOKEN} (نام
 *                            canonical) یا {@code EITAA_BOT_TOKEN}.
 * @param appToken            توکن برنامک — مسیر ب (کاربر خصوصی).
 *                            از {@code APP_EITAA_APP_TOKEN} (نام
 *                            canonical) یا {@code EITAA_APP_TOKEN}.
 * @param baseUrl             ریشه API ربات؛ پیش‌فرض {@code https://eitaayar.ir/api}
 * @param appBaseUrl          ریشه API برنامک؛ پیش‌فرض {@code https://eitaayar.ir/api/app}
 * @param getMeMethod         نام متد اطلاعات ربات؛ پیش‌فرض {@code getMe}
 * @param sendMessageMethod   نام متد ارسال متن؛ پیش‌فرض {@code sendMessage}
 * @param sendFileMethod      نام متد ارسال فایل.
 *                            ⚠️ <b>تأییدنشده:</b> سند رسمی در فهرست ابتدایی
 *                            {@code sendDocument} و در شرح کامل {@code sendFile}
 *                            می‌گوید. تا آزمون واقعی، {@code sendFile} فرض
 *                            می‌شود و از همین‌جا قابل تغییر است.
 * @param connectTimeout      مهلت برقراری اتصال؛ پیش‌فرض ۵ ثانیه
 * @param readTimeout         مهلت خواندن پاسخ؛ پیش‌فرض ۱۵ ثانیه
 * @param initDataMaxAge      پنجره اعتبار {@code initData}؛ پیش‌فرض ۵ دقیقه
 * @param remoteVerifyEnabled اعتبارسنجی از راه دور ایتا — پیش‌فرض خاموش
 *                            ({@code docs/eitaa.md} بخش ۷.۳: سقف ۲۰ req/s و
 *                            مسدودشدن IP، آن را برای ما غیرقابل‌استفاده می‌کند)
 * @param maxMessageLength    سقف طول پیام.
 *                            ⚠️ <b>تأییدنشده</b> — مقدار محافظه‌کارانه.
 * @param sendRatePerSecond   سقف ارسال در ثانیه برای هر توکن.
 *                            ⚠️ <b>تأییدنشده</b> — مقدار محافظه‌کارانه.
 * @param maxSendAttempts     تعداد تلاش در برابر خطای شبکه؛ پیش‌فرض ۳
 * @param trustForwardedFor   اعتماد به هدر {@code X-Forwarded-For} برای IP کاربر.
 *                            پیش‌فرض {@code false} — هدری که کلاینت می‌تواند
 *                            جعل کند، مبنای تصمیم امنیتی نمی‌شود.
 */
@ConfigurationProperties(prefix = "app.eitaa")
public record EitaaProperties(
        String botToken,
        String appToken,
        String baseUrl,
        String appBaseUrl,
        String getMeMethod,
        String sendMessageMethod,
        String sendFileMethod,
        Duration connectTimeout,
        Duration readTimeout,
        Duration initDataMaxAge,
        boolean remoteVerifyEnabled,
        int maxMessageLength,
        int sendRatePerSecond,
        int maxSendAttempts,
        boolean trustForwardedFor
) {

    /**
     * سقف محافظه‌کارانه طول پیام.
     *
     * <p>⚠️ سند رسمی ایتا حداکثر طول متن را نمی‌گوید. این عدد عمداً
     * پایین انتخاب شده تا پیام‌های بلند به‌جای ردشدن، تقسیم شوند.
     */
    public static final int DEFAULT_MAX_MESSAGE_LENGTH = 4096;

    public EitaaProperties {
        baseUrl = orDefault(baseUrl, "https://eitaayar.ir/api");
        appBaseUrl = orDefault(appBaseUrl, "https://eitaayar.ir/api/app");
        getMeMethod = orDefault(getMeMethod, "getMe");
        sendMessageMethod = orDefault(sendMessageMethod, "sendMessage");
        sendFileMethod = orDefault(sendFileMethod, "sendFile");
        connectTimeout = (connectTimeout == null) ? Duration.ofSeconds(5) : connectTimeout;
        readTimeout = (readTimeout == null) ? Duration.ofSeconds(15) : readTimeout;
        initDataMaxAge = (initDataMaxAge == null) ? Duration.ofMinutes(5) : initDataMaxAge;
        maxMessageLength = (maxMessageLength <= 0)
                ? DEFAULT_MAX_MESSAGE_LENGTH : maxMessageLength;
        sendRatePerSecond = (sendRatePerSecond <= 0) ? 10 : sendRatePerSecond;
        maxSendAttempts = (maxSendAttempts <= 0) ? 3 : maxSendAttempts;
    }

    /** آیا توکن مسیر الف تنظیم شده است؟ */
    public boolean hasBotToken() {
        return botToken != null && !botToken.isBlank();
    }

    /** آیا توکن مسیر ب تنظیم شده است؟ */
    public boolean hasAppToken() {
        return appToken != null && !appToken.isBlank();
    }

    /**
     * توکن ربات به‌شکل نوع ماسک‌شده — یا {@code null} اگر تنظیم نشده باشد.
     *
     * <p>این متد تنها راه رسیدن به توکن است. {@code EitaaBotToken} در
     * {@code toString()} خود مقدار را پنهان می‌کند، پس حتی اگر یک شیء
     * ناخواسته در Log بیفتد، توکن نمی‌افتد.
     */
    public EitaaBotToken botTokenOrNull() {
        return hasBotToken() ? new EitaaBotToken(botToken) : null;
    }

    /** توکن برنامک به‌شکل نوع ماسک‌شده — یا {@code null} اگر تنظیم نشده باشد. */
    public EitaaAppToken appTokenOrNull() {
        return hasAppToken() ? new EitaaAppToken(appToken) : null;
    }

    private static String orDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    /**
     * ⚠️ عمداً بازنویسی شده تا <b>هیچ‌کدام</b> از دو توکن در Log نیفتد.
     * این همان قاعده بخش ۵۳ خواسته‌ها است: کلید API نباید در Log،
     * Swagger، حسابرسی یا استثنا ظاهر شود.
     */
    @Override
    public String toString() {
        return "EitaaProperties{baseUrl=" + baseUrl + ", appBaseUrl=" + appBaseUrl
                + ", botToken=" + (hasBotToken() ? "***" : "(تنظیم‌نشده)")
                + ", appToken=" + (hasAppToken() ? "***" : "(تنظیم‌نشده)")
                + ", sendFileMethod=" + sendFileMethod
                + ", initDataMaxAge=" + initDataMaxAge
                + ", remoteVerifyEnabled=" + remoteVerifyEnabled + '}';
    }
}
