package ir.omid.smartteacher.eitaa;

import ir.omid.smartteacher.common.error.EitaaInitDataExpiredException;
import ir.omid.smartteacher.common.error.EitaaInitDataInvalidException;
import ir.omid.smartteacher.common.error.EitaaInitDataReplayedException;
import ir.omid.smartteacher.common.util.Hashing;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * اعتبارسنجی {@code initData} ایتا ({@code docs/eitaa.md} بخش ۷.۴).
 *
 * <p>سه بررسی پشت‌سرهم، و هر سه لازم‌اند:
 * <ol>
 *   <li><b>امضا</b> — HMAC-SHA256 با کلید مشتق‌شده از توکن ربات. ثابت
 *       می‌کند داده از ایتا آمده و دست نخورده.</li>
 *   <li><b>تازگی</b> — {@code auth_date} نباید از پنجره مجاز قدیمی‌تر
 *       باشد. ثابت می‌کند داده <i>الآن</i> صادر شده.</li>
 *   <li><b>ضدبازپخش</b> — همین {@code initData} دوباره پذیرفته نمی‌شود.
 *       پنجره تازگی را از بازپخش <i>در همان پنجره</i> جدا می‌کند.</li>
 * </ol>
 * بدون بررسی دوم، یک {@code initData} دزدیده‌شده تا ابد کار می‌کند.
 * بدون بررسی سوم، تا پنج دقیقه می‌شود بی‌نهایت نشست از آن ساخت.
 *
 * <p><b>⚠️ این کلاس رویداد امنیتی ثبت نمی‌کند — عمدی است.</b> دلیلش
 * جداسازی ماژول است: {@code eitaa} نباید به ماژول {@code security}
 * وابسته شود. فراخوان (سرویس ورود) نوع استثنا را می‌بیند و رویداد را
 * ثبت می‌کند — و آنجا زمینه بهتری هم دارد (کاربر، IP، traceId).
 * اگر روزی این تصمیم عوض شود، اینجا جای افزودنش است.
 */
@Component
public class EitaaInitDataValidator {

    private static final Logger log = LoggerFactory.getLogger(EitaaInitDataValidator.class);

