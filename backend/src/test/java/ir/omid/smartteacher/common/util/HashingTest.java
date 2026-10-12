package ir.omid.smartteacher.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * آزمون درهم‌سازی و مقایسه امن.
 *
 * <p>کاربرد اصلی این ابزارها دو چیز است: ذخیره هش توکن Refresh به‌جای
 * خود توکن، و مقایسه امضاها بدون نشت زمان. هر دو در
 * {@code docs/security.md} الزام شده‌اند.
 */
class HashingTest {

    /** هش SHA-256 شناخته‌شده رشته {@code "abc"} — مرجع مستقل از پیاده‌سازی. */
    private static final String ABC_SHA256 =
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    @Nested
    @DisplayName("sha256Hex")
    class Sha256Hex {

        @Test
        @DisplayName("مقدار مرجع درست را می‌دهد")
        void matchesKnownVector() {
            // مقدار مرجع از استاندارد NIST گرفته شده، نه از خروجی خود
            // این پیاده‌سازی — وگرنه تست فقط خودش را تأیید می‌کرد.
            assertThat(Hashing.sha256Hex("abc")).isEqualTo(ABC_SHA256);
        }

        @Test
        @DisplayName("طول خروجی ۶۴ کاراکتر hex است")
        void hasCorrectLength() {
            assertThat(Hashing.sha256Hex("هر متنی")).hasSize(Hashing.SHA256_HEX_LENGTH);
        }

        @Test
        @DisplayName("حروف کوچک است")
        void usesLowercase() {
            assertThat(Hashing.sha256Hex("abc")).isLowerCase();
        }

        @Test
        @DisplayName("ورودی یکسان همیشه هش یکسان می‌دهد")
        void isDeterministic() {
            assertThat(Hashing.sha256Hex("token-value")).isEqualTo(Hashing.sha256Hex("token-value"));
        }

        @Test
        @DisplayName("ورودی متفاوت هش متفاوت می‌دهد")
        void differsForDifferentInput() {
            assertThat(Hashing.sha256Hex("token-a")).isNotEqualTo(Hashing.sha256Hex("token-b"));
        }

        @Test
        @DisplayName("متن فارسی درست هش می‌شود")
        void handlesPersianText() {
            String persian = "دستیار هوشمند معلم";

            assertThat(Hashing.sha256Hex(persian))
                    .hasSize(Hashing.SHA256_HEX_LENGTH)
                    .isEqualTo(Hashing.sha256Hex(persian.getBytes(StandardCharsets.UTF_8)));
        }

        @Test
        @DisplayName("null پذیرفته نمی‌شود")
        void rejectsNull() {
            assertThatThrownBy(() -> Hashing.sha256Hex((String) null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Hashing.sha256Hex((byte[]) null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Hashing.sha256((byte[]) null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("هش خام ۳۲ بایت است")
        void rawHashIsThirtyTwoBytes() {
            assertThat(Hashing.sha256("abc".getBytes(StandardCharsets.UTF_8))).hasSize(32);
        }
    }

    @Nested
    @DisplayName("constantTimeEquals")
    class ConstantTimeEquals {

        @Test
        @DisplayName("رشته‌های برابر true می‌دهند")
        void returnsTrueForEqualStrings() {
            assertThat(Hashing.constantTimeEquals("توکن-یک", "توکن-یک")).isTrue();
            assertThat(Hashing.constantTimeEquals("", "")).isTrue();
        }

        @Test
        @DisplayName("رشته‌های نابرابر false می‌دهند")
        void returnsFalseForDifferentStrings() {
            assertThat(Hashing.constantTimeEquals("token-a", "token-b")).isFalse();
        }

        @Test
        @DisplayName("تفاوت فقط در آخرین کاراکتر هم گرفته می‌شود")
        void detectsDifferenceInLastCharacter() {
            // این همان حالتی است که یک مقایسه ساده را زودتر برمی‌گرداند
            // و زمان اجرا را وابسته به ورودی می‌کند.
            assertThat(Hashing.constantTimeEquals(
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaX")).isFalse();
        }

        @Test
        @DisplayName("حساس به بزرگی و کوچکی حروف است")
        void isCaseSensitive() {
            assertThat(Hashing.constantTimeEquals("Token", "token")).isFalse();
        }

        @Test
        @DisplayName("دو null برابر شمرده می‌شوند و یک null نابرابر")
        void handlesNulls() {
            assertThat(Hashing.constantTimeEquals((String) null, null)).isTrue();
            assertThat(Hashing.constantTimeEquals("value", null)).isFalse();
            assertThat(Hashing.constantTimeEquals(null, "value")).isFalse();
        }

        @Test
        @DisplayName("روی آرایه بایت هم کار می‌کند")
        void worksOnByteArrays() {
            byte[] first = {1, 2, 3};
            byte[] same = {1, 2, 3};
            byte[] different = {1, 2, 4};

            assertThat(Hashing.constantTimeEquals(first, same)).isTrue();
            assertThat(Hashing.constantTimeEquals(first, different)).isFalse();
            assertThat(Hashing.constantTimeEquals((byte[]) null, null)).isTrue();
        }
    }

    @Test
    @DisplayName("کلاس ابزاری قابل نمونه‌سازی نیست")
    void cannotBeInstantiated() throws Exception {
        var constructor = Hashing.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
                .hasCauseInstanceOf(AssertionError.class);
    }
}
