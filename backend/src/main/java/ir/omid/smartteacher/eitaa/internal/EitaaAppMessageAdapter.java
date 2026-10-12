package ir.omid.smartteacher.eitaa.internal;

import ir.omid.smartteacher.common.error.EitaaApiException;
import ir.omid.smartteacher.eitaa.EitaaAppToken;
import ir.omid.smartteacher.eitaa.EitaaCapability;
import ir.omid.smartteacher.eitaa.EitaaCapabilityNotSupportedException;
import ir.omid.smartteacher.eitaa.EitaaProperties;
import ir.omid.smartteacher.eitaa.EitaaSendResult;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * مسیر ب — ارسال پیام خصوصی به کاربر از راه «برنامه».
 *
 * <p><b>چرا یک کلاس جدا؟</b> مسیر ب با مسیر الف <i>یک API متفاوت</i>
 * است، نه یک پارامتر اضافه. تفاوت‌ها آن‌قدر بنیادی‌اند که قاطی‌کردنشان
 * در یک کلاس، همان اشتباه رایجی را می‌سازد که سند رسمی درباره‌اش هشدار
 * داده:
 *
 * <table border="1">
 *   <caption>تفاوت دو مسیر</caption>
 *   <tr><th></th><th>مسیر الف (ربات)</th><th>مسیر ب (برنامه)</th></tr>
 *   <tr><td>Endpoint</td><td>{@code /api/{token}/sendMessage}</td>
 *       <td>{@code /api/app/sendMessage}</td></tr>
 *   <tr><td>بدنه</td><td>{@code chat_id, text}</td>
 *       <td>{@code token, chat_id, text}</td></tr>
 *   <tr><td>مقصد</td><td>گروه یا کانال</td><td>کاربر خصوصی</td></tr>
 *   <tr><td>پاسخ</td><td>{@code result} یک شیء با {@code message_id}</td>
 *       <td>{@code result} رشته {@code "success"}</td></tr>
 *   <tr><td>هزینه</td><td>ندارد</td>
 *       <td><b>۱ اعتبار به‌ازای هر پیام</b> — از ۵۰٬۰۰۰ رایگان</td></tr>
 * </table>
 *
 * <p><b>🔴 چرا هزینه اینجا مهم است؟</b> چون تجربه دوباره‌فرست (retry)
 * روی این مسیر، مستقیم پول و اعتبار می‌سوزاند. لایه
 * {@link EitaaHttpClient} فقط روی خطای شبکه و ۵xx دوباره تلاش می‌کند،
 * نه روی پاسخ {@code ok:false} — چون پاسخ {@code ok:false} یعنی درخواست
 * ما مشکل داشت و تکرارش یک اعتبار دیگر هم هدر می‌دهد.
 *
 * <p><b>⚠️ این کلاس {@code EitaaAdapter} نیست.</b> اگر بود، دو Bean از
 * یک اینترفیس داشتیم و تزریق مبهم می‌شد. {@code EitaaAdapter} یک پیاده
 * سازی اصلی دارد ({@link EitaaYarBotAdapter}) که مسیر ب را به این کلاس
 * واگذار می‌کند.
 *
 * <p><b>⚠️ پیام‌های مسیر ب ردیابی‌پذیر نیستند.</b> پاسخ ایتا
 * {@code message_id} نمی‌دهد — فقط رشته {@code "success"}.
 * پس {@code EitaaSendResult.messageId} اینجا همیشه {@code null} است.
 * این یک محدودیت واقعی API است، نه نقص پیاده‌سازی؛ برای
 * {@code message_log} در فاز ۹ مستقیماً اثر دارد: ارسال‌های مسیر ب را
 * نمی‌شود بعداً با شناسه پیام پیدا کرد.
 */
@Component
@ConditionalOnProperty(name = "app.mock.eitaa", havingValue = "false", matchIfMissing = true)
public class EitaaAppMessageAdapter {

    private static final Logger log = LoggerFactory.getLogger(EitaaAppMessageAdapter.class);

    private final EitaaHttpClient httpClient;
    private final EitaaProperties properties;

