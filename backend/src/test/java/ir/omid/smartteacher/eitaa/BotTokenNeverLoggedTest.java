package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ir.omid.smartteacher.eitaa.internal.EitaaYarBotAdapter;
import ir.omid.smartteacher.eitaa.internal.InMemoryReplayGuard;
import ir.omid.smartteacher.eitaa.internal.LocalHmacVerifier;
import ir.omid.smartteacher.eitaa.internal.MockEitaaAdapter;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * توکن ربات و توکن برنامک هرگز در Log نمی‌آیند.
 *
 * <p><b>🔴 چرا این کلاس لازم است، در حالی که همه {@code toString}ها
 * دستی ماسک شده‌اند؟</b> چون ماسک‌کردن {@code toString} فقط یکی از
 * راه‌های نشت را می‌بندد. توکن می‌تواند از راه‌های دیگری بیرون برود:
 * <ul>
 *   <li>URL — توکن بخشی از مسیر است: {@code /api/{TOKEN}/sendMessage}.</li>
 *   <li>پیام خطای ایتا، که ممکن است درخواست را بازتاب دهد.</li>
 *   <li>یک رکورد که کسی فراموش کرده ماسک کند.</li>
 *   <li>یک Exception که {@code getMessage()} راز را حمل می‌کند.</li>
 * </ul>
 * پس این آزمون <b>رفتار</b> را می‌سنجد، نه یک متد خاص: مسیرهای واقعی را
 * صدا می‌زند، همه لاگ‌ها را می‌گیرد، و در <i>هیچ‌کدام</i> دنبال توکن
 * می‌گردد. این همان چیزی است که قاعده «هیچ API Key در Log نرود» می‌خواهد.
 *
 * <p><b>⚠️ چرا URL واقعی استفاده نمی‌شود؟</b> چون این آزمون نباید به
 * {@code eitaayar.ir} درخواست بفرستد. مسیرهای HTTP رو به یک سرور محلی
 * می‌روند و همان کد واقعی را اجرا می‌کنند — URL با توکن ساخته می‌شود،
 * فقط به جای دیگری اشاره می‌کند. اگر جایی آن URL لاگ شود، آزمون می‌افتد.
 */
class BotTokenNeverLoggedTest {

    /** کلید امضای مشتق‌شده — خودش هم راز است و نباید در Log بیاید. */
    private static final String DERIVED_SECRET =
            LocalHmacVerifier.deriveSecretKeyHex(EitaaTestData.BOT_TOKEN);

    /**
     * خط نشانه برای اثبات زنده‌بودن خط لوله ضبط لاگ.
     *
     * <p>⚠️ هیچ کاراکتر ASCII ندارد تا محال باشد ناخواسته بخشی از یک
     * توکن را بسازد و آزمون نشت را به‌غلط بیندازد.
     */
    private static final String CAPTURE_CANARY = "نشانه-زنده-بودن-ضبط-لاگ";

    private ListAppender<ILoggingEvent> appender;
    private Logger applicationLogger;
    private Level previousLevel;

