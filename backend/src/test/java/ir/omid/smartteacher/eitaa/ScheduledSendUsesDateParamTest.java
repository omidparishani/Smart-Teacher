package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;

import ir.omid.smartteacher.eitaa.internal.EitaaYarBotAdapter;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * نگاشت گزینه‌های مستندشده به پارامترهای واقعی درخواست.
 *
 * <p><b>⚠️ چرا این آزمون روی سرور محلی است و نه یک آزمون خالص؟</b> چون
 * چیزی که باید سنجیده شود، <i>محتوای درخواست خروجی</i> است — و آن فقط
 * وقتی دیده می‌شود که یک درخواست واقعی جایی ثبت شود. آزمون خالص فقط
 * می‌توانست بگوید «رکورد EitaaSendOptions این مقدارها را دارد»، که
 * چیزی درباره رشته JSON ارسالی نمی‌گوید.
 *
 * <p><b>⚠️ درباره پارامتر {@code date}:</b> سند رسمی ایتا می‌گوید
 * {@code date} زمان‌بندی بومی است، ولی <i>منطقه زمانی‌اش را نمی‌گوید</i>.
 * اینجا {@code epochSecond} در UTC فرض شده. آزمون همین فرض فعلی را
 * تثبیت می‌کند تا اگر روزی با آزمون واقعی معلوم شد که منطقه زمانی دیگری
 * است، تغییرش آگاهانه باشد — نه یک تغییر بی‌صدا که کسی نفهمد.
 * این ابهام در {@code docs/eitaa.md} بخش ۱۴ به‌عنوان مورد باز ثبت شده.
 */
class ScheduledSendUsesDateParamTest {

    private static final String CHAT_ID = "@teaching_group";

    private EitaaStubServer server;
    private EitaaYarBotAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        server = EitaaStubServer.start();
        adapter = EitaaRealStack.botAdapter(server.properties());
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private String lastBody() {
        return server.lastRequest().body();
    }

    // ─────────────────────────── زمان‌بندی ───────────────────────────

    @Test
    @DisplayName("زمان‌بندی به پارامتر date با epoch ثانیه نگاشت می‌شود")
    void scheduledAtMapsToDateParam() {
        Instant when = Instant.parse("2026-10-01T07:30:00Z");
        server.respondOkWith("{\"message_id\":\"1\"}");

        adapter.sendToChat(null, CHAT_ID, "تکلیف", EitaaSendOptions.scheduledAt(when));

        assertThat(lastBody())
                .contains("\"date\":" + when.getEpochSecond())
                .doesNotContain("\"date\":\"");
    }

    @Test
    @DisplayName("ارسال بدون زمان‌بندی، پارامتر date ندارد")
    void noDateParamWhenNotScheduled() {
        // 🔴 فرستادن date در ارسال عادی یعنی «این را در آن زمان
        // بفرست» — یک پیام بی‌موقع. نبودن فیلد، امن‌تر از مقدار پیش‌فرض
        // بودنش است.
        server.respondOkWith("{\"message_id\":\"1\"}");

        adapter.sendToChat(null, CHAT_ID, "تکلیف", EitaaSendOptions.defaults());

        assertThat(lastBody()).doesNotContain("\"date\"");
    }

    @Test
    @DisplayName("زمان‌بندی در گذشته هم همان‌طور نگاشت می‌شود")
    void pastInstantMapsVerbatim() {
        // ⚠️ تصمیم «آیا زمان گذشته مجاز است؟» کار این لایه نیست. اینجا
        // فقط نگاشت سنجیده می‌شود؛ رد کردن، وظیفه لایه کاری است.
        Instant past = Instant.parse("2020-01-01T00:00:00Z");
        server.respondOkWith("{\"message_id\":\"1\"}");

        adapter.sendToChat(null, CHAT_ID, "متن", EitaaSendOptions.scheduledAt(past));

        assertThat(lastBody()).contains("\"date\":" + past.getEpochSecond());
    }

    // ─────────────────────────── پرچم‌های بولی ───────────────────────────

    @Test
    @DisplayName("ارسال بی‌صدا با مقدار ۱ می‌رود، نه true")
    void disableNotificationUsesNumericOne() {
        // 🔴 سند رسمی برای پرچم‌های بولی «۱» می‌گوید، نه true. اگر کسی
        // این را به true عوض کند، آزمون می‌افتد — چون در بعضی
        // پیاده‌سازی‌ها true پذیرفته نمی‌شود و بی‌صدا نادیده می‌ماند.
        server.respondOkWith("{\"message_id\":\"1\"}");
        EitaaSendOptions options = new EitaaSendOptions(true, null, null, false, null);

        adapter.sendToChat(null, CHAT_ID, "متن", options);

        assertThat(lastBody())
                .contains("\"disable_notification\":1")
                .doesNotContain("true");
    }

    @Test
    @DisplayName("سنجاق‌کردن با مقدار ۱ می‌رود")
    void pinUsesNumericOne() {
        server.respondOkWith("{\"message_id\":\"1\"}");
        EitaaSendOptions options = new EitaaSendOptions(false, null, null, true, null);

        adapter.sendToChat(null, CHAT_ID, "متن", options);

        assertThat(lastBody()).contains("\"pin\":1");
    }

