package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.eitaa.internal.MockEitaaAdapter;
import ir.omid.smartteacher.eitaa.internal.MockEitaaAdapter.Kind;
import ir.omid.smartteacher.eitaa.internal.MockEitaaAdapter.SentMessage;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * مسیر الف روی شبیه‌ساز: ارسال متن به گروه یا کانال.
 *
 * <p><b>⚠️ چرا آزمون روی شبیه‌ساز؟</b> چون این کلاس درباره «آیا پیام
 * درست ساخته و به آداپتور سپرده شد؟» است، نه «آیا HTTP درست کار
 * می‌کند؟». آن دومی جای دیگری سنجیده می‌شود، رو به یک سرور واقعی.
 * شبیه‌ساز اینجا نقش یک ضبط‌کننده را دارد: هرچه به آن برسد، عیناً ثبت
 * می‌شود و می‌شود درباره‌اش حکم داد.
 *
 * <p>هر آزمون از یک نمونه تازه شروع می‌کند تا جعبه نامه از آزمون قبلی
 * آلوده نباشد — وابستگی بین آزمون‌ها، شکستی می‌سازد که فقط در ترتیب
 * خاصی رخ می‌دهد.
 */
class MockSendToChatTest {

    private static final String CHAT_ID = "@my_teaching_group";

