package ir.omid.smartteacher.eitaa.internal;

import ir.omid.smartteacher.common.error.EitaaApiException;
import ir.omid.smartteacher.eitaa.EitaaAdapter;
import ir.omid.smartteacher.eitaa.EitaaAppToken;
import ir.omid.smartteacher.eitaa.EitaaBotInfo;
import ir.omid.smartteacher.eitaa.EitaaBotToken;
import ir.omid.smartteacher.eitaa.EitaaCapability;
import ir.omid.smartteacher.eitaa.EitaaCapabilityNotSupportedException;
import ir.omid.smartteacher.eitaa.EitaaFilePayload;
import ir.omid.smartteacher.eitaa.EitaaHealth;
import ir.omid.smartteacher.eitaa.EitaaNotification;
import ir.omid.smartteacher.eitaa.EitaaProperties;
import ir.omid.smartteacher.eitaa.EitaaSendOptions;
import ir.omid.smartteacher.eitaa.EitaaSendResult;
import ir.omid.smartteacher.eitaa.internal.application.MessageCreditService;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * پیاده‌سازی واقعی {@link EitaaAdapter} روی Bot API ایتایار — مسیر الف.
 *
 * <p>این کلاس تنها نقطه تماس با ایتا در حالت واقعی است. نامش از
 * {@code eitaayar.ir} می‌آید، یعنی سرویسی که Bot API را ارائه می‌دهد.
 *
 * <p><b>🔴 فقط سه متد وجود دارد.</b> {@code getMe}، {@code sendMessage}،
 * {@code sendFile}. هر روش دیگری در این کلاس، ساختن قابلیتی است که
 * وجود ندارد. اگر روزی کسی «دریافت پیام» خواست، پاسخ درست این است که
 * ایتا آن را ندارد — نه اینکه یک کلاس خالی اینجا اضافه شود.
 *
 * <p><b>⚠️ مسیر ب (کاربر خصوصی) اینجا پیاده نشده.</b> به
 * {@link EitaaAppMessageAdapter} واگذار می‌شود. دلیلش دو چیز است: یک،
 * آن مسیر یک Endpoint و بدنه و پاسخ کاملاً متفاوت دارد؛ دو، اگر دو کلاس
 * همزمان {@code EitaaAdapter} را پیاده می‌کردند، دو Bean از یک نوع
 * داشتیم و تزریق مبهم می‌شد.
 *
 * <p><b>⚠️ {@code capabilities()} بر پایه پیکربندی است، نه ثابت.</b>
 * اگر توکن برنامه تنظیم نشده باشد، {@code SEND_TO_USER} در مجموعه
 * <i>نیست</i>. این دقیقاً کاری است که «جایگزین Fake کردن» را معنا
 * می‌دهد: مصرف‌کننده می‌تواند بپرسد «آیا این کار شدنی است؟» و پاسخ
 * درست بگیرد، به‌جای اینکه فراخوانی کند و در زمان اجرا بشکند.
 */
@Component
@ConditionalOnProperty(name = "app.mock.eitaa", havingValue = "false", matchIfMissing = true)
public class EitaaYarBotAdapter implements EitaaAdapter {

    private static final Logger log = LoggerFactory.getLogger(EitaaYarBotAdapter.class);

    /** مقدار درخواستی ایتا برای پرچم‌های بولی — سند رسمی «۱» می‌گوید، نه true. */
    private static final int FLAG_ON = 1;

    private static final String PARAM_CHAT_ID = "chat_id";
    private static final String PARAM_TEXT = "text";
    private static final String PARAM_CAPTION = "caption";
    private static final String PARAM_DISABLE_NOTIFICATION = "disable_notification";
    private static final String PARAM_REPLY_TO = "reply_to_message_id";
    private static final String PARAM_DATE = "date";
    private static final String PARAM_PIN = "pin";
    private static final String PARAM_VIEW_COUNT_FOR_DELETE = "viewCountForDelete";