    /**
     * اختلاف ساعت مجاز برای {@code auth_date} در آینده.
     *
     * <p><b>چرا این بررسی لازم است، در حالی که فقط «قدیمی بودن» کافی
     * به نظر می‌رسد؟</b> چون بدون آن، یک {@code auth_date} در آینده
     * <i>هرگز</i> منقضی نمی‌شود — ساعت کاربر می‌تواند جلو باشد یا
     * مهاجم با دست‌کاری ساعت دستگاهش چنین مقداری بسازد. و امضا هم
     * می‌خواند، چون ایتا داده را امضا کرده است. پس بدون این بررسی،
     * «تازگی» عملاً بی‌اثر می‌شود.
     *
     * <p>ساعت دستگاه‌های واقعی چند دقیقه عقب و جلو می‌رود، پس پنج دقیقه
     * فرصت داده شده — نه صفر که کاربران بی‌گناه را رد کند.
     */
    private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);

    private static final String PARAM_HASH = "hash";
    private static final String PARAM_AUTH_DATE = "auth_date";
    private static final String PARAM_USER = "user";
    private static final String PARAM_QUERY_ID = "query_id";
    private static final String PARAM_START_PARAM = "start_param";
    private static final String PARAM_CHAT_TYPE = "chat_type";
    private static final String PARAM_DEVICE_ID = "device_id";

    private final EitaaProperties properties;
    private final EitaaHashVerifier hashVerifier;
    private final ReplayGuard replayGuard;
    private final ObjectMapper objectMapper;

    public EitaaInitDataValidator(EitaaProperties properties,
                                  EitaaHashVerifier hashVerifier,
                                  ReplayGuard replayGuard,
                                  ObjectMapper objectMapper) {
        this.properties = properties;
        this.hashVerifier = hashVerifier;
        this.replayGuard = replayGuard;
        this.objectMapper = objectMapper;
    }

    /**
     * اعتبارسنجی کامل {@code initData}.
     *
     * @param initData رشته خام دریافتی از Mini App
     * @return داده تأییدشده
     * @throws EitaaInitDataInvalidException  اگر امضا نخواند یا بدنه ناقص بود
     * @throws EitaaInitDataExpiredException  اگر {@code auth_date} بیرون پنجره مجاز بود
     * @throws EitaaInitDataReplayedException اگر همین {@code initData} قبلاً مصرف شده بود
     */
    public EitaaInitData validate(String initData) {
        if (initData == null || initData.isBlank()) {
            throw EitaaInitDataInvalidException.because("initData خالی است");
        }

        Map<String, String> params = parseQuery(initData);
        String receivedHash = params.remove(PARAM_HASH);

        if (receivedHash == null || receivedHash.isBlank()) {
            throw EitaaInitDataInvalidException.because("فیلد hash وجود ندارد");
        }

        String dataCheckString = verifySignature(params, receivedHash);

        return checkFreshnessAndParse(dataCheckString, params);
    }

    // ─────────────────────────── امضا ───────────────────────────

    /**
     * بررسی امضا.
     *
     * @return رشته canonical که امضا روی آن محاسبه شد — فراخوان باید
     *         همین را اثر انگشت ضدبازپخش قرار دهد، نه رشته خام را
     */
    private String verifySignature(Map<String, String> params, String receivedHash) {
        String dataCheckString = buildDataCheckString(params);

        if (!hashVerifier.verify(dataCheckString, receivedHash)) {
            // ⚠️ دلیل دقیق هرگز به کاربر نمی‌رود — فقط گفته می‌شود
            // «نامعتبر». گفتن اینکه کدام فیلد خراب بود به مهاجم کمک می‌کند.
            throw EitaaInitDataInvalidException.because("امضای HMAC نخواند");
        }

        return dataCheckString;
    }

    /**
     * رشته‌ای که امضا روی آن محاسبه می‌شود.
     *
     * <p>⚠️ ترتیب حروف الفبا و جداکننده خط شکست — هر دو در سند رسمی
     * ایتا مشخص‌شده‌اند. تغییر هرکدام، امضا را همیشه نامعتبر می‌کند.
     */
    private static String buildDataCheckString(Map<String, String> params) {
        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    // ─────────────────────────── تازگی، ضدبازپخش، پارس ───────────────────────────

    private EitaaInitData checkFreshnessAndParse(String dataCheckString,
                                                 Map<String, String> params) {
        Instant issuedAt = parseAuthDate(params);
        Instant now = Instant.now();

        if (issuedAt.isBefore(now.minus(properties.initDataMaxAge()))) {
            throw new EitaaInitDataExpiredException(issuedAt);
        }

        if (issuedAt.isAfter(now.plus(MAX_CLOCK_SKEW))) {
            // ⚠️ به‌عنوان «نامعتبر» گزارش می‌شود نه «منقضی»: کاربری که
            // ساعت دستگاهش جلو است باید پیام «دوباره وارد شو» بگیرد، نه
            // توضیحی که به مهاجم بگوید چرا رد شد.
            throw EitaaInitDataInvalidException.because(
                    "auth_date در آینده است — احتمال دست‌کاری ساعت");
        }

        // ── ضدبازپخش ──
        // 🔴 اثر انگشت از رشته canonical ساخته می‌شود، نه از رشته خام
        // درخواست. این تفاوت یک دور زدن واقعی را می‌بندد: ترتیب
        // پارامترها در initData امضا را باطل نمی‌کند (چون امضا روی
        // نسخه مرتب‌شده محاسبه می‌شود)، پس مهاجمی که یک initData
        // دزدیده را با ترتیب دیگری بفرستد، رشته خام متفاوتی می‌سازد و
        // از نگه‌بان بازپخش می‌گذرد — در حالی که همان داده است.
        // با رشته canonical، هر جایگشت از یک داده، یک اثر انگشت می‌دهد.
        String fingerprint = Hashing.sha256Hex(dataCheckString);

        if (!replayGuard.markUsed(fingerprint, properties.initDataMaxAge())) {
            throw new EitaaInitDataReplayedException();
        }

        EitaaUserData user = parseUser(params.get(PARAM_USER));

        log.debug("initData ایتا معتبر شد: userId={} issuedAt={}", user.id(), issuedAt);

        return new EitaaInitData(
                user,
                issuedAt,
                blankToNull(params.get(PARAM_QUERY_ID)),
                parseStartParam(params.get(PARAM_START_PARAM)),
                blankToNull(params.get(PARAM_CHAT_TYPE)),
                blankToNull(params.get(PARAM_DEVICE_ID)),
                fingerprint);
    }

    private static Instant parseAuthDate(Map<String, String> params) {
        String raw = params.get(PARAM_AUTH_DATE);
        if (raw == null || raw.isBlank()) {
            throw EitaaInitDataInvalidException.because("فیلد auth_date وجود ندارد");
        }
        try {
            return Instant.ofEpochSecond(Long.parseLong(raw.trim()));
        } catch (NumberFormatException e) {
            throw EitaaInitDataInvalidException.because("auth_date عدد معتبر نیست");
        }
    }

    private EitaaUserData parseUser(String rawUser) {
        if (rawUser == null || rawUser.isBlank()) {
            throw EitaaInitDataInvalidException.because("فیلد user وجود ندارد");
        }

        // ⚠️ رمزگشایی URL فقط پس از تأیید امضا انجام می‌شود — و فقط روی
        // فیلدهایی که مقدارشان یک رشته معنادار است (اینجا user، و در
        // parseStartParam). پیش از محاسبه HMAC هرگز — چون مقادیر
        // initData از قبل URL-encoded هستند و رمزگشایی پیش از امضا،
        // data_check_string را با آنچه ایتا امضا کرده ناهمخوان می‌کند.
        EitaaUserData user;
        try {
            user = objectMapper.readValue(
                    URLDecoder.decode(rawUser, StandardCharsets.UTF_8),
                    EitaaUserData.class);
        } catch (RuntimeException e) {
            throw EitaaInitDataInvalidException.because("فیلد user قابل خواندن نبود");
        }

        if (user == null || user.id() == null) {
            throw EitaaInitDataInvalidException.because("شناسه کاربر در user وجود ندارد");
        }

        return user;
    }

    /**
     * رمزگشایی {@code start_param} — پارامتر Deep Link.
     *
     * <p>⚠️ چرا این هم مثل {@code user} رمزگشایی می‌شود؟ چون
     * {@code initData} ذاتاً یک پرس‌وجوی URL است ({@code URLSearchParams}
     * در سمت Mini App). هر مقداری که کاراکتر {@code %}&rlm;، {@code &}
     * یا {@code =} داشته باشد، percent-encoded سفر می‌کند. مقدار
     * رمزگشایی‌نشده برای لایه مسیریابی <i>غلط</i> است: {@code asg_1}
     * درست از آب درمی‌آید، ولی {@code cls_1&exm_2} به
     * {@code cls_1%26exm_2} تبدیل می‌شود و هرگز با هیچ شناسه‌ای جور
     * نمی‌شود. آن‌وقت De Link بی‌صدا کار نمی‌کند — بدون هیچ خطایی.
     *
     * <p>⚠️ پس از بررسی امضا انجام می‌شود، نه پیش از آن. رمزگشایی
     * پیش از امضا، {@code data_check_string} را با آنچه ایتا امضا کرده
     * ناهمخوان می‌کند ({@link #parseQuery}).
     *
     * <p>⚠️ و برخلاف {@code user}، اینجا خطای رمزگشایی کل ورود را رد
     * نمی‌کند. تفاوت عمدی است: {@code user} هویت را حمل می‌کند و اگر
     * خوانده نشود ورود بی‌معنا است؛ {@code start_param} فقط یک میان‌بر
     * راحتی است. خراب بودن لینک نباید جلوی ورود معلم را بگیرد — پس
     * مقدار خام نگه داشته می‌شود و مشکل ثبت می‌شود.
     */
    private static String parseStartParam(String rawStartParam) {
        if (rawStartParam == null || rawStartParam.isBlank()) {
            return null;
        }

        try {
            String decoded = URLDecoder.decode(rawStartParam, StandardCharsets.UTF_8);

            return decoded.isBlank() ? null : decoded;
        } catch (IllegalArgumentException malformed) {
            // ⚠️ مقدار خام ممکن است هیچ‌وقت مصرف نشود، ولی ثبت می‌کنیم
            // تا اگر Deep Link ها در Production کار نکردند، دلیلش
            // قابل‌پیدا باشد.
            log.warn("start_param قابل رمزگشایی نبود؛ مقدار خام نگه داشته شد");
            return rawStartParam;
        }
    }

    /**
     * پارس پرس‌وجو <b>بدون رمزگشایی URL</b>.
     *
     * <p>⚠️ این مهم‌ترین جزئیات این متد است. مقادیر {@code initData}
     * از قبل URL-encoded هستند و همان شکل، امضا شده. اگر پیش از محاسبه
     * HMAC رمزگشایی شوند، {@code data_check_string} با آنچه ایتا امضا
     * کرده یکی نمی‌شود و اعتبارسنجی <i>همیشه</i> شکست می‌خورد —
     * بدون هیچ سرنخی از علت.
     *
     * <p>اولین {@code =} جداکننده کلید و مقدار است، نه آخرین. مقدار
     * خودش می‌تواند {@code =} داشته باشد (Base64 و URL-encoded)، پس
     * نباید با {@code split("=")} و گرفتن عنصر دوم کار کرد.
     */
    private static Map<String, String> parseQuery(String initData) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : initData.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int separator = pair.indexOf('=');
            if (separator > 0) {
                params.putIfAbsent(
                        pair.substring(0, separator),
                        pair.substring(separator + 1));
            }
        }
        return params;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
