package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.common.error.EitaaInitDataInvalidException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.eitaa.internal.InMemoryReplayGuard;
import ir.omid.smartteacher.eitaa.internal.LocalHmacVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * رد کردن {@code initData} دست‌کاری‌شده.
 *
 * <p>هر آزمون این کلاس یک <b>تغییر مشخص</b> را می‌سنجد. تغییر عمومی
 * («یک کاراکتر را عوض کن») ضعیف است چون ممکن است تصادفاً فیلدی را عوض
 * کند که اثرش با فیلد دیگری خنثی شود؛ اینجا هر تغییر هدفمند است.
 *
 * <p>قاعده مشترک: هر تغییری در پارامترها باید امضا را باطل کند، و هر
 * تغییری در خود {@code hash} هم باید رد شود. اگر روزی یکی از این‌ها
 * بگذرد، یعنی امضا دیگر همه‌چیز را پوشش نمی‌دهد.
 */
class EitaaInitDataTamperedTest {

    private static final long USER_ID = 123_456_789L;

    private static EitaaInitDataValidator validator(EitaaProperties properties) {
        return new EitaaInitDataValidator(
                properties,
                new LocalHmacVerifier(properties),
                new InMemoryReplayGuard(),
                EitaaTestData.mapper());
    }

    private static EitaaInitDataValidator validator() {
        return validator(EitaaTestData.botProperties());
    }

    /** جایگزینی مقدار یک پارامتر، بدون امضای دوباره. */
    private static String tamper(String initData, String key, String newValue) {
        String[] pairs = initData.split("&");
        StringBuilder result = new StringBuilder();

        for (String pair : pairs) {
            if (!result.isEmpty()) {
                result.append('&');
            }
            int separator = pair.indexOf('=');
            String pairKey = pair.substring(0, separator);

            result.append(pairKey).append('=')
                    .append(pairKey.equals(key) ? newValue : pair.substring(separator + 1));
        }

        return result.toString();
    }

    /** جایگزینی مقدار hash با مقدار داده‌شده. */
    private static String withHash(String initData, String hash) {
        return tamper(initData, "hash", hash);
    }

    // ─────────────────────────── دست‌کاری ───────────────────────────

    @Test
    @DisplayName("hash عوض‌شده رد می‌شود")
    void rejectsAlteredHash() {
        String initData = EitaaTestData.validInitData(USER_ID);
        String altered = withHash(initData, "0".repeat(64));

        assertThatThrownBy(() -> validator().validate(altered))
                .isInstanceOf(EitaaInitDataInvalidException.class);
    }

    @Test
    @DisplayName("یک کاراکتر hash عوض شود هم رد می‌شود")
    void rejectsHashWithSingleCharacterChanged() {
        String initData = EitaaTestData.validInitData(USER_ID);
        String originalHash = initData.substring(initData.indexOf("hash=") + "hash=".length());

        char first = originalHash.charAt(0);
        String alteredHash = ((first == 'a') ? "b" : "a") + originalHash.substring(1);

        assertThatThrownBy(() -> validator().validate(withHash(initData, alteredHash)))
                .as("تغییر یک کاراکتر در امضا نباید بگذرد")
                .isInstanceOf(EitaaInitDataInvalidException.class);
    }

    @Test
    @DisplayName("شناسه کاربر عوض‌شده رد می‌شود")
    void rejectsAlteredUserId() {
        String initData = EitaaTestData.validInitData(USER_ID);
        String otherUser = EitaaTestData.encode(EitaaTestData.userJson(999L));

        // 🔴 این خطرناک‌ترین حمله است: اگر بگذرد، مهاجم می‌تواند
        // شناسه خودش را به شناسه معلم دیگری عوض کند و به کلاس‌های او
        // دسترسی بگیرد.
        assertThatThrownBy(() -> validator().validate(tamper(initData, "user", otherUser)))
                .as("جعل هویت با عوض‌کردن user.id نباید ممکن باشد")
                .isInstanceOf(EitaaInitDataInvalidException.class);
    }

    @Test
    @DisplayName("auth_date عوض‌شده رد می‌شود")
    void rejectsAlteredAuthDate() {
        String initData = EitaaTestData.validInitData(USER_ID);

        assertThatThrownBy(() -> validator().validate(
                tamper(initData, "auth_date", "9999999999")))
                .as("اگر auth_date قابل تغییر بود، تازگی بی‌معنا می‌شد")
                .isInstanceOf(EitaaInitDataInvalidException.class);
    }

    @Test
    @DisplayName("پارامتر تازه‌افزوده امضا را باطل می‌کند")
    void rejectsAddedParameter() {
        String initData = EitaaTestData.validInitData(USER_ID);
        String withExtra = initData.replace("&hash=", "&device_id=injected&hash=");

        assertThatThrownBy(() -> validator().validate(withExtra))
                .as("افزودن پارامتر باید امضا را بشکند، وگرنه هر کسی می‌تواند فیلد تزریق کند")
                .isInstanceOf(EitaaInitDataInvalidException.class);
    }

    @Test
    @DisplayName("امضای توکن ربات دیگر رد می‌شود")
    void rejectsSignatureFromAnotherBotToken() {
        // ⚠️ این سناریو وقتی واقعی می‌شود که توکن ربات ما لو رفته باشد
        // و کسی با یک ربات دیگر initData بسازد. چون کلید امضا از توکن
        // مشتق می‌شود، امضای ربات دیگر باید بی‌اثر باشد.
        String foreign = EitaaTestData.signed(
                EitaaTestData.baseParams(USER_ID), EitaaTestData.OTHER_BOT_TOKEN);

        assertThatThrownBy(() -> validator().validate(foreign))
                .as("امضای ربات دیگر نباید با توکن ما بخواند")
                .isInstanceOf(EitaaInitDataInvalidException.class);
    }

    @Test
    @DisplayName("hash خالی رد می‌شود")
    void rejectsEmptyHash() {
        String initData = EitaaTestData.validInitData(USER_ID);

        assertThatThrownBy(() -> validator().validate(withHash(initData, "")))
                .isInstanceOf(EitaaInitDataInvalidException.class);
    }

    @Test
    @DisplayName("بدنه ناقص و بی‌ربط رد می‌شود")
    void rejectsGarbage() {
        assertThatThrownBy(() -> validator().validate("این یک initData نیست"))
                .isInstanceOf(EitaaInitDataInvalidException.class);
    }

    @Test
    @DisplayName("پیام خطا نمی‌گوید کدام فیلد خراب بود")
    void errorMessageDoesNotRevealWhichFieldFailed() {
        // ⚠️ اگر پیام بگوید «امضای user نخواند»، مهاجم می‌فهمد کدام فیلد
        // را باید دست بزند و کدام درست بود — یعنی یک اوراکل برای
        // بهینه‌کردن حمله ساخته‌ایم. دلیل واقعی فقط در technicalReason
        // می‌ماند که به Log می‌رود، نه به کاربر.
        String initData = EitaaTestData.validInitData(USER_ID);
        String tamperedUser = EitaaTestData.encode(EitaaTestData.userJson(999L));

        assertThatThrownBy(() -> validator().validate(tamper(initData, "user", tamperedUser)))
                .as("پیام کاربر باید عمومی بماند")
                .hasMessage(ErrorCode.EITAA_INIT_DATA_INVALID.defaultMessage())
                .as("و نام فیلد و مقدارش را لو ندهد")
                .hasMessageNotContaining("user")
                .hasMessageNotContaining("hash")
                .hasMessageNotContaining(tamperedUser);
    }
}