    @BeforeEach
    void attachAppender() {
        appender = new ListAppender<>();
        appender.start();

        // ⚠️ سطح TRACE عمدی است: اگر توکن در یک پیام سطح پایین لاگ
        // شود، با INFO از دستمان می‌رفت و آزمون بی‌دلیل سبز می‌ماند.
        applicationLogger = (Logger) LoggerFactory.getLogger("ir.omid.smartteacher");
        previousLevel = applicationLogger.getLevel();
        applicationLogger.setLevel(Level.TRACE);

        // ⚠️ روی ریشه بسته می‌شود، نه روی logger خودمان: هر کتابخانه
        // واسط هم باید پوشش داده شود.
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).detachAppender(appender);
        applicationLogger.setLevel(previousLevel);
        appender.stop();
    }

    // ─────────────────────────── گردآوری لاگ ───────────────────────────

    private List<String> capturedLines() {
        return appender.list.stream()
                .map(event -> event.getFormattedMessage()
                        + " | " + (event.getThrowableProxy() == null
                                ? ""
                                : event.getThrowableProxy().getMessage()))
                .toList();
    }

    private void assertNothingLeaked() {
        List<String> lines = capturedLines();

        assertThat(lines)
                .as("آزمون باید لاگ تولید کرده باشد، وگرنه «چیزی پیدا نشد» بی‌معنا است")
                .isNotEmpty();

        assertThat(lines)
                .as("توکن ربات هرگز نباید در Log بیاید")
                .noneMatch(line -> line.contains(EitaaTestData.BOT_TOKEN));
        assertThat(lines)
                .as("توکن برنامک هرگز نباید در Log بیاید")
                .noneMatch(line -> line.contains(EitaaTestData.APP_TOKEN));
        assertThat(lines)
                .as("کلید مشتق‌شده از توکن هم راز است")
                .noneMatch(line -> line.contains(DERIVED_SECRET));
    }

    /**
     * ثابت می‌کند خط لوله ضبط لاگ واقعاً کار می‌کند.
     *
     * <p><b>🔴 چرا لازم است؟</b> مسیرهای <i>موفق</i> هیچ لاگی تولید
     * نمی‌کنند — که خودش خبر خوبی است، ولی آزمون را در معرض یک نتیجه
     * کاذب می‌گذارد: اگر اپندر جدا شده باشد، سطح لاگ عوض شده باشد، یا
     * نام logger جور نباشد، فهرست لاگ <i>خالی</i> می‌ماند و «توکن پیدا
     * نشد» بی‌آنکه چیزی سنجیده شده باشد سبز می‌شود. آن‌وقت یک نشت
     * واقعی هم دیده نمی‌شد.
     *
     * <p>پس پیش از مسیرهای موفق، یک خط نشانه از همان مسیر لاگ می‌فرستیم
     * و حضورش را می‌سنجیم. با این، خالی‌بودن لاگِ آداپتور یک
     * <b>مشاهده</b> است، نه یک نقاب: اگر آداپتور توکن را لاگ کرده بود،
     * همین خط لوله آن را می‌گرفت.
     *
     * <p>⚠️ نشانه از خود آداپتور فرستاده نمی‌شود، چون آن‌وقت آزمون
     * درباره خودش بود نه درباره کد.
     */
    private void proveCapturePipelineIsLive() {
        LoggerFactory.getLogger("ir.omid.smartteacher.canary").info(CAPTURE_CANARY);

        assertThat(capturedLines())
                .as("خط لوله ضبط لاگ کار نمی‌کند؛ هر نتیجه‌ای از این آزمون بی‌اعتبار است")
                .anyMatch(line -> line.contains(CAPTURE_CANARY));
    }

    // ─────────────────────────── نمای‌ها ───────────────────────────

    @Test
    @DisplayName("toString پیکربندی، توکن‌ها را ماسک می‌کند")
    void propertiesToStringMasksTokens() {
        String rendered = EitaaTestData.fullProperties().toString();

        assertThat(rendered)
                .doesNotContain(EitaaTestData.BOT_TOKEN)
                .doesNotContain(EitaaTestData.APP_TOKEN)
                .contains("***");
    }

    @Test
    @DisplayName("toString نوع توکن، مقدار را پنهان می‌کند")
    void tokenTypesHideValue() {
        assertThat(new EitaaBotToken(EitaaTestData.BOT_TOKEN).toString())
                .doesNotContain(EitaaTestData.BOT_TOKEN)
                .contains("REDACTED");

        assertThat(new EitaaAppToken(EitaaTestData.APP_TOKEN).toString())
                .doesNotContain(EitaaTestData.APP_TOKEN)
                .contains("REDACTED");
    }

    @Test
    @DisplayName("توکن خالی، پیام خطایش مقدار را حمل نمی‌کند")
    void tokenRejectsBlankWithoutEchoing() {
        // ⚠️ پیام استثنا هم یک مسیر نشت است: اگر پیام خودِ مقدار را
        // بازتاب دهد، وارد Log می‌شود. اینجا بررسی می‌شود که پیام فقط
        // بگوید «خالی است» — نه اینکه چه چیزی داده شد.
        assertThatThrownBy(() -> new EitaaBotToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("null")
                .hasMessageContaining("توکن");

        assertThatThrownBy(() -> new EitaaAppToken(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("برنامک");
    }

    // ─────────────────────────── شبیه‌ساز ───────────────────────────

    @Test
    @DisplayName("همه عملیات شبیه‌ساز، توکن را لو نمی‌دهند")
    void mockOperationsLeakNothing() {
        MockEitaaAdapter mock = new MockEitaaAdapter(EitaaTestCredits.noop());
        EitaaFilePayload file = new EitaaFilePayload("t.pdf", "application/pdf", new byte[]{1});
        EitaaBotToken token = new EitaaBotToken(EitaaTestData.BOT_TOKEN);

        mock.getBotInfo(token);
        mock.sendToChat(token, "@group", "متن", EitaaSendOptions.defaults());
        mock.sendFileToChat(token, "@group", file, "توضیح", null);
        mock.sendToUser(null, 555L, "متن");
        mock.notifyUser(555L, EitaaNotification.of("اعلان"));
        mock.checkHealth(token);
        mock.failNextSends(1, "ERR", "خطا");
        mock.sendToChat(token, "@group", "متن", null);

        assertNothingLeaked();
    }

    // ─────────────────────────── آداپتور واقعی ───────────────────────────

    @Test
    @DisplayName("مسیرهای موفق واقعی، توکن را لو نمی‌دهند")
    void successfulRealPathsLeakNothing() throws IOException {
        // 🔴 مسیرهای موفق هیچ لاگی تولید نمی‌کنند، پس این آزمون تنها
        // با «چیزی پیدا نشد» سبز می‌شد — و همان جمله می‌تواند معنای
        // «خط لوله ضبط خراب است» را بدهد. پیش از هر کاری، زنده‌بودن
        // ضبط را ثابت می‌کنیم.
        proveCapturePipelineIsLive();

        try (EitaaStubServer server = EitaaStubServer.start()) {
            server.respondOkWith("{\"message_id\":\"1\"}");
            EitaaYarBotAdapter adapter = EitaaRealStack.botAdapter(server.properties());

            adapter.getBotInfo(null);
            adapter.sendToChat(null, "@group", "متن", EitaaSendOptions.defaults());
            adapter.sendFileToChat(null, "@group",
                    new EitaaFilePayload("t.pdf", "application/pdf", new byte[]{1}), null, null);
            adapter.checkHealth(null);

            // ⚠️ لنگر دوم: مسیرها واقعاً اجرا شده باشند. بدون این،
            // آداپتوری که هیچ کاری نمی‌کند هم آزمون را سبز می‌کرد.
            assertThat(server.requestCount())
                    .as("مسیرهای موفق باید واقعاً به ایتا درخواست بدهند")
                    .isEqualTo(4);
        }

        assertNothingLeaked();
    }

    @Test
    @DisplayName("مسیرهای خطای واقعی، توکن را در URL یا پیام لو نمی‌دهند")
    void failingRealPathsLeakNothing() throws IOException {
        // 🔴 این مهم‌ترین آزمون کلاس است. مسیرهای خطا بیشترین
        // احتمال نشت را دارند: کسی که می‌خواهد دیباگ کند، وسوسه
        // می‌شود URL را لاگ کند — و URL توکن را حمل می‌کند.
        try (EitaaStubServer server = EitaaStubServer.start()) {
            EitaaYarBotAdapter adapter = EitaaRealStack.botAdapter(server.properties());

            server.respondNotOk("403", "bot was blocked by the user");
            adapter.sendToChat(null, "@group", "متن", null);

            server.respondWith(500, "{\"ok\":false}");
            adapter.sendToChat(null, "@group", "متن", null);

            server.respondWith(401, "");
            adapter.sendToChat(null, "@group", "متن", null);

            server.respondWith(200, "<html>not json</html>");
            adapter.sendToChat(null, "@group", "متن", null);

            server.respondWith(200, "");
            adapter.sendFileToChat(null, "@group",
                    new EitaaFilePayload("t.pdf", "application/pdf", new byte[]{1}), null, null);

            // مسیر ب هم جزو همین قاعده است.
            adapter.sendToUser(null, 555L, "متن");
            adapter.checkHealth(null);
        }

        assertNothingLeaked();
    }

    @Test
    @DisplayName("پیام استثنای ایتا، توکن را حمل نمی‌کند")
    void apiExceptionMessageCarriesNoToken() throws IOException {
        // ⚠️ Exceptionها جای دیگری هستند که راز می‌تواند از آن بیرون
        // بزند: بدنه پاسخ، درخواست، یا URL را در پیام می‌گذارند.
        try (EitaaStubServer server = EitaaStubServer.start()) {
            server.respondNotOk("403", "blocked: token " + EitaaTestData.BOT_TOKEN);

            // ایتا در توصیف خطا خودش توکن را بازتاب می‌دهد — سناریوی
            // واقعی و بدخیم. پیام ما نباید آن را تکرار کند.
            EitaaYarBotAdapter adapter = EitaaRealStack.botAdapter(server.properties());
            EitaaSendResult result = adapter.sendToChat(null, "@group", "متن", null);

            assertThat(result.errorMessage())
                    .as("توصیف خطای ایتا نباید به کاربر یا Log برود")
                    .doesNotContain(EitaaTestData.BOT_TOKEN);
        }

        assertNothingLeaked();
    }

    @Test
    @DisplayName("اعتبارسنجی initData، توکن یا کلید مشتق‌شده را لو نمی‌دهد")
    void initDataValidationLeaksNothing() {
        // ⚠️ کلید مشتق‌شده در همین مسیر ساخته می‌شود و دقیقاً همان
        // چیزی است که با آن می‌شود امضا جعل کرد. مسیر موفق و ناموفق
        // هر دو سنجیده می‌شوند.
        EitaaProperties properties = EitaaTestData.botProperties();
        EitaaInitDataValidator validator = new EitaaInitDataValidator(
                properties,
                new LocalHmacVerifier(properties),
                new InMemoryReplayGuard(),
                EitaaTestData.mapper());

        validator.validate(EitaaTestData.validInitData(1L));

        try {
            validator.validate("hash=" + "0".repeat(64) + "&auth_date=1&user={}");
        } catch (RuntimeException expected) {
            // رد شدن مهم نیست؛ چیزی که مهم است این است که در Log
            // چیزی نشت نکرده.
        }

        assertNothingLeaked();
    }

    // ─────────────────────────── جعبه نامه ───────────────────────────

    @Test
    @DisplayName("پیام‌های شبیه‌ساز، توکن را نگه نمی‌دارند")
    void outboxDoesNotRetainToken() {
        // ⚠️ اگر SentMessage توکن را نگه می‌داشت، هر dump حافظه یا
        // گزارش خطای بعدی آن را بیرون می‌کشید — بدون آنکه هیچ لاگی
        // نوشته شده باشد. به همین دلیل توکن اصلاً ذخیره نمی‌شود.
        MockEitaaAdapter mock = new MockEitaaAdapter(EitaaTestCredits.noop());
        mock.sendToChat(new EitaaBotToken(EitaaTestData.BOT_TOKEN), "@group", "متن", null);

        assertThat(mock.lastMessage().toString()).doesNotContain(EitaaTestData.BOT_TOKEN);
        assertThat(mock.outbox())
                .allSatisfy(message -> assertThat(message.toString())
                        .doesNotContain(EitaaTestData.BOT_TOKEN));
    }
}
