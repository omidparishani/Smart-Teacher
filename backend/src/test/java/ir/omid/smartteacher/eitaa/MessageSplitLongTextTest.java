package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;

import ir.omid.smartteacher.eitaa.internal.EitaaTextSplitter;
import ir.omid.smartteacher.eitaa.internal.EitaaYarBotAdapter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * تقسیم پیام بلند.
 *
 * <p><b>چرا تقسیم لازم است؟</b> ایتا برای طول متن سقف دارد و پیام بلندتر
 * را رد می‌کند. سقف دقیق در سند رسمی نیامده، پس سقف محافظه‌کارانه‌ای
 * می‌گیریم و پیام بلند را <i>تقسیم</i> می‌کنیم. رد شدن یک اعلان تکلیف،
 * بدتر از رسیدنش در دو پیام است.
 *
 * <p>دو لایه سنجیده می‌شود:
 * <ol>
 *   <li><b>الگوریتم تقسیم</b> — آزمون خالص. مرزهای معنی‌دار، جفت جانشین،
 *       و پیشرفت تضمین‌شده.</li>
 *   <li><b>مسیر واقعی ارسال</b> — رو به سرور محلی، تا معلوم شود تقسیم
 *       واقعاً به چند درخواست تبدیل می‌شود و گزینه‌ها کجا اعمال می‌شوند.</li>
 * </ol>
 * لایه اول بدون دوم کافی نیست: یک تقسیم‌کننده درست که هیچ‌وقت صدا زده
 * نشود، مشکلی را حل نمی‌کند.
 */
class MessageSplitLongTextTest {

    // ─────────────────────────── الگوریتم ───────────────────────────

    @Nested
    @DisplayName("الگوریتم تقسیم")
    class Algorithm {

        @Test
        @DisplayName("متن کوتاه‌تر از سقف، یک بخش می‌ماند")
        void shortTextStaysWhole() {
            assertThat(EitaaTextSplitter.split("متن کوتاه", 100))
                    .containsExactly("متن کوتاه");
        }

        @Test
        @DisplayName("متن دقیقاً به اندازه سقف، تقسیم نمی‌شود")
        void exactlyAtLimitIsNotSplit() {
            // ⚠️ لبه دقیق. اگر شرط `<=` به `<` عوض شود، پیامی که
            // دقیقاً جا می‌شود بی‌دلیل دو تکه می‌شود.
            String text = "x".repeat(50);

            assertThat(EitaaTextSplitter.split(text, 50)).containsExactly(text);
        }

        @Test
        @DisplayName("هیچ بخشی از سقف بلندتر نیست")
        void noPartExceedsLimit() {
            String text = "الف ".repeat(500);

            List<String> parts = EitaaTextSplitter.split(text, 40);

            assertThat(parts).isNotEmpty();
            assertThat(parts).allSatisfy(part -> assertThat(part.length()).isLessThanOrEqualTo(40));
        }

        @Test
        @DisplayName("هیچ بخشی خالی نیست")
        void noEmptyParts() {
            // ⚠️ بخش خالی یعنی یک درخواست بی‌محتوا به ایتا — که
            // احتمالاً رد می‌شود و ممکن است کل ارسال را شکست دهد.
            String text = "خط اول\n\n\nخط دوم ".repeat(20);

            assertThat(EitaaTextSplitter.split(text, 25)).doesNotContain("");
        }

        @Test
        @DisplayName("هیچ کاراکتری گم نمی‌شود")
        void concatenationRestoresOriginal() {
            // 🔴 مهم‌ترین خاصیت: تقسیم باید بی‌اتلاف باشد. اگر یک
            // کاراکتر گم شود، متن تکلیف ناقص به معلم می‌رسد.
            String text = "این یک متن نسبتاً بلند فارسی است که باید تقسیم شود.\n"
                    + "و خط دومش هم طولانی است تا مطمئن شویم همه‌چیز سالم می‌ماند.";

            List<String> parts = EitaaTextSplitter.split(text, 20);

            assertThat(String.join("", parts)).isEqualTo(text);
        }