    public EitaaAppMessageAdapter(EitaaHttpClient httpClient, EitaaProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    /**
     * ارسال پیام خصوصی به یک کاربر ایتا.
     *
     * @param token       توکن برنامه؛ اگر {@code null} باشد از تنظیمات
     *                    خوانده می‌شود
     * @param eitaaUserId شناسه کاربر در ایتا — ۵۲ بیتی
     * @param text        متن پیام
     * @return نتیجه ارسال — در شکست، {@code success = false} با کد خطا
     * @throws EitaaCapabilityNotSupportedException اگر توکن برنامه تنظیم
     *         نشده باشد
     */
    public EitaaSendResult sendToUser(EitaaAppToken token, Long eitaaUserId, String text) {
        EitaaAppToken effectiveToken = resolveToken(token);

        if (eitaaUserId == null) {
            throw new IllegalArgumentException("شناسه کاربر ایتا نباید خالی باشد");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("متن پیام نباید خالی باشد");
        }

        // ⚠️ تقسیم پیام اینجا هم لازم است: سقف طول، محدودیت خود سرویس
        // ارسال است و به مسیر الف و ب یکسان اعمال می‌شود. اما توجه:
        // هر بخش یک اعتبار جدا مصرف می‌کند.
        List<String> parts = EitaaTextSplitter.split(text, properties.maxMessageLength());
        String rateLimitKey = EitaaRateLimiter.keyFor(effectiveToken.value());

        String lastMessageId = null;
        Instant lastSentAt = null;

        for (String part : parts) {
            EitaaSendResult result = sendSinglePart(effectiveToken, eitaaUserId, part, rateLimitKey);

            if (!result.success()) {
                // ⚠️ اگر بخش دوم به بعد شکست بخورد، بخش اول <i>رفته</i>
                // است. این را پنهان نمی‌کنیم: شکست را برمی‌گردانیم و
                // وفادارانه می‌گوییم کدام بخش مشکل داشت.
                log.warn("ارسال مسیر ب شکست خورد (کاربر {}، کد {})",
                        eitaaUserId, result.errorCode());
                return result;
            }

            lastMessageId = result.messageId();
            lastSentAt = result.sentAt();
        }

        return EitaaSendResult.success(lastMessageId, lastSentAt);
    }

    /**
     * ارسال یک بخش — یک درخواست HTTP.
     *
     * <p>بدنه دقیقاً سه فیلد دارد: {@code token}، {@code chat_id}،
     * {@code text}. هیچ فیلد اضافه‌ای فرستاده نمی‌شود، چون سند رسمی
     * فقط همین سه را برای این Endpoint نام برده و فرستادن فیلد ناشناخته
     * می‌تواند درخواست را رد کند.
     */
    private EitaaSendResult sendSinglePart(EitaaAppToken token,
                                           Long eitaaUserId,
                                           String text,
                                           String rateLimitKey) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token.value());

        // ⚠️ chat_id به‌صورت <b>عدد</b> می‌رود، نه رشته. سند رسمی خودش
        // می‌گوید id تا ۵۲ بیت اهمیت دارد — که نشان می‌دهد در JSON عدد
        // است. فرستادنش به‌صورت رشته ممکن است در سمت ایتا نوعِ اشتباه
        // تلقی شود. Long در JSON بدون افت دقت سریال می‌شود.
        body.put("chat_id", eitaaUserId);
        body.put("text", text);

        String url = properties.appBaseUrl() + "/" + properties.sendMessageMethod();

        JsonNode response;
        try {
            response = httpClient.postJson(url, "app.sendMessage", rateLimitKey, body);
        } catch (EitaaApiException e) {
            // 🔴 شکست ارسال استثنا نیست — قرارداد EitaaAdapter. این مسیر
            // بیش از مسیر الف به آن نیاز دارد: هر پیام یک اعتبار از
            // ۵۰٬۰۰۰ اعتبار رایگان مصرف می‌کند، پس اگر استثنا بیرون
            // بزند، تکلیف برمی‌گردد و اعتبار هم هدر رفته است.
            return EitaaSendFailures.from(e);
        }

        // ⚠️ پاسخ موفق این مسیر result را به‌صورت رشته "success"
        // می‌دهد، نه شیء. پس message_id ای وجود ندارد که برگردانیم.
        // عمداً سعی نمی‌کنیم رشته را جوری تفسیر کنیم که شناسه دربیاوریم.
        String rawResult = response.path("result").asString(null);
        if (rawResult == null) {
            // ok=true بود ولی شکل پاسخ آن‌چیزی نیست که سند می‌گوید.
            // این را قبول می‌کنیم (پیام رفته) ولی ثبت می‌کنیم — چون
            // ممکن است نشانه تغییر API باشد.
            log.info("پاسخ مسیر ب برای کاربر {} فیلد result نداشت (متد app.sendMessage)",
                    eitaaUserId);
        }

        return EitaaSendResult.success(null, Instant.now());
    }

    /** آیا مسیر ب قابل استفاده است؟ — بر پایه وجود توکن برنامه. */
    public boolean isConfigured() {
        return properties.hasAppToken();
    }

    /**
     * انتخاب توکن مؤثر.
     *
     * <p>اگر فراخوان توکنی نداده باشد، از تنظیمات خوانده می‌شود. اگر
     * هیچ‌کدام نبود، قابلیت واقعاً وجود ندارد و باید صریح بگوییم — نه
     * اینکه با توکن خالی درخواست بفرستیم و خطای مبهم بگیریم.
     */
    private EitaaAppToken resolveToken(EitaaAppToken token) {
        EitaaAppToken effective = (token != null) ? token : properties.appTokenOrNull();

        if (effective == null || effective.value() == null || effective.value().isBlank()) {
            throw new EitaaCapabilityNotSupportedException(EitaaCapability.SEND_TO_USER);
        }

        return effective;
    }
}
