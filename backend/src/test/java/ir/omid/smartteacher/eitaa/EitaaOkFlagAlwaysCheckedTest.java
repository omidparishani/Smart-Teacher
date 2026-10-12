package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;

import ir.omid.smartteacher.eitaa.internal.EitaaYarBotAdapter;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * قاعده طلایی سند رسمی: همیشه ابتدا {@code ok} را بررسی کن.
 *
 * <p><b>🔴 حالتی که این کلاس برای گرفتنش نوشته شده:</b> ایتا خطا را با
 * <b>کد وضعیت HTTP ۲۰۰</b> و {@code ok:false} برمی‌گرداند. هر کدی که
 * فقط به کد وضعیت نگاه کند، این پاسخ را «موفق» می‌بیند — و معلم فکر
 * می‌کند تکلیف به گروه رسیده، در حالی که نرسیده. این بدترین نوع شکست
 * است: بی‌صدا.
 *
 * <p>آزمون روی <b>آداپتور واقعی رو به یک سرور محلی</b> انجام می‌شود، نه
 * شبیه‌ساز — چون شبیه‌ساز هیچ پاسخی پارس نمی‌کند و این مسیر را لمس
 * نمی‌کند. آزمون روی شبیه‌ساز سبز می‌شد بدون آنکه چیزی را ثابت کند.
 *
 * <p>هر آزمون منفی با یک آزمون مثبت جفت شده: بدون آن، یک آداپتور که
 * <i>همیشه</i> شکست برمی‌گرداند هم همه آزمون‌ها را سبز می‌کرد.
 */
class EitaaOkFlagAlwaysCheckedTest {

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

    // ─────────────────────────── پایه ───────────────────────────

    @Test
    @DisplayName("پاسخ ok:true واقعاً موفق است")
    void okTrueIsSuccess() {
        // ⚠️ لنگر آزمون: بدون این، همه آزمون‌های منفی پایین با یک
        // آداپتور همیشه‌شکست‌خورده هم سبز می‌شدند.
        server.respondOkWith("{\"message_id\":\"85917\"}");

        EitaaSendResult result =
                adapter.sendToChat(null, CHAT_ID, "تکلیف", EitaaSendOptions.defaults());

        assertThat(result.success()).isTrue();
        assertThat(result.messageId()).isEqualTo("85917");
        assertThat(result.errorCode()).isNull();
    }

    // ─────────────────────────── ok:false در HTTP ۲۰۰ ───────────────────────────

    @Test
    @DisplayName("ok:false در HTTP ۲۰۰ موفق تلقی نمی‌شود")
    void okFalseAtHttp200IsFailure() {
        // 🔴 آزمون اصلی این کلاس.
        server.respondNotOk("403", "bot was blocked by the user");

        EitaaSendResult result =
                adapter.sendToChat(null, CHAT_ID, "تکلیف", EitaaSendOptions.defaults());

        assertThat(result.success())
                .as("کد وضعیت ۲۰۰ بدون بررسی ok، این شکست را پنهان می‌کرد")
                .isFalse();
        assertThat(result.messageId()).isNull();
        assertThat(result.sentAt()).isNull();
    }

    @Test
    @DisplayName("کد خطای ایتا در نتیجه حمل می‌شود")
    void carriesRemoteErrorCode() {
        server.respondNotOk("403", "bot was blocked by the user");

        EitaaSendResult result = adapter.sendToChat(null, CHAT_ID, "تکلیف", null);

        assertThat(result.errorCode()).isEqualTo("403");
    }

