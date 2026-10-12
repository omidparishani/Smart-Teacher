package ir.omid.smartteacher.eitaa.internal;

import ir.omid.smartteacher.common.error.EitaaApiException;
import ir.omid.smartteacher.eitaa.EitaaProperties;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * لایه HTTP مشترک گفت‌وگو با ایتایار.
 *
 * <p>چهار مسئولیت، و هر چهار در <b>یک</b> جا متمرکز شده‌اند تا هیچ‌کدام
 * فراموش نشود:
 * <ol>
 *   <li><b>مهلت زمانی</b> — اتصال و خواندن، هر دو محدود.</li>
 *   <li><b>محدودیت نرخ</b> — پیش از هر درخواست، نوبت گرفته می‌شود.</li>
 *   <li><b>تلاش دوباره</b> — فقط برای خطای شبکه و ۵xx.</li>
 *   <li><b>🔴 بررسی {@code ok}</b> — قاعده طلایی سند رسمی.</li>
 * </ol>
 *
 * <p><b>🔴 چرا بررسی {@code ok} اینجاست و نه در فراخوان‌ها؟</b> چون سند
 * رسمی می‌گوید «همیشه ابتدا {@code ok} را بررسی کنید» و اگر این بررسی
 * در هر فراخوان تکرار شود، یک روز یکی فراموشش می‌کند و آن‌وقت یک خطای
 * ایتا <i>موفق</i> تلقی می‌شود: معلم فکر می‌کند تکلیف به گروه رسیده،
 * در حالی که نرسیده. اینجا یک نقطه شکست دارد، نه n نقطه.
 *
 * <p><b>⚠️ هیچ‌جا URL لاگ نمی‌شود.</b> توکن ربات بخشی از مسیر URL است:
 * {@code https://eitaayar.ir/api/{TOKEN}/{METHOD}}. لاگ‌کردن URL یعنی
 * نوشتن توکن در فایل لاگ — همان چیزی که بخش ۵۳ خواسته‌ها صریحاً ممنوع
 * کرده. به‌جایش برچسب متد لاگ می‌شود.
 *
 * <p><b>⚠️ چرا ۴xx دوباره تلاش نمی‌شود؟</b> چون خطای ۴xx یعنی درخواست
 * ما مشکل داشت و تکرارش همان نتیجه را می‌دهد — فقط وقت و اعتبار مصرف
 * می‌کند. در مسیر ب، هر پیام یک اعتبار از ۵۰٬۰۰۰ اعتبار رایگان کم
 * می‌کند؛ پس تلاش بیهوده، هزینه واقعی دارد.
 */
@Component
@ConditionalOnProperty(name = "app.mock.eitaa", havingValue = "false", matchIfMissing = true)
public class EitaaHttpClient {

    private static final Logger log = LoggerFactory.getLogger(EitaaHttpClient.class);

    /** بررسی {@code ok} نباید دیرتر از این طول بکشد — پس بدنه محدود می‌شود. */
    private static final Duration RATE_LIMIT_MAX_WAIT = Duration.ofSeconds(10);

    private static final long RETRY_BASE_DELAY_MILLIS = 250L;

    /** سقف تأخیر پشت‌سرهم — تا انتظار بی‌اندازه نشود. */
    private static final long RETRY_MAX_DELAY_MILLIS = 4_000L;

    private final EitaaProperties properties;
    private final EitaaRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ExecutorService executor;

    public EitaaHttpClient(EitaaProperties properties,
                           EitaaRateLimiter rateLimiter,
                           ObjectMapper objectMapper) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;

        // Virtual Thread برای فراخوانی‌های خروجی: تعداد درخواست‌های
        // هم‌زمان به ایتا می‌تواند زیاد شود و نخ سیستم‌عامل نباید گلوگاه
        // باشد. پروژه در کل روی Virtual Thread اجرا می‌شود؛ این هم
        // همان الگو را ادامه می‌دهد.
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .executor(executor)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }

    // ─────────────────────────── ورودی‌های عمومی ───────────────────────────

    /**
     * ارسال JSON به ایتا.
     *
     * @param url          نشانی کامل — <b>حاوی توکن است، لاگ نشود</b>
     * @param methodLabel  برچسب متد برای لاگ، مثل {@code sendMessage}
     * @param rateLimitKey کلید محدودیت نرخ — هش توکن
     * @param body         بدنه درخواست
     * @return پاسخ تأییدشده ایتا (بخشی که {@code ok: true} داشته)
     */
    public JsonNode postJson(String url, String methodLabel, String rateLimitKey,
                             Map<String, Object> body) {
        String payload = objectMapper.writeValueAsString(body);
        return send(methodLabel, rateLimitKey, () -> HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(properties.readTimeout())
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build());
    }

    /**
     * فراخوانی بدون بدنه — برای متدی مثل {@code getMe}.
     *
     * @param url          نشانی کامل — حاوی توکن
     * @param methodLabel  برچسب متد برای لاگ
     * @param rateLimitKey کلید محدودیت نرخ
     * @return پاسخ تأییدشده ایتا
     */
    public JsonNode get(String url, String methodLabel, String rateLimitKey) {
        return send(methodLabel, rateLimitKey, () -> HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(properties.readTimeout())
                .header("Accept", "application/json")
                .GET()
                .build());
    }

    /**
     * ارسال فایل با {@code multipart/form-data}.
     *
     * <p>سند رسمی می‌گوید فایل باید در {@code multipart/form-data} برود
     * و پارامترش {@code file} است.
     *
     * @param url           نشانی کامل — حاوی توکن
     * @param methodLabel   برچسب متد برای لاگ
     * @param rateLimitKey  کلید محدودیت نرخ
     * @param fields        فیلدهای متنی همراه
     * @param fileFieldName نام فیلد فایل — معمولاً {@code file}
     * @param fileName      نام فایل همراه پسوند
     * @param contentType   نوع MIME فایل
     * @param content       محتوای فایل
     * @return پاسخ تأییدشده ایتا
     */
    public JsonNode postMultipart(String url,
                                  String methodLabel,
                                  String rateLimitKey,
                                  Map<String, String> fields,
                                  String fileFieldName,
                                  String fileName,
                                  String contentType,
                                  byte[] content) {
        String boundary = "----SmartTeacherBoundary" + java.util.UUID.randomUUID();
        byte[] payload = buildMultipart(boundary, fields, fileFieldName,
                fileName, contentType, content);

        return send(methodLabel, rateLimitKey, () -> HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(properties.readTimeout())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build());
    }

    // ─────────────────────────── هسته ───────────────────────────

    /**
     * اجرای درخواست با محدودیت نرخ، تلاش دوباره، و بررسی {@code ok}.
     *
     * <p>سازنده درخواست به‌صورت {@code Supplier} می‌آید چون هر تلاش
     * دوباره باید یک {@link HttpRequest} <i>تازه</i> بسازد: یک
     * {@code HttpRequest} مصرف‌شده را نمی‌شود دوباره فرستاد.
     */
    private JsonNode send(String methodLabel,
                          String rateLimitKey,
                          java.util.function.Supplier<HttpRequest> requestFactory) {

        if (!rateLimiter.acquire(rateLimitKey,
                properties.sendRatePerSecond(), RATE_LIMIT_MAX_WAIT)) {
            // نوبت نگرفتیم. این «خطای ایتا» نیست، محدودیت خودمان است —
            // ولی نتیجه‌اش برای کاربر یکی است: الآن فرستاده نشد.
            throw EitaaApiException.fromResponse("RATE_LIMITED",
                    "محدودیت نرخ داخلی: نوبت ارسال در زمان مجاز گرفته نشد");
        }

        int attempts = Math.max(1, properties.maxSendAttempts());
        EitaaApiException lastFailure = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(
                        requestFactory.get(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                int status = response.statusCode();

                if (status >= 500 && attempt < attempts) {
                    log.warn("ایتا خطای {} داد (متد {}، تلاش {}/{}) — تلاش دوباره",
                            status, methodLabel, attempt, attempts);
                    sleepBackoff(attempt);
                    continue;
                }

                if (status < 200 || status >= 300) {
                    // ⚠️ بدنه پاسخ لاگ نمی‌شود — ممکن است داده داخلی داشته باشد.
                    // فقط طولش می‌آید که برای تشخیص کافی است.
                    log.warn("ایتا کد وضعیت {} برگرداند (متد {}، طول بدنه {} بایت)",
                            status, methodLabel, response.body().length());
                    throw EitaaApiException.fromResponse("HTTP_" + status,
                            "کد وضعیت " + status);
                }

                return parseAndCheckOk(response.body(), methodLabel);

            } catch (IOException e) {
                lastFailure = EitaaApiException.unreachable(e);
                if (attempt < attempts) {
                    log.warn("خطای شبکه در متد {} (تلاش {}/{}) — تلاش دوباره",
                            methodLabel, attempt, attempts);
                    sleepBackoff(attempt);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw EitaaApiException.unreachable(e);
            }
        }

        throw (lastFailure == null)
                ? EitaaApiException.fromResponse("UNKNOWN", "ارسال پس از چند تلاش ناموفق بود")
                : lastFailure;
    }

    /**
     * 🔴 قاعده طلایی سند رسمی: «اولین پارامتر هر پاسخ JSON، {@code ok}
     * است. همیشه ابتدا {@code ok} را بررسی کنید.»
     *
     * <p>اگر پاسخ JSON نباشد یا {@code ok} نداشته باشد یا
     * {@code false} باشد، استثنا پرتاب می‌شود. هیچ مسیری از این متد
     * نمی‌تواند با پاسخ نامعتبر «موفق» برگردد.
     */
    private JsonNode parseAndCheckOk(String responseBody, String methodLabel) {
        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (RuntimeException e) {
            // پاسخ HTML صفحه خطا، یا بدنه خالی، یا JSON ناقص. هیچ‌کدام
            // «موفق» نیست. ⚠️ بدنه در پیام نمی‌آید.
            throw EitaaApiException.fromResponse("MALFORMED_RESPONSE",
                    "پاسخ ایتا JSON معتبر نبود (متد " + methodLabel + ")");
        }

        if (root == null) {
            throw EitaaApiException.fromResponse("MALFORMED_RESPONSE",
                    "پاسخ ایتا JSON معتبر نبود (متد " + methodLabel + ")");
        }

        // ⚠️ قاعده طلایی: نبود ok یا ok=false یعنی شکست، حتی اگر کد
        // وضعیت HTTP ۲۰۰ بود. این همان حالتی است که اگر فراموش شود،
        // یک شکست بی‌صدا «موفق» به نظر می‌رسد.
        //
        // ⚠️ اگر بدنه خالی باشد، readTree گره MissingNode می‌دهد؛
        // path روی آن هم MissingNode می‌دهد و asBoolean(false) مقدار
        // false — یعنی همان شکست. پس بدنه خالی هم درست گرفته می‌شود.
        if (!root.path("ok").asBoolean(false)) {
            // ⚠️ متن خطای ایتا شکل تأییدنشده‌ای دارد، پس فقط اگر فیلد
            // شناخته‌شده‌ای موجود بود خوانده می‌شود؛ هیچ ساختاری حدس
            // زده نمی‌شود. اگر نبود، null می‌ماند.
            String remoteCode = root.path("error_code").asString(null);
            String remoteDescription = root.path("description").asString(null);

            throw EitaaApiException.fromResponse(remoteCode, remoteDescription);
        }

        return root;
    }

    private static void sleepBackoff(int attempt) {
        long exponential = RETRY_BASE_DELAY_MILLIS << (attempt - 1);
        long capped = Math.min(exponential, RETRY_MAX_DELAY_MILLIS);

        // ⚠️ تلنگر تصادفی عمدی است: بدون آن، چند درخواست که هم‌زمان
        // شکست خورده‌اند، هم‌زمان هم دوباره تلاش می‌کنند و ایتا را
        // دوباره زیر بار می‌برند.
        long jitter = ThreadLocalRandom.current().nextLong(capped / 2 + 1);
        long delay = capped / 2 + jitter;

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * ساخت بدنه {@code multipart/form-data}.
     *
     * <p>دستی ساخته می‌شود چون بدنه باید بایت‌به‌بایت تحت کنترل باشد:
     * فایل ممکن است باینری باشد و نباید دوباره کدگذاری شود. فیلدهای
     * متنی با UTF-8 نوشته می‌شوند تا متن فارسی سالم برود.
     */
    private static byte[] buildMultipart(String boundary,
                                         Map<String, String> fields,
                                         String fileFieldName,
                                         String fileName,
                                         String contentType,
                                         byte[] content) {
        var buffer = new java.io.ByteArrayOutputStream();

        try {
            for (Map.Entry<String, String> field : fields.entrySet()) {
                if (field.getValue() == null) {
                    continue;
                }
                buffer.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                buffer.write(("Content-Disposition: form-data; name=\""
                        + field.getKey() + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                buffer.write(field.getValue().getBytes(StandardCharsets.UTF_8));
                buffer.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }

            buffer.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            buffer.write(("Content-Disposition: form-data; name=\"" + fileFieldName
                    + "\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            buffer.write(("Content-Type: " + contentType + "\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            buffer.write(content);
            buffer.write("\r\n".getBytes(StandardCharsets.UTF_8));

            buffer.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // ByteArrayOutputStream هرگز IOException نمی‌دهد؛ اینجا فقط
            // برای رضایت امضای متد است.
            throw new IllegalStateException("ساخت بدنه multipart شکست خورد", e);
        }

        return buffer.toByteArray();
    }
}
