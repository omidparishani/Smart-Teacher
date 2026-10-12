package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;

import ir.omid.smartteacher.eitaa.internal.LocalHmacVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ⭐ بردار آزمون مستندشده — <b>جهت کلید</b>.
 *
 * <p><b>🔴 چرا این آزمون مهم‌ترین آزمون کل ماژول ایتا است؟</b> چون
 * جهت کلید در مرحله ۵ الگوریتم ایتا، یک دام است: کلید رشته ثابت
 * {@code "WebAppData"} است و پیام، توکن ربات — نه برعکس.
 *
 * <p>اگر کسی این دو را جابه‌جا کند، همه‌چیز <i>به‌نظر</i> درست کار
 * می‌کند: کد کامپایل می‌شود، آزمون‌هایی که با fixture امضا می‌کنند سبز
 * می‌مانند (چون fixture هم همان اشتباه را می‌کند)، و فقط در Production
 * معلوم می‌شود که هیچ ورودی‌ای از ایتا پذیرفته نمی‌شود — با خطای
 * «امضای HMAC نخواند» که هیچ سرنخی از علت نمی‌دهد.
 *
 * <p>این آزمون همان دام را با یک مقدار <b>بیرونی و مستندشده</b> می‌بندد:
 * مقدار درست از سند رسمی می‌آید، نه از کد ما. پس اگر هر دو پیاده‌سازی
 * (Production و fixture) اشتباه باشند، هر دو اینجا می‌افتند.
 */
class EitaaSecretKeyDerivationTest {

    @Test
    @DisplayName("پیاده‌سازی Production همان کلید مستندشده را می‌دهد")
    void productionDerivationMatchesDocumentedVector() {
        String derived = LocalHmacVerifier.deriveSecretKeyHex(EitaaTestData.BOT_TOKEN);

        assertThat(derived)
                .as("کلید مشتق‌شده باید عیناً همان مقدار docs/eitaa.md بخش ۷.۲ باشد")
                .isEqualTo(EitaaTestData.DOCUMENTED_SECRET_KEY_HEX);
    }

    @Test
    @DisplayName("پیاده‌سازی مستقل fixture هم همان کلید را می‌دهد")
    void fixtureDerivationMatchesDocumentedVector() {
        // ⚠️ این آزمون عمداً جدا از بالایی است. fixture و Production دو
        // پیاده‌سازی مستقل‌اند؛ اگر فقط یکی را با بردار می‌سنجیدیم،
        // دیگری می‌توانست بی‌سروصدا از آن فاصله بگیرد و همه آزمون‌های
        // وابسته به fixture بی‌اعتبار شوند.
        String derived = EitaaTestData.secretKeyHex(EitaaTestData.BOT_TOKEN);

        assertThat(derived)
                .as("fixture آزمون باید همان الگوریتم سند را پیاده کرده باشد")
                .isEqualTo(EitaaTestData.DOCUMENTED_SECRET_KEY_HEX);
    }

    @Test
    @DisplayName("جهت جابه‌جا شده، مقدار دیگری می‌دهد — پس دام واقعی است")
    void reversedDirectionProducesADifferentKey() {
        String reversed = EitaaTestData.hmacHex(
                EitaaTestData.BOT_TOKEN.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "WebAppData");

        assertThat(reversed)
                .as("اگر جهت برعکس شود، کلید عوض می‌شود — پس این دام بی‌اثر نیست")
                .isEqualTo(EitaaTestData.REVERSED_DIRECTION_HEX)
                .isNotEqualTo(EitaaTestData.DOCUMENTED_SECRET_KEY_HEX);
    }

    @Test
    @DisplayName("توکن دیگر، کلید دیگر می‌دهد")
    void differentTokenProducesDifferentKey() {
        assertThat(EitaaTestData.secretKeyHex(EitaaTestData.OTHER_BOT_TOKEN))
                .as("کلید باید به توکن وابسته باشد، وگرنه هر رباتی امضای هر رباتی را می‌خواند")
                .isNotEqualTo(EitaaTestData.DOCUMENTED_SECRET_KEY_HEX);
    }

    @Test
    @DisplayName("کلید ۳۲ بایت و در قالب hex ۶۴ کاراکتری است")
    void keyHasExpectedShape() {
        assertThat(EitaaTestData.secretKeyHex(EitaaTestData.BOT_TOKEN))
                .hasSize(64)
                .matches("[0-9a-f]{64}");
    }
}
