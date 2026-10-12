package ir.omid.smartteacher.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.common.error.EncryptionException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * آزمون رمزنگاری کلیدهای API — الزام بخش ۵۳ سند اصلی.
 *
 * <p>سه چیز اینجا آزموده می‌شود که هر سه امنیتی‌اند، نه کارکردی:
 * <ol>
 *   <li><b>تصادفی‌بودن IV:</b> رمز کردن یک متن ثابت باید هر بار خروجی
 *       متفاوت بدهد. اگر IV تکرار شود، محرمانگی GCM می‌شکند و مهاجم
 *       می‌فهمد دو رکورد مقدار یکسان دارند.</li>
 *   <li><b>تشخیص دست‌کاری:</b> تغییر حتی یک بایت باید در رمزگشایی
 *       شناسایی شود. این تمام فایده AEAD است؛ اگر کار نکند، مهاجم با
 *       دسترسی نوشتنی به دیتابیس می‌تواند کلید را عوض کند.</li>
 *   <li><b>کلید اشتباه:</b> داده رمزشده با کلید دیگر باید شکست بخورد،
 *       نه اینکه متن بی‌معنا برگرداند.</li>
 * </ol>
 *
 * <p>هیچ‌کدام از کلیدهای این فایل راز واقعی نیستند.
 */
class SecretEncryptorTest {

    /** Base64 از «0123456789abcdef0123456789abcdef» (۳۲ بایت). */
    private static final String KEY_1 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    /** Base64 از «fedcba9876543210fedcba9876543210» (۳۲ بایت). */
    private static final String KEY_2 = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    private final SecretEncryptor encryptor = new SecretEncryptor(new EncryptionProperties(KEY_1));
    private final SecretEncryptor otherEncryptor = new SecretEncryptor(new EncryptionProperties(KEY_2));

    // ─────────────────────── رفت و برگشت ───────────────────────

    @Nested
    @DisplayName("رفت و برگشت")
    class RoundTrip {

