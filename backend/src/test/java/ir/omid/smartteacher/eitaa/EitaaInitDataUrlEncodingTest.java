package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.common.error.EitaaInitDataInvalidException;
import ir.omid.smartteacher.eitaa.internal.InMemoryReplayGuard;
import ir.omid.smartteacher.eitaa.internal.LocalHmacVerifier;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * قاعده URL-encoding: مقادیر <b>پیش از HMAC رمزگشایی نمی‌شوند</b>.
 *
 * <p><b>🔴 چرا این کلاس جدا وجود دارد؟</b> چون این یک دام ظریف است که
 * کد را «درست به‌نظر می‌رساند» ولی در Production همه ورودها را می‌شکند.
 * مقدارها در {@code initData} از قبل URL-encoded هستند و <i>همان شکل</i>
 * امضا شده. اگر کسی با نیت خوب بگوید «خب، اول decode می‌کنم که
 * خوانا باشد، بعد امضا را حساب می‌کنم»، {@code data_check_string} با
 * آنچه ایتا امضا کرده یکی نمی‌شود — و نتیجه، امضایی است که <b>هرگز</b>
 * نمی‌خواند، بدون هیچ سرنخی از علت.
 *
 * <p>پس دو چیز باید هم‌زمان درست باشند: امضا روی مقدار encoded حساب
 * شود، و مقدار <i>کاربر</i> پس از تأیید امضا رمزگشایی شود تا نام
 * فارسی سالم دربیاید. این کلاس هر دو را می‌سنجد.
 */
class EitaaInitDataUrlEncodingTest {

    private static final long USER_ID = 424_242_424L;

    private static EitaaInitDataValidator validator() {
        EitaaProperties properties = EitaaTestData.botProperties();
        return new EitaaInitDataValidator(
                properties,
                new LocalHmacVerifier(properties),
                new InMemoryReplayGuard(),
                EitaaTestData.mapper());
    }

    /** JSON کاربر با نام دلخواه — برای آزمون کاراکترهای ویژه. */
    private static String userJson(long id, String firstName, String lastName) {
        return "{\"id\":" + id
                + ",\"first_name\":\"" + firstName + "\""
                + ",\"last_name\":\"" + lastName + "\""
                + ",\"language_code\":\"fa\"}";
    }

    /** پارامترها با فیلد {@code user} دلخواه — به‌شکل encoded. */
    private static Map<String, String> paramsWithUser(String rawUserJson) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond()));
        params.put("user", EitaaTestData.encode(rawUserJson));
        return params;
    }

    /**
     * ساخت initData با هش دلخواه — بدون امضای دوباره.
     *
     * <p>لازم است چون آزمون منفی باید {@code initData}ای بسازد که
     * امضایش روی مقادیر <i>رمزگشایی‌شده</i> حساب شده.
     */
    private static String withHash(Map<String, String> encodedParams, String hash) {
        StringBuilder initData = new StringBuilder();
        encodedParams.forEach((key, value) ->
                initData.append(key).append('=').append(value).append('&'));
        return initData.append("hash=").append(hash).toString();
    }

    // ─────────────────────────── رمزگشایی پس از امضا ───────────────────────────

    @Test
    @DisplayName("نام فارسی پس از تأیید امضا سالم رمزگشایی می‌شود")
    void decodesUserAfterSignatureCheck() {
        Map<String, String> params = paramsWithUser(userJson(USER_ID, "علی", "احمدی"));

        EitaaInitData result = validator().validate(EitaaTestData.signed(params));

        assertThat(result.user().firstName()).isEqualTo("علی");
        assertThat(result.user().lastName()).isEqualTo("احمدی");
    }

    @Test
    @DisplayName("نام با فاصله سالم رمزگشایی می‌شود")
    void decodesNameWithSpace() {
        Map<String, String> params = paramsWithUser(userJson(USER_ID, "زهرا", "محمدی نیا"));

        EitaaInitData result = validator().validate(EitaaTestData.signed(params));

        assertThat(result.user().lastName()).isEqualTo("محمدی نیا");
    }

    @Test
    @DisplayName("کاراکترهای ویژه در نام، پارس را نمی‌شکنند")
    void handlesSpecialCharactersInName() {
        // ⚠️ `&` و `=` اگر encoded نمی‌شدند، پارس پرس‌وجو را می‌شکستند:
        // `&` یک پارامتر تازه می‌ساخت و `=` کلید و مقدار را جابه‌جا
        // می‌کرد. چون encoded هستند، هیچ‌کدام رخ نمی‌دهد.
        Map<String, String> params = paramsWithUser(
                userJson(USER_ID, "a&b=c", "100% خالص"));

        EitaaInitData result = validator().validate(EitaaTestData.signed(params));

        assertThat(result.user().firstName()).isEqualTo("a&b=c");
        assertThat(result.user().lastName()).isEqualTo("100% خالص");
    }

    // ─────────────────────────── آزمون منفی ───────────────────────────

    @Test
    @DisplayName("امضای حساب‌شده روی مقادیر رمزگشایی‌شده رد می‌شود")
    void rejectsSignatureComputedOverDecodedValues() {
        // 🔴 این همان اشتباهی است که کلاس برای گرفتنش ساخته شده:
        // اگر کسی پیش از HMAC رمزگشایی کند، هشش این می‌شود.
        Map<String, String> encodedParams = paramsWithUser(userJson(USER_ID, "علی", "احمدی"));

        Map<String, String> decodedParams = new LinkedHashMap<>(encodedParams);
        decodedParams.put("user", userJson(USER_ID, "علی", "احمدی"));

        String wrongHash = EitaaTestData.hmacHex(
                EitaaTestData.secretKey(EitaaTestData.BOT_TOKEN),
                EitaaTestData.dataCheckString(decodedParams));

        String initData = withHash(encodedParams, wrongHash);

        assertThatThrownBy(() -> validator().validate(initData))
                .as("امضا باید روی مقدار encoded حساب شود، نه رمزگشایی‌شده")
                .isInstanceOf(EitaaInitDataInvalidException.class);
    }

    @Test
    @DisplayName("مقدار رمزگشایی‌نشده در data_check_string، امضا را می‌شکند")
    void signatureIsOverEncodedForm() {
        Map<String, String> params = paramsWithUser(userJson(USER_ID, "علی", "احمدی"));

        // اثبات مثبت: همان پارامترها وقتی به‌شکل encoded امضا شوند
        // می‌گذرند. اگر کد رمزگشایی می‌کرد، این آزمون می‌افتاد.
        assertThatCode(() -> validator().validate(EitaaTestData.signed(params)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("فیلد user غیرقابل رمزگشایی، خطای واضح می‌دهد")
    void reportsUnreadableUserField() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("auth_date", String.valueOf(Instant.now().getEpochSecond()));
        // نه JSON است و نه قابل رمزگشایی به JSON.
        params.put("user", EitaaTestData.encode("not-json-at-all"));

        assertThatThrownBy(() -> validator().validate(EitaaTestData.signed(params)))
                .isInstanceOf(EitaaInitDataInvalidException.class);
    }

    @Test
    @DisplayName("user بدون شناسه رد می‌شود")
    void rejectsUserWithoutId() {
        Map<String, String> params = paramsWithUser("{\"first_name\":\"بی‌نام\"}");

        assertThatThrownBy(() -> validator().validate(EitaaTestData.signed(params)))
                .as("بدون شناسه نمی‌شود کاربر را تشخیص داد")
                .isInstanceOf(EitaaInitDataInvalidException.class);
    }
}