        @Test
        @DisplayName("برش ترجیحاً روی خط جدید می‌افتد")
        void prefersNewlineBoundary() {
            String text = "خط اول\nخط دوم";

            List<String> parts = EitaaTextSplitter.split(text, 8);

            assertThat(parts).hasSize(2);
            assertThat(parts.get(0))
                    .as("خط جدید باید به بخش بعد برود، نه به بخش فعلی")
                    .isEqualTo("خط اول");
        }

        @Test
        @DisplayName("اگر خط جدید نبود، برش روی فاصله می‌افتد")
        void fallsBackToSpaceBoundary() {
            // ⚠️ بریدن وسط کلمه در فارسی، خواندن را سخت می‌کند. با سقف
            // ۱۱ و متن چهارکلمه‌ای، نقطه برش باید روی فاصله بیفتد — پس
            // بخش اول یک یا چند کلمه کامل است، نه یک کلمه نیمه.
            List<String> parts = EitaaTextSplitter.split("کلمه کلمه کلمه کلمه", 11);

            assertThat(parts.get(0))
                    .as("باید روی مرز کلمه بریده باشد")
                    .isEqualTo("کلمه کلمه")
                    .doesNotEndWith(" ");
            assertThat(String.join("", parts)).isEqualTo("کلمه کلمه کلمه کلمه");
        }

        @Test
        @DisplayName("متن بدون هیچ فاصله‌ای هم تقسیم می‌شود")
        void splitsUnbrokenText() {
            // ⚠️ مثال واقعی: یک لینک بلند، یا یک رشته Base64. اگر
            // الگوریتم در نبود مرز معنی‌دار متوقف شود، حلقه بی‌نهایت
            // می‌شود.
            String text = "x".repeat(250);

            List<String> parts = EitaaTextSplitter.split(text, 100);

            assertThat(parts).hasSize(3);
            assertThat(String.join("", parts)).isEqualTo(text);
        }

        @Test
        @DisplayName("وسط ایموجی بریده نمی‌شود")
        void doesNotSplitSurrogatePair() {
            // 🔴 یک ایموجی دو واحد char می‌گیرد. بریدن وسطش یک
            // کاراکتر نامعتبر می‌سازد که در نمایش مربع خالی می‌شود.
            // این آزمون با تکرار یک ایموجی، هر نقطه برش ممکن را می‌آزماید.
            for (int limit = 2; limit <= 20; limit++) {
                String text = "😀".repeat(20);

                List<String> parts = EitaaTextSplitter.split(text, limit);

                assertThat(String.join("", parts))
                        .as("با سقف %d، متن باید سالم بازسازی شود", limit)
                        .isEqualTo(text);

                for (String part : parts) {
                    assertThat(Character.isLowSurrogate(part.charAt(0)))
                            .as("بخش نباید با نیمه دوم یک ایموجی شروع شود")
                            .isFalse();
                    assertThat(Character.isHighSurrogate(part.charAt(part.length() - 1)))
                            .as("بخش نباید با نیمه اول یک ایموجی تمام شود")
                            .isFalse();
                }
            }
        }

        @Test
        @DisplayName("سقف کوچک، حلقه را بی‌نهایت نمی‌کند")
        void tinyLimitStillTerminates() {
            // ⚠️ این همان حالتی است که یک پیاده‌سازی ساده را قفل می‌کند:
            // اگر نقطه برش هر بار به ابتدای بازه برگردد، هر بخش خالی
            // می‌شود و حلقه هیچ‌وقت تمام نمی‌شود.
            List<String> parts = EitaaTextSplitter.split("متن تست", 1);

            assertThat(parts).isNotEmpty();
            assertThat(String.join("", parts)).isEqualTo("متن تست");
        }

        @Test
        @DisplayName("سقف نامعتبر، متن را یک‌تکه برمی‌گرداند")
        void invalidLimitReturnsWholeText() {
            assertThat(EitaaTextSplitter.split("متن", 0)).containsExactly("متن");
            assertThat(EitaaTextSplitter.split("متن", -5)).containsExactly("متن");
        }

        @Test
        @DisplayName("ورودی خالی، فهرست خالی می‌دهد")
        void emptyInputGivesEmptyList() {
            assertThat(EitaaTextSplitter.split(null, 100)).isEmpty();
            assertThat(EitaaTextSplitter.split("", 100)).isEmpty();
        }
    }