        @Test
        @DisplayName("متن ASCII سالم برمی‌گردد")
        void asciiRoundTrip() {
            String plaintext = "sk-1234567890abcdefghijklmnop";

            assertThat(encryptor.decrypt(encryptor.encrypt(plaintext))).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("متن فارسی و یونیکد سالم برمی‌گردد")
        void unicodeRoundTrip() {
            String plaintext = "کلید آزمایشی — 🔐 テスト";

            assertThat(encryptor.decrypt(encryptor.encrypt(plaintext))).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("رشته خالی سالم برمی‌گردد")
        void emptyStringRoundTrip() {
            assertThat(encryptor.decrypt(encryptor.encrypt(""))).isEmpty();
        }

        @Test
        @DisplayName("null در null می‌ماند و استثنا نمی‌دهد")
        void nullPassesThrough() {
            assertThat(encryptor.encrypt(null)).isNull();
            assertThat(encryptor.decrypt(null)).isNull();
            assertThat(encryptor.encryptBytes(null)).isNull();
            assertThat(encryptor.decryptBytes(null)).isNull();
        }

        @Test
        @DisplayName("داده باینری دلخواه سالم برمی‌گردد")
        void binaryRoundTrip() {
            byte[] data = {0, 1, 2, (byte) 0xFF, (byte) 0x80, 42};

            assertThat(encryptor.decryptBytes(encryptor.encryptBytes(data))).isEqualTo(data);
        }

        @Test
        @DisplayName("متن خام در خروجی دیده نمی‌شود")
        void plaintextDoesNotAppearInOutput() {
            String plaintext = "sk-super-secret-value";
            byte[] payload = encryptor.encryptBytes(plaintext.getBytes(StandardCharsets.UTF_8));

            assertThat(new String(payload, StandardCharsets.ISO_8859_1))
                    .as("متن خام نباید در خروجی رمزشده پیدا شود")
                    .doesNotContain(plaintext);
        }
    }

    // ─────────────────────── IV تصادفی ───────────────────────

    @Nested
    @DisplayName("IV تصادفی")
    class IvUniqueness {

        @Test
        @DisplayName("رمز کردن دو بار یک متن، دو خروجی متفاوت می‌دهد")
        void encryptingTwiceProducesDifferentCiphertext() {
            String plaintext = "same-value-every-time";

            String first = encryptor.encrypt(plaintext);
            String second = encryptor.encrypt(plaintext);

            // اگر این تست شکست بخورد، IV تکرار شده و GCM دیگر امن نیست —
            // با تکرار IV، مهاجم می‌تواند XOR دو متن رمزشده را ببیند.
            assertThat(first)
                    .as("تکرار IV امنیت GCM را می‌شکند؛ خروجی باید متفاوت باشد")
                    .isNotEqualTo(second);
            assertThat(encryptor.decrypt(first)).isEqualTo(plaintext);
            assertThat(encryptor.decrypt(second)).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("در صد رمزگذاری، صد IV متمایز تولید می‌شود")
        void producesDistinctIvs() {
            int samples = 100;
            var ciphertexts = new HashSet<String>();

            for (int i = 0; i < samples; i++) {
                ciphertexts.add(encryptor.encrypt("ثابت"));
            }

            assertThat(ciphertexts).hasSize(samples);
        }
    }

    // ─────────────────────── تشخیص دست‌کاری ───────────────────────

    @Nested
    @DisplayName("تشخیص دست‌کاری")
    class TamperDetection {

        @Test
        @DisplayName("تغییر یک بایت متن رمز، شناسایی می‌شود")
        void flippedCiphertextByteIsDetected() {
            byte[] payload = encryptor.encryptBytes("مقدار اصلی".getBytes(StandardCharsets.UTF_8));

            // آخرین بایت بخشی از برچسب احراز اصالت است.
            payload[payload.length - 1] ^= 0x01;

            assertThatThrownBy(() -> encryptor.decryptBytes(payload))
                    .isInstanceOf(EncryptionException.class);
        }

        @Test
        @DisplayName("تغییر یک بایت IV شناسایی می‌شود")
        void flippedIvByteIsDetected() {
            byte[] payload = encryptor.encryptBytes("مقدار اصلی".getBytes(StandardCharsets.UTF_8));

            payload[1] ^= 0x01; // بایت اول IV

            assertThatThrownBy(() -> encryptor.decryptBytes(payload))
                    .isInstanceOf(EncryptionException.class);
        }

        @Test
        @DisplayName("بریدن انتهای داده شناسایی می‌شود")
        void truncatedPayloadIsRejected() {
            byte[] payload = encryptor.encryptBytes("مقدار اصلی".getBytes(StandardCharsets.UTF_8));
            byte[] truncated = Arrays.copyOf(payload, payload.length - 1);

            assertThatThrownBy(() -> encryptor.decryptBytes(truncated))
                    .isInstanceOf(EncryptionException.class);
        }

        @Test
        @DisplayName("داده کوتاه‌تر از حداقل، بی‌درنگ رد می‌شود")
        void tooShortPayloadIsRejected() {
            assertThatThrownBy(() -> encryptor.decryptBytes(new byte[]{1, 2, 3}))
                    .isInstanceOf(EncryptionException.class);
        }

        @Test
        @DisplayName("نسخه ناشناخته قالب رد می‌شود")
        void unknownFormatVersionIsRejected() {
            byte[] payload = encryptor.encryptBytes("مقدار".getBytes(StandardCharsets.UTF_8));
            payload[0] = 99; // نسخه‌ای که وجود ندارد

            assertThatThrownBy(() -> encryptor.decryptBytes(payload))
                    .as("نسخه ناشناخته باید رد شود تا مسیر مهاجرت آینده امن بماند")
                    .isInstanceOf(EncryptionException.class);
        }

        @Test
        @DisplayName("ورودی Base64 نامعتبر رد می‌شود")
        void invalidBase64IsRejected() {
            assertThatThrownBy(() -> encryptor.decrypt("این Base64 نیست !!!"))
                    .isInstanceOf(EncryptionException.class);
        }
    }

    // ─────────────────────── کلید ───────────────────────

    @Nested
    @DisplayName("کلید")
    class KeyHandling {

        @Test
        @DisplayName("داده رمزشده با کلید دیگر رمزگشایی نمی‌شود")
        void dataEncryptedWithAnotherKeyCannotBeDecrypted() {
            String ciphertext = encryptor.encrypt("راز من");

            assertThatThrownBy(() -> otherEncryptor.decrypt(ciphertext))
                    .as("کلید اشتباه باید شکست بدهد، نه متن بی‌معنا برگرداند")
                    .isInstanceOf(EncryptionException.class);
        }

        @Test
        @DisplayName("کلید null یا خالی پذیرفته نمی‌شود")
        void blankKeyIsRejected() {
            assertThatThrownBy(() -> new SecretEncryptor(new EncryptionProperties(null)))
                    .isInstanceOf(EncryptionException.class);
            assertThatThrownBy(() -> new SecretEncryptor(new EncryptionProperties("   ")))
                    .isInstanceOf(EncryptionException.class);
        }

        @Test
        @DisplayName("کلید با طول نادرست پذیرفته نمی‌شود")
        void wrongLengthKeyIsRejected() {
            String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

            assertThatThrownBy(() -> new SecretEncryptor(new EncryptionProperties(shortKey)))
                    .as("AES-256 به کلید ۳۲ بایتی نیاز دارد")
                    .isInstanceOf(EncryptionException.class);
        }

        @Test
        @DisplayName("کلید غیر Base64 پذیرفته نمی‌شود")
        void nonBase64KeyIsRejected() {
            assertThatThrownBy(() -> new SecretEncryptor(new EncryptionProperties("کلید-فارسی-نه-بیس۶۴")))
                    .isInstanceOf(EncryptionException.class);
        }

        @Test
        @DisplayName("پیام خطای کلید، خود کلید را لو نمی‌دهد")
        void errorMessageDoesNotLeakTheKey() {
            String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

            assertThatThrownBy(() -> new SecretEncryptor(new EncryptionProperties(shortKey)))
                    .hasMessageNotContaining(shortKey);
        }
    }

    // ─────────────────────── پوشاندن ───────────────────────

    @Nested
    @DisplayName("mask")
    class Masking {

        @Test
        @DisplayName("چهار کاراکتر آخر آشکار می‌ماند")
        void revealsLastFourCharacters() {
            assertThat(SecretEncryptor.mask("sk-1234567890ABCD")).endsWith("ABCD");
        }

        @Test
        @DisplayName("طول اصلی لو نمی‌رود")
        void doesNotRevealLength() {
            String masked = SecretEncryptor.mask("a-very-long-secret-value-here");

            assertThat(masked.chars().filter(c -> c == '•').count())
                    .as("تعداد نقطه‌ها ثابت است و از طول راز چیزی نمی‌گوید")
                    .isEqualTo(8);
        }

        @Test
        @DisplayName("راز کوتاه کاملاً پوشانده می‌شود")
        void shortSecretIsFullyMasked() {
            assertThat(SecretEncryptor.mask("abcd")).isEqualTo("••••••••");
            assertThat(SecretEncryptor.mask("ab")).isEqualTo("••••••••");
        }

        @Test
        @DisplayName("null و خالی به رشته خالی تبدیل می‌شوند")
        void handlesNullAndEmpty() {
            assertThat(SecretEncryptor.mask(null)).isEmpty();
            assertThat(SecretEncryptor.mask("")).isEmpty();
        }
    }
}