    /** نام فیلد فایل در multipart — سند رسمی. */
    private static final String FILE_FIELD = "file";

    private final EitaaProperties properties;
    private final EitaaHttpClient httpClient;
    private final EitaaAppMessageAdapter appMessageAdapter;
    private final MessageCreditService messageCredits;
    private final Set<EitaaCapability> capabilities;

    public EitaaYarBotAdapter(EitaaProperties properties,
                              EitaaHttpClient httpClient,
                              EitaaAppMessageAdapter appMessageAdapter,
                              MessageCreditService messageCredits) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.appMessageAdapter = appMessageAdapter;
        this.messageCredits = messageCredits;
        this.capabilities = computeCapabilities(properties);

        if (!properties.hasBotToken()) {
            // ⚠️ هشدار، نه خطا: ممکن است این نمونه فعلاً فقط برای مسیر ب
            // راه افتاده باشد. ولی نباید سکوت کند — چون اولین
            // sendToChat با پیام گیج‌کننده شکست می‌خورد، در حالی که
            // علت واقعی از همان ابتدا معلوم بود.
            //
            // 🔴 نام canonical ذکر می‌شود، نه نام کوتاه: `APP_EITAA_BOT_TOKEN`
            // با relaxed binding در **هر** پروفایلی کار می‌کند، بدون نیاز
            // به هیچ کلید yml. نام کوتاه فقط جایی می‌گیرد که صریحاً نگاشت
            // شده باشد — و پیش‌تر در Production نگاشت نشده بود، پس همین
            // پیام به متغیری ارجاع می‌داد که آنجا بی‌اثر بود.
            log.warn("توکن ربات ایتا تنظیم نشده است ({}) — مسیر الف (گروه/کانال) کار نخواهد کرد",
                    "APP_EITAA_BOT_TOKEN");
        }
    }

    /**
     * محاسبه قابلیت‌ها از روی پیکربندی.
     *
     * <p>مسیر الف به توکن ربات نیاز دارد؛ مسیر ب به توکن برنامه. چیزی که
     * توکنش نیست، واقعاً قابلیت نیست.
     */
    private static Set<EitaaCapability> computeCapabilities(EitaaProperties properties) {
        Set<EitaaCapability> available = EnumSet.noneOf(EitaaCapability.class);

        if (properties.hasBotToken()) {
            available.add(EitaaCapability.GET_BOT_INFO);
            available.add(EitaaCapability.SEND_MESSAGE);
            available.add(EitaaCapability.SEND_FILE);
            available.add(EitaaCapability.SEND_TO_GROUP);
            available.add(EitaaCapability.SEND_TO_CHANNEL);
            available.add(EitaaCapability.SCHEDULED_SEND);
            available.add(EitaaCapability.PIN_SENT_MESSAGE);
            available.add(EitaaCapability.AUTO_DELETE_ON_VIEW);
        }

        if (properties.hasAppToken()) {
            available.add(EitaaCapability.SEND_TO_USER);
        }

        return Set.copyOf(available);
    }

    @Override
    public Set<EitaaCapability> capabilities() {
        return capabilities;
    }

    // ─────────────────────────── مسیر الف ───────────────────────────

    @Override
    public EitaaBotInfo getBotInfo(EitaaBotToken token) {
        EitaaBotToken effective = resolveBotToken(token, EitaaCapability.GET_BOT_INFO);
        String url = botUrl(effective, properties.getMeMethod());

        JsonNode result = httpClient.get(url, properties.getMeMethod(),
                EitaaRateLimiter.keyFor(effective.value())).path("result");

        // ⚠️ id هم Long است، نه int — همان دلیلی که در EitaaUserData آمد:
        // سند می‌گوید شناسه‌ها می‌توانند بزرگ باشند و int بی‌صدا خراب
        // می‌کند. asLong(0) اینجا فقط برای نبود مقدار است، نه نادیده‌گرفتن.
        return new EitaaBotInfo(
                result.path("id").asLong(0L),
                result.path("username").asString(null),
                result.path("first_name").asString(null),
                result.path("last_name").asString(null));
    }

    @Override
    public EitaaSendResult sendToChat(EitaaBotToken token,
                                      String chatId,
                                      String text,
                                      EitaaSendOptions options) {
        EitaaBotToken effective = resolveBotToken(token, EitaaCapability.SEND_MESSAGE);
        requireChatId(chatId);
        requireText(text);

        EitaaSendOptions effectiveOptions =
                (options != null) ? options : EitaaSendOptions.defaults();

        // ⚠️ تقسیم پیام: ایتا سقف طول دارد و پیام بلندتر را رد می‌کند.
        // سقف دقیق را نمی‌دانیم، پس محافظه‌کارانه تقسیم می‌کنیم.
        // ⚠️ گزینه‌ها فقط روی بخش اول اعمال می‌شوند: زمان‌بندی و سنجاق
        // و حذف‌خودکار برای «پیام» معنا دارند، نه برای هر تکه از یک
        // متن تقسیم‌شده. اعمالشان روی همه بخش‌ها یعنی چند پیام سنجاق‌شده
        // که هیچ‌کدام منظور نبوده.
        List<String> parts = EitaaTextSplitter.split(text, properties.maxMessageLength());
        String rateLimitKey = EitaaRateLimiter.keyFor(effective.value());
        String url = botUrl(effective, properties.sendMessageMethod());

        String firstMessageId = null;
        Instant firstSentAt = null;

        for (int i = 0; i < parts.size(); i++) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put(PARAM_CHAT_ID, chatId);
            body.put(PARAM_TEXT, parts.get(i));

            if (i == 0) {
                applyOptions(body, effectiveOptions);
            }

            JsonNode result;
            try {
                result = httpClient.postJson(
                        url, properties.sendMessageMethod(), rateLimitKey, body).path("result");
            } catch (EitaaApiException e) {
                // 🔴 شکست ارسال، استثنا نیست — قرارداد EitaaAdapter. اگر
                // این استثنا بیرون می‌زد، تراکنش انتشار تکلیف برمی‌گشت و
                // کار معلم از بین می‌رفت، در حالی که تکلیف ذخیره شده بود
                // و فقط اعلانش نرسیده.
                // ⚠️ اگر بخش اول رفته باشد، آن بخش <i>رفته</i> است. این
                // را پنهان نمی‌کنیم — شکست برگردانده می‌شود.
                log.warn("ارسال به {} شکست خورد (بخش {} از {})",
                        chatId, i + 1, parts.size());
                return EitaaSendFailures.from(e);
            }

            String messageId = result.path("message_id").asString(null);
            Instant sentAt = Instant.now();

            if (i == 0) {
                firstMessageId = messageId;
                firstSentAt = sentAt;
            } else if (messageId != null) {
                // فقط برای ردیابی — پیام دوم به بعد شناسه جدا دارند و
                // شناسه بخش اول که برمی‌گردانیم نماینده کل ارسال است.
                log.debug("بخش {} از {} ارسال شد (شناسه پیام {})",
                        i + 1, parts.size(), messageId);
            }
        }

        return EitaaSendResult.success(firstMessageId, firstSentAt);
    }

    @Override
    public EitaaSendResult sendFileToChat(EitaaBotToken token,
                                          String chatId,
                                          EitaaFilePayload file,
                                          String caption,
                                          EitaaSendOptions options) {
        EitaaBotToken effective = resolveBotToken(token, EitaaCapability.SEND_FILE);
        requireChatId(chatId);

        if (file == null) {
            throw new IllegalArgumentException("فایل نباید خالی باشد");
        }
        // حد حجم در خود EitaaFilePayload بررسی می‌شود، پس اینجا تکرار نمی‌کنیم.

        EitaaSendOptions effectiveOptions =
                (options != null) ? options : EitaaSendOptions.defaults();

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(PARAM_CHAT_ID, chatId);

        if (caption != null && !caption.isBlank()) {
            // ⚠️ در sendFile فیلد متن، caption است نه text — سند رسمی.
            fields.put(PARAM_CAPTION, caption);
        }
        applyOptionsAsText(fields, effectiveOptions);

        String url = botUrl(effective, properties.sendFileMethod());
        String rateLimitKey = EitaaRateLimiter.keyFor(effective.value());

        JsonNode result;
        try {
            result = httpClient.postMultipart(
                    url,
                    properties.sendFileMethod(),
                    rateLimitKey,
                    fields,
                    FILE_FIELD,
                    file.fileName(),
                    file.contentType(),
                    file.bytes()).path("result");
        } catch (EitaaApiException e) {
            // همان قاعده مسیر متن: شکست فایل هم استثنا نیست، چون فراخوان
            // (انتشار تکلیف) نباید به‌خاطر آن تراکنشش را از دست بدهد.
            // ⚠️ نام فایل لاگ می‌شود ولی محتوایش نه.
            log.warn("ارسال فایل {} به {} شکست خورد", file.fileName(), chatId);
            return EitaaSendFailures.from(e);
        }

        return EitaaSendResult.success(
                result.path("message_id").asString(null), Instant.now());
    }

    // ─────────────────────────── مسیر ب ───────────────────────────

    @Override
    public EitaaSendResult sendToUser(EitaaAppToken token, Long eitaaUserId, String text) {
        EitaaSendResult result = appMessageAdapter.sendToUser(token, eitaaUserId, text);
        if (result.success()) {
            decrementCreditQuietly();
        }
        return result;
    }

    @Override
    public EitaaSendResult notifyUser(Long eitaaUserId, EitaaNotification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("اعلان نباید خالی باشد");
        }
        if (!appMessageAdapter.isConfigured()) {
            // ⚠️ «انتخاب خودکار مسیر» در سند، یعنی همین: اعلان شخصی فقط
            // از مسیر ب می‌رود. مسیر الف اصلاً نمی‌تواند به کاربر خصوصی
            // پیام بدهد — مقصدش گروه و کانال است. پس اگر توکن برنامه
            // نباشد، جایگزینی وجود ندارد که به آن سوئیچ کنیم؛ باید
            // صریح بگوییم قابلیت نیست.
            throw new EitaaCapabilityNotSupportedException(EitaaCapability.SEND_TO_USER);
        }

        // ⚠️ گزینه‌های اعلان اینجا مصرف نمی‌شوند: مسیر ب فقط
        // {token, chat_id, text} می‌پذیرد و پارامتری مثل date یا pin
        // برایش مستند نشده. عمداً فیلد ناشناخته نمی‌فرستیم.
        EitaaSendResult result = appMessageAdapter.sendToUser(null, eitaaUserId, notification.text());
        if (result.success()) {
            decrementCreditQuietly();
        }
        return result;
    }

    private void decrementCreditQuietly() {
        try {
            messageCredits.decrementOnSuccessfulUserSend();
        } catch (Exception ex) {
            log.debug("کسر اعتبار پیام نادیده گرفته شد: {}", ex.toString());
        }
    }

    // ─────────────────────────── سلامت ───────────────────────────

    @Override
    public EitaaHealth checkHealth(EitaaBotToken token) {
        Instant now = Instant.now();
        try {
            EitaaBotInfo info = getBotInfo(token);
            return EitaaHealth.up(info.displayName(), now);
        } catch (EitaaApiException e) {
            // ⚠️ استثنا را رد نمی‌کنیم: این متد برای <i>گزارش</i> سلامت
            // است، نه برای شکست دادن فراخوان. اگر پرتاب می‌کرد، هر
            // Endpoint سلامت با یک خطای ۵۰۲ برمی‌گشت و دیگر نمی‌شد فهمید
            // «ایتا خراب است» یا «خودمان خرابیم».
            return EitaaHealth.down(e.getMessage(), now);
        } catch (RuntimeException e) {
            return EitaaHealth.down("خطای غیرمنتظره در بررسی سلامت ایتا", now);
        }
    }

    // ─────────────────────────── کمکی‌ها ───────────────────────────

    /**
     * ساخت نشانی کامل مسیر الف.
     *
     * <p>قالب: {@code {baseUrl}/{token}/{method}}.
     *
     * <p><b>⚠️ خروجی این متد حاوی راز است.</b> هرگز لاگ نشود. لاگ‌کردن
     * از روی {@code methodLabel} انجام می‌شود، نه از روی این نشانی.
     */
    private String botUrl(EitaaBotToken token, String method) {
        return properties.baseUrl() + "/" + token.value() + "/" + method;
    }

    /**
     * اعمال گزینه‌ها روی بدنه JSON.
     *
     * <p>فقط گزینه‌هایی که واقعاً تنظیم شده‌اند فرستاده می‌شوند. فرستادن
     * {@code disable_notification: 0} بی‌ضرر به نظر می‌رسد ولی یعنی
     * «صریحاً روشن کن»، و اگر ایتا مقدار پیش‌فرض را عوض کند ما از آن
     * مطلع نمی‌شویم. نبودن فیلد، امن‌تر از صفر بودنش است.
     */
    private static void applyOptions(Map<String, Object> body, EitaaSendOptions options) {
        if (options.disableNotification()) {
            body.put(PARAM_DISABLE_NOTIFICATION, FLAG_ON);
        }
        if (options.replyToMessageId() != null && !options.replyToMessageId().isBlank()) {
            body.put(PARAM_REPLY_TO, asIdOrText(options.replyToMessageId()));
        }
        if (options.hasScheduling()) {
            // ✅ پارامتر date = Unix Timestamp → زمان‌بندی بومی ایتا.
            // ⚠️ این جایگزین Scheduler خودمان نیست؛ لایه دوم اطمینان است.
            body.put(PARAM_DATE, options.scheduledAt().getEpochSecond());
        }
        if (options.pinAfterSend()) {
            body.put(PARAM_PIN, FLAG_ON);
        }
        if (options.hasAutoDelete()) {
            body.put(PARAM_VIEW_COUNT_FOR_DELETE, options.viewCountForDelete());
        }
    }

    /**
     * همان {@link #applyOptions} ولی برای {@code multipart/form-data}، که
     * همه مقدارهایش رشته‌اند.
     */
    private static void applyOptionsAsText(Map<String, String> fields, EitaaSendOptions options) {
        Map<String, Object> asObjects = new LinkedHashMap<>();
        applyOptions(asObjects, options);
        asObjects.forEach((key, value) -> fields.put(key, String.valueOf(value)));
    }

    /**
     * شناسه پیام را عدد می‌فرستیم اگر عدد باشد.
     *
     * <p>{@code message_id} در پاسخ‌های سند رسمی همیشه عدد است
     * ({@code 85917}). پس فرستادنش به‌صورت عدد با آنچه ایتا خودش
     * تولید می‌کند هم‌خوان است. ولی اگر مقدار عددی نبود، آن را
     * <i>حذف نمی‌کنیم</i> — یک شناسه با قالب نامنتظر را بی‌صدا دور
     * انداختن، بدتر از فرستادنش به‌صورت رشته است.
     */
    private static Object asIdOrText(String rawId) {
        try {
            return Long.parseLong(rawId.trim());
        } catch (NumberFormatException e) {
            return rawId;
        }
    }

    private EitaaBotToken resolveBotToken(EitaaBotToken token, EitaaCapability capability) {
        EitaaBotToken effective = (token != null) ? token : properties.botTokenOrNull();

        if (effective == null || effective.value() == null || effective.value().isBlank()) {
            throw new EitaaCapabilityNotSupportedException(capability);
        }

        return effective;
    }

    private static void requireChatId(String chatId) {
        if (chatId == null || chatId.isBlank()) {
            throw new IllegalArgumentException("شناسه گفت‌وگو نباید خالی باشد");
        }
    }

    private static void requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("متن پیام نباید خالی باشد");
        }
    }
}
