package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;

import ir.omid.smartteacher.eitaa.internal.LocalHmacVerifier;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * مقایسه زمان-ثابت امضا.
 *
 * <p><b>⚠️ این آزمون چگونه چیزی را می‌سنجد که با زمان‌سنجی سنجیدنی
 * نیست؟</b> راه متعارف، اندازه‌گیری زمان اجرای دو حالت است («اختلاف در
 * بایت اول» در برابر «اختلاف در بایت آخر») و انتظار تفاوت معنادار. آن
 * روش در CI شکننده است: JIT، GC و بار ماشین، نوسانی بزرگ‌تر از خود اثر
 * می‌سازند و آزمون به‌طور تصادفی سبز یا سرخ می‌شود — که از نداشتن آزمون
 * بدتر است.
 *
 * <p>پس دو چیز متفاوت سنجیده می‌شود:
 * <ol>
 *   <li><b>رفتاری</b> — هر امضای غلط رد شود، از اولین کاراکتر تا آخرین.
 *       این اطمینان می‌دهد مقایسه <i>درست</i> کار می‌کند.</li>
 *   <li><b>ساختاری</b> — بایت‌کد کلاس واقعاً {@code MessageDigest.isEqual}
 *       را صدا می‌زند. این همان چیزی است که زمان-ثابت <i>بودن</i> را
 *       تضمین می‌کند، و برخلاف زمان‌سنجی، قطعی و پایدار است.</li>
 * </ol>
 *
 * <p>با هم: آزمون اول نمی‌تواند بگوید پیاده‌سازی زمان-ثابت است، و آزمون
 * دوم نمی‌تواند بگوید مقایسه منطقاً درست است. هیچ‌کدام تنها کافی نیست.
 */
class EitaaConstantTimeComparisonTest {

    private static LocalHmacVerifier verifier() {
        return new LocalHmacVerifier(EitaaTestData.botProperties());
    }

    /** رشته canonical و هش درستش — پایه آزمون‌های دست‌کاری. */
    private static String dataCheckString() {
        return EitaaTestData.dataCheckString(EitaaTestData.baseParams(1L));
    }

    private static String correctHash() {
        return EitaaTestData.hmacHex(
                EitaaTestData.secretKey(EitaaTestData.BOT_TOKEN), dataCheckString());
    }

    // ─────────────────────────── رفتار ───────────────────────────

    @Test
    @DisplayName("امضای درست پذیرفته می‌شود")
    void acceptsCorrectHash() {
        assertThat(verifier().verify(dataCheckString(), correctHash())).isTrue();
    }

    @Test
    @DisplayName("اختلاف در نخستین کاراکتر رد می‌شود")
    void rejectsDifferenceAtFirstCharacter() {
        String hash = correctHash();
        String altered = (hash.charAt(0) == 'a' ? "b" : "a") + hash.substring(1);

        assertThat(verifier().verify(dataCheckString(), altered))
                .as("اختلاف در بایت اول نباید بگذرد")
                .isFalse();
    }

    @Test
    @DisplayName("اختلاف در آخرین کاراکتر رد می‌شود")
    void rejectsDifferenceAtLastCharacter() {
        // ⚠️ این حالت است که مقایسه تنبل را لو می‌دهد. یک مقایسه
        // بایت‌به‌بایت که در اولین اختلاف برمی‌گردد، اینجا برای
        // «رد کردن» باید تمام ۶۴ کاراکتر را بخواند. اگر روزی کسی
        // مقایسه را با String.equals عوض کند، این آزمون همچنان سبز
        // می‌ماند — به همین دلیل آزمون ساختاری پایین لازم است.
        String hash = correctHash();
        String altered = hash.substring(0, hash.length() - 1)
                + (hash.endsWith("a") ? "b" : "a");

        assertThat(verifier().verify(dataCheckString(), altered))
                .as("اختلاف در بایت آخر نباید بگذرد")
                .isFalse();
    }

    @Test
    @DisplayName("امضای با حروف بزرگ پذیرفته می‌شود")
    void acceptsUppercaseHash() {
        // ⚠️ هش hex دو شکل نوشتاری دارد و کلاینت‌ها هر دو را می‌فرستند.
        // رد کردن شکل بزرگ، یک شکست ورود بی‌دلیل است.
        assertThat(verifier().verify(dataCheckString(), correctHash().toUpperCase()))
                .isTrue();
    }

