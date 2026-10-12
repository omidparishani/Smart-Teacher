package ir.omid.smartteacher.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * آزمون پوشاندن داده حساس.
 *
 * <p>این کلاس جای رمزنگاری را نمی‌گیرد — فقط تضمین می‌کند وقتی قرار است
 * چیزی در Log یا پاسخ مدیریتی بنشیند، مقدار کامل آن نباشد.
 *
 * <p>نکته‌ای که در طراحی مهم بود و اینجا آزموده می‌شود: قالب‌های
 * ناشناخته <b>کامل</b> پوشانده می‌شوند. حدس‌زدن اینکه کدام بخش یک
 * قالب ناشناس بی‌خطر است، خودش یک ریسک امنیتی است.
 */
class SensitiveDataMaskerTest {

    @Nested
    @DisplayName("maskEmail")
    class Email {

        @Test
        @DisplayName("اولین کاراکتر و دامنه آشکار می‌ماند")
        void keepsFirstCharacterAndDomain() {
            assertThat(SensitiveDataMasker.maskEmail("ali@example.com"))
                    .isEqualTo("a***@example.com");
        }

        @Test
        @DisplayName("بقیه نام کاربری لو نمی‌رود")
        void hidesLocalPart() {
            String masked = SensitiveDataMasker.maskEmail("mohammadreza@school.ir");

            assertThat(masked).doesNotContain("ohammadreza").endsWith("@school.ir");
        }

        @Test
        @DisplayName("null و رشته خالی به رشته خالی تبدیل می‌شوند")
        void handlesNullAndBlank() {
            assertThat(SensitiveDataMasker.maskEmail(null)).isEmpty();
            assertThat(SensitiveDataMasker.maskEmail("")).isEmpty();
            assertThat(SensitiveDataMasker.maskEmail("   ")).isEmpty();
        }

        @Test
        @DisplayName("قالب ناشناخته کامل پوشانده می‌شود")
        void masksUnknownFormatEntirely() {
            assertThat(SensitiveDataMasker.maskEmail("no-at-sign")).isEqualTo("***");
            assertThat(SensitiveDataMasker.maskEmail("@example.com")).isEqualTo("***");
        }
    }

    @Nested
    @DisplayName("maskMobile")
    class Mobile {

        @Test
        @DisplayName("چهار رقم اول و چهار رقم آخر آشکار می‌ماند")
        void keepsOuterDigits() {
            assertThat(SensitiveDataMasker.maskMobile("09123456789"))
                    .isEqualTo("0912***6789");
        }

        @Test
        @DisplayName("ارقام میانی لو نمی‌روند")
        void hidesMiddleDigits() {
            String masked = SensitiveDataMasker.maskMobile("09123456789");

            assertThat(masked).doesNotContain("3456");
        }

        @Test
        @DisplayName("طول غیرمنتظره کامل پوشانده می‌شود")
        void masksUnexpectedLengthEntirely() {
            // حدس‌زدن اینکه کدام بخش یک قالب ناشناس بی‌خطر است،
            // خودش ریسک است؛ پس کل مقدار پوشانده می‌شود.
            assertThat(SensitiveDataMasker.maskMobile("12345")).isEqualTo("***");
            assertThat(SensitiveDataMasker.maskMobile("+989123456789")).isEqualTo("***");
            assertThat(SensitiveDataMasker.maskMobile("091234567890")).isEqualTo("***");
        }

        @Test
        @DisplayName("null و رشته خالی به رشته خالی تبدیل می‌شوند")
        void handlesNullAndBlank() {
            assertThat(SensitiveDataMasker.maskMobile(null)).isEmpty();
            assertThat(SensitiveDataMasker.maskMobile("")).isEmpty();
        }
    }

    @Nested
    @DisplayName("maskSecret")
    class Secret {

        @Test
        @DisplayName("چهار کاراکتر آخر آشکار می‌ماند")
        void revealsLastFourCharacters() {
            assertThat(SensitiveDataMasker.maskSecret("sk-1234567890ABCD"))
                    .endsWith("ABCD");
        }

        @Test
        @DisplayName("تعداد نقطه‌ها از طول راز چیزی نمی‌گوید")
        void doesNotRevealLength() {
            String shortMasked = SensitiveDataMasker.maskSecret("abcdefgh");
            String longMasked = SensitiveDataMasker.maskSecret("abcdefghijklmnopqrstuvwxyz");

            assertThat(shortMasked.chars().filter(c -> c == '•').count())
                    .isEqualTo(longMasked.chars().filter(c -> c == '•').count());
        }

        @Test
        @DisplayName("راز کوتاه کاملاً پوشانده می‌شود")
        void shortSecretIsFullyMasked() {
            assertThat(SensitiveDataMasker.maskSecret("ab")).isEqualTo("••••••••");
        }

        @Test
        @DisplayName("null و رشته خالی به رشته خالی تبدیل می‌شوند")
        void handlesNullAndEmpty() {
            assertThat(SensitiveDataMasker.maskSecret(null)).isEmpty();
            assertThat(SensitiveDataMasker.maskSecret("")).isEmpty();
        }
    }

    @Nested
    @DisplayName("سازگاری با SecretEncryptor.mask")
    class CrossLayerConsistency {

        @Test
        @DisplayName("هر دو لایه یک خروجی می‌دهند")
        void matchesSecretEncryptorMask() {
            // این دو متد عمداً جدا پیاده شده‌اند تا لایه‌های دیگر مجبور
            // نباشند برای یک نمایش ساده به ماژول رمزنگاری وابسته شوند.
            // اما خروجی‌شان باید یکسان بماند، وگرنه کاربر دو نمایش
            // مختلف از یک راز می‌بیند و گیج می‌شود.
            String secret = "sk-1234567890ABCD";

            assertThat(SensitiveDataMasker.maskSecret(secret))
                    .isEqualTo(ir.omid.smartteacher.security.crypto.SecretEncryptor.mask(secret));
        }
    }
}