    private MockEitaaAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MockEitaaAdapter(EitaaTestCredits.noop());
    }

    // ─────────────────────────── ثبت پیام ───────────────────────────

    @Test
    @DisplayName("متن به گروه ثبت می‌شود")
    void recordsTextMessage() {
        EitaaSendResult result =
                adapter.sendToChat(null, CHAT_ID, "تکلیف فردا", EitaaSendOptions.defaults());

        assertThat(result.success()).isTrue();
        assertThat(adapter.outbox()).hasSize(1);

        SentMessage sent = adapter.lastMessage();
        assertThat(sent.kind()).isEqualTo(Kind.TEXT);
        assertThat(sent.target()).isEqualTo(CHAT_ID);
        assertThat(sent.text()).isEqualTo("تکلیف فردا");
    }

    @Test
    @DisplayName("گزینه null به پیش‌فرض تبدیل می‌شود، نه خطا")
    void nullOptionsBecomeDefaults() {
        // ⚠️ فراخوانی که گزینه ندارد، باید کار کند. اگر null به خطا
        // تبدیل می‌شد، هر محل فراخوانی مجبور بود EitaaSendOptions.defaults()
        // را تکرار کند — و یکی از آن‌ها فراموش می‌کرد.
        adapter.sendToChat(null, CHAT_ID, "متن", null);

        assertThat(adapter.lastMessage().options()).isEqualTo(EitaaSendOptions.defaults());
    }

    @Test
    @DisplayName("گزینه‌های ارسال، بی‌تغییر نگه داشته می‌شوند")
    void keepsOptionsIntact() {
        EitaaSendOptions options = new EitaaSendOptions(true, "85917", null, true, 100);

        adapter.sendToChat(null, CHAT_ID, "متن", options);

        assertThat(adapter.lastMessage().options()).isEqualTo(options);
    }

    @Test
    @DisplayName("هر ارسال شناسه پیام یکتا می‌گیرد")
    void assignsUniqueMessageIds() {
        adapter.sendToChat(null, CHAT_ID, "یک", null);
        adapter.sendToChat(null, CHAT_ID, "دو", null);
        adapter.sendToChat(null, CHAT_ID, "سه", null);

        assertThat(adapter.outbox())
                .extracting(SentMessage::messageId)
                .doesNotHaveDuplicates()
                .doesNotContainNull();
    }

    @Test
    @DisplayName("ترتیب ارسال‌ها حفظ می‌شود")
    void preservesOrder() {
        adapter.sendToChat(null, CHAT_ID, "اول", null);
        adapter.sendToChat(null, CHAT_ID, "دوم", null);

        assertThat(adapter.outbox())
                .extracting(SentMessage::text)
                .containsExactly("اول", "دوم");
    }

    @Test
    @DisplayName("ارسال موفق، زمان ارسال دارد")
    void successfulSendHasTimestamp() {
        adapter.sendToChat(null, CHAT_ID, "متن", null);

        assertThat(adapter.lastMessage().sentAt()).isNotNull();
    }

    // ─────────────────────────── جعبه نامه ───────────────────────────

    @Test
    @DisplayName("جعبه نامه فقط خواندنی است")
    void outboxIsImmutable() {
        adapter.sendToChat(null, CHAT_ID, "متن", null);

        assertThatThrownBy(() -> adapter.outbox().clear())
                .as("دست‌کاری جعبه نامه باید ناممکن باشد، وگرنه آزمون‌ها می‌توانند همدیگر را خراب کنند")
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("خالی‌کردن جعبه نامه، فقط تاریخچه را پاک می‌کند")
    void clearOutboxResetsHistory() {
        adapter.sendToChat(null, CHAT_ID, "متن", null);
        adapter.clearOutbox();

        assertThat(adapter.outbox()).isEmpty();
        assertThat(adapter.lastMessage()).isNull();
    }

    // ─────────────────────────── تزریق خطا ───────────────────────────

    @Test
    @DisplayName("شکست ارسال، نتیجه ناموفق می‌دهد — نه استثنا")
    void failureReturnsResultNotException() {
        // 🔴 قاعده اصلی سند: شکست ارسال نباید استثنا باشد. اگر بود،
        // تراکنش انتشار تکلیف برمی‌گشت و کار معلم از بین می‌رفت.
        adapter.failNextSends(1, "EITAA_DOWN", "ایتا پاسخ نداد");

        EitaaSendResult result =
                adapter.sendToChat(null, CHAT_ID, "متن", EitaaSendOptions.defaults());

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("EITAA_DOWN");
        assertThat(result.errorMessage()).isEqualTo("ایتا پاسخ نداد");
        assertThat(result.messageId()).isNull();
        assertThat(result.sentAt()).isNull();
    }

    @Test
    @DisplayName("ارسال ناموفق در جعبه نامه ثبت نمی‌شود")
    void failedSendIsNotRecorded() {
        // ⚠️ اگر شکست هم ثبت می‌شد، آزمون‌های بعدی روی پیامی سبز
        // می‌شدند که هرگز نرفته است.
        adapter.failNextSends(1, "ERR", "خطا");

        adapter.sendToChat(null, CHAT_ID, "متن", null);

        assertThat(adapter.outbox()).isEmpty();
    }

    @Test
    @DisplayName("تزریق خطا دقیقاً به تعداد اعلام‌شده اثر دارد")
    void failureInjectionIsCounted() {
        adapter.failNextSends(2, "ERR", "خطا");

        assertThat(adapter.sendToChat(null, CHAT_ID, "یک", null).success()).isFalse();
        assertThat(adapter.sendToChat(null, CHAT_ID, "دو", null).success()).isFalse();
        assertThat(adapter.sendToChat(null, CHAT_ID, "سه", null).success())
                .as("پس از دو شکست، ارسال باید عادی شود")
                .isTrue();

        assertThat(adapter.outbox()).hasSize(1);
    }

    @Test
    @DisplayName("برداشتن حالت خطا، ارسال را عادی می‌کند")
    void resetFailuresRestoresSending() {
        adapter.failNextSends(5, "ERR", "خطا");
        adapter.resetFailures();

        assertThat(adapter.sendToChat(null, CHAT_ID, "متن", null).success()).isTrue();
    }

    @Test
    @DisplayName("کد خطای خالی، مقدار پیش‌فرض می‌گیرد")
    void nullErrorCodeFallsBack() {
        // ⚠️ کد خطای null در نتیجه، مصرف‌کننده را مجبور می‌کند خودش
        // برای «کد نامعلوم» تصمیم بگیرد. مقدار پیش‌فرض، آن تصمیم را
        // یک‌جا می‌گیرد.
        adapter.failNextSends(1, null, null);

        EitaaSendResult result = adapter.sendToChat(null, CHAT_ID, "متن", null);

        assertThat(result.errorCode()).isNotBlank();
        assertThat(result.errorMessage()).isNotBlank();
    }

    @Test
    @DisplayName("تعداد منفی تزریق خطا، بی‌اثر است")
    void negativeFailureCountIsIgnored() {
        adapter.failNextSends(-3, "ERR", "خطا");

        assertThat(adapter.sendToChat(null, CHAT_ID, "متن", null).success()).isTrue();
    }

    // ─────────────────────────── سلامت و اطلاعات ───────────────────────────

    @Test
    @DisplayName("اطلاعات ربات شبیه‌سازی‌شده برگردانده می‌شود")
    void returnsBotInfo() {
        EitaaBotInfo info = adapter.getBotInfo(null);

        assertThat(info.username()).isEqualTo("mock_eitaa_bot");
        assertThat(info.id()).isNotNull();
    }

    @Test
    @DisplayName("بررسی سلامت شبیه‌ساز همیشه سالم است")
    void healthIsUp() {
        EitaaHealth health = adapter.checkHealth(null);

        assertThat(health.healthy()).isTrue();
        assertThat(health.checkedAt()).isNotNull().isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("شبیه‌ساز همه قابلیت‌ها را اعلام می‌کند")
    void announcesAllCapabilities() {
        // ⚠️ تفاوت عمدی با آداپتور واقعی: شبیه‌ساز باید همه مسیرها را
        // بدون توکن قابل آزمودن کند. آداپتور واقعی فقط چیزی را اعلام
        // می‌کند که توکنش موجود است.
        assertThat(adapter.capabilities())
                .containsExactlyInAnyOrder(EitaaCapability.values());
    }

    @Test
    @DisplayName("toString پیام، متن را لو نمی‌دهد")
    void sentMessageToStringHidesText() {
        // ⚠️ متن پیام ممکن است نام دانش‌آموز یا نمره او باشد. اگر در
        // Log بیفتد، یک نشت داده است — بدون آنکه کسی متوجه شود.
        String sensitive = "نمره علی احمدی: ۱۲";

        adapter.sendToChat(null, CHAT_ID, sensitive, null);

        assertThat(adapter.lastMessage().toString()).doesNotContain(sensitive);
    }
}