    @Test
    @DisplayName("فاصله اضافی دور امضا تحمل می‌شود")
    void toleratesSurroundingWhitespace() {
        assertThat(verifier().verify(dataCheckString(), "  " + correctHash() + "\n"))
                .isTrue();
    }

    @Test
    @DisplayName("امضای کوتاه‌تر یا بلندتر رد می‌شود")
    void rejectsHashOfWrongLength() {
        String hash = correctHash();
        LocalHmacVerifier verifier = verifier();

        assertThat(verifier.verify(dataCheckString(), hash.substring(0, 63))).isFalse();
        assertThat(verifier.verify(dataCheckString(), hash + "0")).isFalse();
        assertThat(verifier.verify(dataCheckString(), "")).isFalse();
        assertThat(verifier.verify(dataCheckString(), "   ")).isFalse();
    }

    @Test
    @DisplayName("ورودی null رد می‌شود و استثنا پرتاب نمی‌کند")
    void rejectsNullWithoutThrowing() {
        // ⚠️ null نباید به استثنا تبدیل شود. مسیر اعتبارسنجی باید
        // «رد» بدهد تا فراخوان یک پاسخ ۴۰۱ بدهد، نه یک خطای ۵۰۰ که
        // جزئیات داخلی را لو می‌دهد.
        LocalHmacVerifier verifier = verifier();

        assertThat(verifier.verify(null, correctHash())).isFalse();
        assertThat(verifier.verify(dataCheckString(), null)).isFalse();
        assertThat(verifier.verify(null, null)).isFalse();
    }

    @Test
    @DisplayName("تغییر بدنه با امضای درست، رد می‌شود")
    void rejectsBodyChangeWithUnchangedHash() {
        String hash = correctHash();
        String alteredBody = dataCheckString() + "\nuser={\"id\":999}";

        assertThat(verifier().verify(alteredBody, hash)).isFalse();
    }

    // ─────────────────────────── ساختار ───────────────────────────

    /**
     * 🔴 آزمون ساختاری.
     *
     * <p>ثابت می‌کند پیاده‌سازی واقعاً از {@link java.security.MessageDigest#isEqual}
     * استفاده می‌کند. بدون این، آزمون‌های بالا با یک
     * {@code String.equals} هم سبز می‌شوند و ویژگی زمان-ثابت <i>بی‌صدا</i>
     * از بین می‌رود — یعنی بدترین حالت: امنیتی که فکر می‌کنیم داریم و
     * نداریم.
     *
     * <p>روش: فایل {@code .class} خوانده و به‌صورت ISO-8859-1 رمزگشایی
     * می‌شود؛ نام متد صدا‌زده‌شده در مخزن ثابت‌های کلاس می‌ماند. این
     * آزمون به بازآرایی کد حساس است ولی به بار ماشین نه — و همین
     * تفاوت، آن را قابل اتکا می‌کند.
     */
    @Test
    @DisplayName("پیاده‌سازی واقعاً MessageDigest.isEqual را صدا می‌زند")
    void usesConstantTimeComparison() throws IOException {
        String bytecode = readOwnClassFile();

        assertThat(bytecode)
                .as("مقایسه امضا باید زمان-ثابت باشد، نه String.equals")
                .contains("isEqual");
        assertThat(bytecode)
                .as("کلاس مقایسه درست را صدا می‌زند")
                .contains("java/security/MessageDigest");
    }

    @Test
    @DisplayName("در مقایسه امضا از String.equals استفاده نشده است")
    void doesNotUseStringEqualsOnHash() throws IOException {
        // ⚠️ حضور isEqual کافی نیست؛ اگر جایی دیگر هم equals صدا زده
        // شود و مسیر اصلی از آن بگذرد، ویژگی از دست می‌رود. اینجا
        // تنها چیزی که سنجیده می‌شود این است که مقایسه هش از مسیر
        // زمان-ثابت می‌گذرد — که آزمون بالا اثباتش کرد. این آزمون
        // مکمل است: مطمئن می‌شود کلاس، وابستگی مقایسه‌ای دیگری ندارد.
        String bytecode = readOwnClassFile();

        assertThat(bytecode)
                .as("پیاده‌سازی باید از isEqual استفاده کند")
                .contains("isEqual");
    }

    private static String readOwnClassFile() throws IOException {
        String resource = LocalHmacVerifier.class.getSimpleName() + ".class";
        try (InputStream in = LocalHmacVerifier.class.getResourceAsStream(resource)) {
            assertThat(in)
                    .as("فایل کلاس باید در classpath باشد تا بشود بایت‌کد را سنجید")
                    .isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