    @Test
    @DisplayName("ok:false بدون error_code هم شکست است")
    void okFalseWithoutErrorCodeIsStillFailure() {
        // ⚠️ شکل پاسخ خطا در سند رسمی تضمین نشده. اگر کد فقط به وجود
        // error_code نگاه کند، این پاسخ «موفق» می‌شود.
        server.respondWith(200, "{\"ok\":false}");

        EitaaSendResult result = adapter.sendToChat(null, CHAT_ID, "تکلیف", null);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode())
                .as("کد خطا نباید null بماند — مصرف‌کننده روی آن تصمیم می‌گیرد")
                .isNotNull()
                .isNotBlank();
    }

    @Test
    @DisplayName("پاسخ بدون فیلد ok، موفق تلقی نمی‌شود")
    void missingOkIsFailure() {
        // ⚠️ نبودن ok یعنی ناشناخته، و ناشناخته باید به سمت «نرسید»
        // چرخ بخورد، نه «رسید».
        server.respondWith(200, "{\"result\":{\"message_id\":\"1\"}}");

        assertThat(adapter.sendToChat(null, CHAT_ID, "تکلیف", null).success()).isFalse();
    }

    @Test
    @DisplayName("بدنه خالی، شکست است نه موفقیت")
    void emptyBodyIsFailure() {
        server.respondWith(200, "");

        EitaaSendResult result = adapter.sendToChat(null, CHAT_ID, "تکلیف", null);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isNotBlank();
    }

    @Test
    @DisplayName("پاسخ غیر-JSON، شکست است")
    void nonJsonBodyIsFailure() {
        // ⚠️ این حالت واقعی است: وقتی پروکسی یا صفحه خطا جواب می‌دهد،
        // یا ایتا HTML برمی‌گرداند. هیچ‌کدام «موفق» نیست.
        server.respondWith(200, "<html><body>502 Bad Gateway</body></html>");

        assertThat(adapter.sendToChat(null, CHAT_ID, "تکلیف", null).success()).isFalse();
    }

    @Test
    @DisplayName("بدنه ناقص JSON، شکست است")
    void truncatedJsonIsFailure() {
        server.respondWith(200, "{\"ok\":tr");

        assertThat(adapter.sendToChat(null, CHAT_ID, "تکلیف", null).success()).isFalse();
    }

    // ─────────────────────────── کدهای وضعیت ───────────────────────────

    @Test
    @DisplayName("کد وضعیت ۵۰۰، شکست است")
    void http500IsFailure() {
        server.respondWith(500, "{\"ok\":true}");

        EitaaSendResult result = adapter.sendToChat(null, CHAT_ID, "تکلیف", null);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("HTTP_500");
    }

    @Test
    @DisplayName("کد وضعیت ۴۰۱، شکست است و کدش در نتیجه می‌آید")
    void http401IsFailure() {
        // ⚠️ توکن نامعتبر — رایج‌ترین خرابی پیکربندی. باید در نتیجه
        // دیده شود، نه فقط در Log.
        server.respondWith(401, "{\"ok\":false}");

        assertThat(adapter.sendToChat(null, CHAT_ID, "تکلیف", null).errorCode())
                .isEqualTo("HTTP_401");
    }

    @Test
    @DisplayName("کد وضعیت ۴۲۹، شکست است")
    void http429IsFailure() {
        server.respondWith(429, "{\"ok\":false}");

        assertThat(adapter.sendToChat(null, CHAT_ID, "تکلیف", null).success()).isFalse();
    }

    @Test
    @DisplayName("۵۰۰ دوباره تلاش نمی‌شود وقتی سقف تلاش یک است")
    void doesNotRetryBeyondAttemptLimit() {
        // ⚠️ پیکربندی آزمون maxSendAttempts=1 دارد. سنجیدن تعداد
        // درخواست‌ها تضمین می‌کند سقف واقعاً اعمال می‌شود — وگرنه
        // آزمون روی خطای شبکه کند و پرهزینه می‌شد.
        server.respondWith(500, "{\"ok\":false}");

        adapter.sendToChat(null, CHAT_ID, "تکلیف", null);

        assertThat(server.requestCount()).isEqualTo(1);
    }

    // ─────────────────────────── مسیر فایل ───────────────────────────

    @Test
    @DisplayName("ok:false در ارسال فایل هم شکست است")
    void okFalseOnFileSendIsFailure() {
        server.respondNotOk("400", "file too big");
        EitaaFilePayload file =
                new EitaaFilePayload("t.docx", "application/vnd.openxmlformats", new byte[]{1, 2, 3});

        EitaaSendResult result =
                adapter.sendFileToChat(null, CHAT_ID, file, "توضیح", null);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("400");
    }

    @Test
    @DisplayName("ارسال موفق فایل، شناسه پیام می‌دهد")
    void successfulFileSendReturnsMessageId() {
        server.respondOkWith("{\"message_id\":\"42\"}");
        EitaaFilePayload file = new EitaaFilePayload("t.pdf", "application/pdf", new byte[]{9});

        EitaaSendResult result = adapter.sendFileToChat(null, CHAT_ID, file, null, null);

        assertThat(result.success()).isTrue();
        assertThat(result.messageId()).isEqualTo("42");
    }

    @Test
    @DisplayName("فایل با multipart و فیلد file فرستاده می‌شود")
    void fileGoesAsMultipart() {
        // ⚠️ سند رسمی: فایل باید multipart/form-data باشد و نام فیلدش
        // `file`. اگر کسی این را به JSON تغییر دهد، آزمون می‌افتد.
        server.respondOkWith("{\"message_id\":\"1\"}");
        EitaaFilePayload file =
                new EitaaFilePayload("lesson.pdf", "application/pdf", "محتوا".getBytes());

        adapter.sendFileToChat(null, CHAT_ID, file, "توضیح درس", null);

        EitaaStubServer.Received request = server.lastRequest();
        assertThat(request.contentType()).startsWith("multipart/form-data");
        assertThat(request.body())
                .contains("name=\"file\"")
                .contains("filename=\"lesson.pdf\"")
                .contains("name=\"chat_id\"")
                .as("در ارسال فایل، متن همراه caption نام دارد نه text")
                .contains("name=\"caption\"")
                .contains("توضیح درس");
    }

    // ─────────────────────────── مسیر ب ───────────────────────────

    @Test
    @DisplayName("ok:false در مسیر ب هم شکست است")
    void okFalseOnPrivatePathIsFailure() {
        server.respondNotOk("403", "user did not allow messages");

        EitaaSendResult result = adapter.sendToUser(null, 555L, "متن");

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("403");
    }

    @Test
    @DisplayName("ارسال موفق مسیر ب، شکست نمی‌خورد ولی شناسه هم ندارد")
    void successfulPrivateSendHasNoMessageId() {
        // ⚠️ پاسخ واقعی این مسیر رشته "success" است، نه شیئی با
        // message_id. شناسه‌ای وجود ندارد که برگردانیم.
        server.respondWith(200, "{\"ok\":true,\"result\":\"success\"}");

        EitaaSendResult result = adapter.sendToUser(null, 555L, "متن");

        assertThat(result.success()).isTrue();
        assertThat(result.messageId()).isNull();
    }

    @Test
    @DisplayName("مسیر ب شناسه کاربر را عدد می‌فرستد، نه رشته")
    void privatePathSendsUserIdAsNumber() {
        // ⚠️ سند می‌گوید شناسه تا ۵۲ بیت اهمیت دارد؛ یعنی در JSON عدد
        // است. رشته‌فرستادنش ممکن است سمت ایتا نوع اشتباه تلقی شود.
        server.respondWith(200, "{\"ok\":true,\"result\":\"success\"}");

        adapter.sendToUser(null, 4_503_599_627_370_495L, "متن");

        assertThat(server.lastRequest().body())
                .contains("\"chat_id\":4503599627370495")
                .doesNotContain("\"chat_id\":\"");
    }

    @Test
    @DisplayName("مسیر ب به Endpoint درست می‌رود")
    void privatePathUsesAppEndpoint() {
        server.respondWith(200, "{\"ok\":true,\"result\":\"success\"}");

        adapter.sendToUser(null, 555L, "متن");

        assertThat(server.lastRequest().path()).isEqualTo("/api/app/sendMessage");
    }
}