    // ─────────────────────────── مسیر واقعی ارسال ───────────────────────────

    @Nested
    @DisplayName("ارسال واقعی")
    class RealSend {

        private EitaaStubServer server;

        @BeforeEach
        void setUp() throws IOException {
            server = EitaaStubServer.start();
        }

        @AfterEach
        void tearDown() {
            server.close();
        }

        private EitaaYarBotAdapter adapterWithLimit(int maxLength) {
            EitaaProperties strict =
                    EitaaTestData.withMaxMessageLength(server.properties(), maxLength);
            return EitaaRealStack.botAdapter(strict);
        }

        @Test
        @DisplayName("پیام بلند به چند درخواست تبدیل می‌شود")
        void longTextBecomesSeveralRequests() {
            // 🔴 اگر تقسیم وجود نداشت، ایتا پیام را رد می‌کرد و تکلیف
            // بی‌صدا به گروه نمی‌رسید.
            server.respondOkWith("{\"message_id\":\"1\"}");
            EitaaYarBotAdapter adapter = adapterWithLimit(30);
            String text = "این متن عمداً بلند است تا به چند بخش تقسیم شود.";

            EitaaSendResult result = adapter.sendToChat(null, "@group", text, null);

            assertThat(result.success()).isTrue();
            assertThat(server.requestCount())
                    .as("یک متن ۴۷ کاراکتری با سقف ۳۰ باید حداقل دو درخواست بدهد")
                    .isGreaterThan(1);
        }

        @Test
        @DisplayName("مجموع بخش‌های ارسالی، همان متن اصلی است")
        void sentPartsRebuildOriginalText() {
            server.respondOkWith("{\"message_id\":\"1\"}");
            EitaaYarBotAdapter adapter = adapterWithLimit(30);
            String text = "خط اول پیام\nخط دوم پیام که بلندتر است\nخط سوم";

            adapter.sendToChat(null, "@group", text, null);

            String joined = server.requests().stream()
                    .map(request -> extractTextField(request.body()))
                    .collect(Collectors.joining());

            assertThat(joined).isEqualTo(text);
        }

        @Test
        @DisplayName("هیچ بخش ارسالی از سقف بلندتر نیست")
        void noSentPartExceedsLimit() {
            server.respondOkWith("{\"message_id\":\"1\"}");
            EitaaYarBotAdapter adapter = adapterWithLimit(25);
            String text = "متن بسیار بلند برای آزمون تقسیم در ارسال واقعی به ایتا";

            adapter.sendToChat(null, "@group", text, null);

            assertThat(server.requests()).allSatisfy(request ->
                    assertThat(extractTextField(request.body()).length())
                            .isLessThanOrEqualTo(25));
        }

        @Test
        @DisplayName("گزینه‌ها فقط روی بخش اول اعمال می‌شوند")
        void optionsApplyOnlyToFirstPart() {
            // 🔴 این مهم‌ترین آزمون این بخش است. اگر گزینه‌ها روی همه
            // بخش‌ها اعمال شوند، یک تکلیف زمان‌بندی‌شده به سه پیام
            // زمان‌بندی‌شده تبدیل می‌شود — و «سنجاق» به سه پیام سنجاق‌شده
            // که هیچ‌کدام منظور نبوده.
            server.respondOkWith("{\"message_id\":\"1\"}");
            EitaaYarBotAdapter adapter = adapterWithLimit(20);
            EitaaSendOptions options =
                    new EitaaSendOptions(true, null, null, true, null);
            String text = "یک متن بلند که قطعاً به چند بخش تقسیم خواهد شد";

            adapter.sendToChat(null, "@group", text, options);

            assertThat(server.requestCount()).isGreaterThan(1);

            String first = server.requests().get(0).body();
            assertThat(first)
                    .as("بخش اول باید گزینه‌ها را داشته باشد")
                    .contains("disable_notification")
                    .contains("\"pin\":1");

            for (int i = 1; i < server.requests().size(); i++) {
                assertThat(server.requests().get(i).body())
                        .as("بخش %d نباید گزینه‌ها را تکرار کند", i + 1)
                        .doesNotContain("disable_notification")
                        .doesNotContain("\"pin\"");
            }
        }