    @Test
    @DisplayName("ارسال بی‌صدا نشده، پارامتر disable_notification ندارد")
    void absentBooleanFlagsAreOmitted() {
        // ⚠️ فرستادن `disable_notification: 0` بی‌ضرر به نظر می‌رسد ولی
        // یعنی «صریحاً خاموش کن». اگر ایتا پیش‌فرضش را عوض کند، ما
        // مطلع نمی‌شویم. نبودن فیلد، صادقانه‌تر است.
        server.respondOkWith("{\"message_id\":\"1\"}");

        adapter.sendToChat(null, CHAT_ID, "متن", EitaaSendOptions.defaults());

        assertThat(lastBody())
                .doesNotContain("disable_notification")
                .doesNotContain("\"pin\"");
    }

    // ─────────────────────────── حذف خودکار ───────────────────────────

    @Test
    @DisplayName("حذف خودکار با نام دقیق viewCountForDelete می‌رود")
    void autoDeleteUsesExactParamName() {
        // ⚠️ این نام camelCase است و با بقیه پارامترهای snake_case
        // فرق دارد. یک اشتباه تایپی اینجا یعنی قابلیت بی‌صدا کار نکند.
        server.respondOkWith("{\"message_id\":\"1\"}");
        EitaaSendOptions options = new EitaaSendOptions(false, null, null, false, 250);

        adapter.sendToChat(null, CHAT_ID, "متن", options);

        assertThat(lastBody())
                .contains("\"viewCountForDelete\":250")
                .doesNotContain("view_count_for_delete");
    }

    @Test
    @DisplayName("مقدار صفر یا منفی حذف خودکار، فرستاده نمی‌شود")
    void nonPositiveAutoDeleteIsOmitted() {
        // ⚠️ «حذف پس از صفر بازدید» یعنی حذف فوری — رفتار خطرناکی که
        // هیچ‌کس نمی‌خواهد. صفر باید به «خاموش» تفسیر شود، نه «صفر بار».
        server.respondOkWith("{\"message_id\":\"1\"}");

        adapter.sendToChat(null, CHAT_ID, "متن",
                new EitaaSendOptions(false, null, null, false, 0));

        assertThat(lastBody()).doesNotContain("viewCountForDelete");
    }

    // ─────────────────────────── پاسخ به پیام ───────────────────────────

    @Test
    @DisplayName("شناسه عددی پاسخ، به‌صورت عدد می‌رود")
    void numericReplyIdGoesAsNumber() {
        server.respondOkWith("{\"message_id\":\"1\"}");
        EitaaSendOptions options = new EitaaSendOptions(false, "85917", null, false, null);

        adapter.sendToChat(null, CHAT_ID, "متن", options);

        assertThat(lastBody()).contains("\"reply_to_message_id\":85917");
    }

    @Test
    @DisplayName("شناسه غیرعددی پاسخ، حذف نمی‌شود")
    void nonNumericReplyIdIsNotDropped() {
        // 🔴 یک شناسه با قالب نامنتظر را بی‌صدا دور انداختن بدتر از
        // فرستادنش به‌صورت رشته است: کاربر هیچ سرنخی نمی‌گیرد که چرا
        // «پاسخ به پیام» کار نکرد.
        server.respondOkWith("{\"message_id\":\"1\"}");
        EitaaSendOptions options = new EitaaSendOptions(false, "msg-abc", null, false, null);

        adapter.sendToChat(null, CHAT_ID, "متن", options);

        assertThat(lastBody()).contains("\"reply_to_message_id\":\"msg-abc\"");
    }

    @Test
    @DisplayName("شناسه خالی پاسخ، فرستاده نمی‌شود")
    void blankReplyIdIsOmitted() {
        server.respondOkWith("{\"message_id\":\"1\"}");

        adapter.sendToChat(null, CHAT_ID, "متن",
                new EitaaSendOptions(false, "   ", null, false, null));

        assertThat(lastBody()).doesNotContain("reply_to_message_id");
    }

    // ─────────────────────────── گزینه‌ها در مسیر فایل ───────────────────────────

    @Test
    @DisplayName("گزینه‌ها در ارسال فایل هم اعمال می‌شوند")
    void optionsApplyToFileSend() {
        // ⚠️ مسیر فایل multipart است و همه مقادیرش رشته. اگر نگاشت
        // گزینه‌ها فقط برای JSON نوشته شده بود، اینجا بی‌صدا کار نمی‌کرد.
        server.respondOkWith("{\"message_id\":\"1\"}");
        Instant when = Instant.parse("2026-10-01T07:30:00Z");
        EitaaSendOptions options = new EitaaSendOptions(true, null, when, false, 100);
        EitaaFilePayload file = new EitaaFilePayload("t.pdf", "application/pdf", new byte[]{7});

        adapter.sendFileToChat(null, CHAT_ID, file, null, options);

        assertThat(lastBody())
                .contains("name=\"disable_notification\"")
                .contains("name=\"date\"")
                .contains(String.valueOf(when.getEpochSecond()))
                .contains("name=\"viewCountForDelete\"")
                .contains("100");
    }

    // ─────────────────────────── ترتیب کلیدها ───────────────────────────

    @Test
    @DisplayName("مقصد و متن همیشه در بدنه هستند")
    void destinationAndTextAlwaysPresent() {
        server.respondOkWith("{\"message_id\":\"1\"}");

        adapter.sendToChat(null, CHAT_ID, "متن درس", EitaaSendOptions.defaults());

        assertThat(lastBody())
                .contains("\"chat_id\":\"" + CHAT_ID + "\"")
                .contains("\"text\":\"متن درس\"");
    }
}