        @Test
        @DisplayName("شناسه پیام برگشتی، شناسه بخش اول است")
        void returnsFirstPartMessageId() {
            server.respondOkWith("{\"message_id\":\"111\"}");
            EitaaYarBotAdapter adapter = adapterWithLimit(15);

            EitaaSendResult result =
                    adapter.sendToChat(null, "@group", "متن بلند برای تقسیم شدن", null);

            assertThat(result.success()).isTrue();
            assertThat(result.messageId()).isEqualTo("111");
        }

        @Test
        @DisplayName("شکست در بخش بعدی، کل ارسال را شکست‌خورده برمی‌گرداند")
        void failureInLaterPartFailsWholeSend() {
            // ⚠️ اگر بخش اول رفته باشد و بخش دوم شکست بخورد، بخش اول
            // <i>رفته</i> است. این را پنهان نمی‌کنیم — یک موفقیت ناقص،
            // خطرناک‌تر از یک شکست صریح است.
            // ⚠️ و از قرارداد مهم‌تر: شکست باید <b>مقدار</b> باشد، نه
            // استثنا. اگر استثنا بود، تراکنش انتشار تکلیف برمی‌گشت و
            // کار معلم از بین می‌رفت — در حالی که بخشی از پیام رفته بود.
            server.respondOkThenFail(1, "429", "too many requests");
            EitaaYarBotAdapter adapter = adapterWithLimit(10);

            EitaaSendResult result =
                    adapter.sendToChat(null, "@group", "متن بلند برای تقسیم", null);

            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo("429");
            assertThat(server.requestCount())
                    .as("باید بیش از یک درخواست رفته باشد تا آزمون حالت ناقص را بسنجد")
                    .isGreaterThan(1);
        }

        @Test
        @DisplayName("متن کوتاه، فقط یک درخواست می‌فرستد")
        void shortTextSendsSingleRequest() {
            // ⚠️ لنگر آزمون: بدون این، یک آداپتور که هر متن را به
            // بی‌نهایت تکه تقسیم می‌کرد هم آزمون‌های بالا را سبز می‌کرد.
            server.respondOkWith("{\"message_id\":\"1\"}");
            EitaaYarBotAdapter adapter = adapterWithLimit(4000);

            adapter.sendToChat(null, "@group", "متن کوتاه", null);

            assertThat(server.requestCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("مسیر ب هم پیام بلند را تقسیم می‌کند")
        void privatePathAlsoSplits() {
            // ⚠️ سقف طول، محدودیت خود سرویس ارسال است و به هر دو مسیر
            // یکسان اعمال می‌شود. ولی توجه: در مسیر ب هر بخش یک اعتبار
            // جدا از ۵۰٬۰۰۰ اعتبار رایگان مصرف می‌کند.
            server.respondWith(200, "{\"ok\":true,\"result\":\"success\"}");
            EitaaYarBotAdapter adapter = adapterWithLimit(20);

            EitaaSendResult result =
                    adapter.sendToUser(null, 555L, "یک متن بلند که تقسیم می‌شود");

            assertThat(result.success()).isTrue();
            assertThat(server.requestCount()).isGreaterThan(1);
        }

        /** استخراج مقدار فیلد {@code text} از بدنه JSON درخواست. */
        private static String extractTextField(String jsonBody) {
            String marker = "\"text\":\"";
            int start = jsonBody.indexOf(marker);
            if (start < 0) {
                return "";
            }
            int valueStart = start + marker.length();

            StringBuilder value = new StringBuilder();
            for (int i = valueStart; i < jsonBody.length(); i++) {
                char c = jsonBody.charAt(i);
                if (c == '\\' && i + 1 < jsonBody.length()) {
                    // ⚠️ کاراکترهای کنترلی مثل \n در JSON escape شده
                    // می‌آیند. برای بازسازی متن اصلی باید برگردانده شوند.
                    char escaped = jsonBody.charAt(++i);
                    value.append(escape(escaped));
                } else if (c == '"') {
                    break;
                } else {
                    value.append(c);
                }
            }
            return value.toString();
        }

        private static String escape(char escaped) {
            return switch (escaped) {
                case 'n' -> "\n";
                case 'r' -> "\r";
                case 't' -> "\t";
                default -> String.valueOf(escaped);
            };
        }
    }
}
