package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.eitaa.internal.MockEitaaAdapter;
import ir.omid.smartteacher.eitaa.internal.MockEitaaAdapter.Kind;
import ir.omid.smartteacher.eitaa.internal.MockEitaaAdapter.SentMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * مسیر ب روی شبیه‌ساز: پیام خصوصی به کاربر.
 *
 * <p><b>🔴 مهم‌ترین آزمون این کلاس، آزمون «شناسه پیام null است»
 * است.</b> مسیر ب در API واقعی ایتا {@code message_id} برنمی‌گرداند —
 * پاسخش فقط رشته {@code "success"} است. اگر شبیه‌ساز اینجا یک شناسه
 * <i>می‌ساخت</i>، همه آزمون‌ها روی چیزی سبز می‌شدند که در Production
 * وجود ندارد، و باگ در فاز ۹ (ثبت {@code message_log}) کشف می‌شد —
 * یعنی زمانی که کد روی آن شناسه ساختار ساخته است.
 *
 * <p>این همان قانون کلی است: شبیه‌ساز باید <i>وفادار</i> باشد، نه
 * مهربان. مهربانی‌اش اینجا یعنی دروغ.
 */
class MockSendToUserTest {

    private static final long EITAA_USER_ID = 4_503_599_627_370_495L;

    private MockEitaaAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new MockEitaaAdapter(EitaaTestCredits.noop());
    }

    // ─────────────────────────── ثبت پیام خصوصی ───────────────────────────

    @Test
    @DisplayName("پیام خصوصی با نوع PRIVATE ثبت می‌شود")
    void recordsPrivateMessage() {
        EitaaSendResult result =
                adapter.sendToUser(null, EITAA_USER_ID, "تکلیف شما تصحیح شد");

        assertThat(result.success()).isTrue();

        SentMessage sent = adapter.lastMessage();
        assertThat(sent.kind()).isEqualTo(Kind.PRIVATE);
        assertThat(sent.target()).isEqualTo(String.valueOf(EITAA_USER_ID));
        assertThat(sent.text()).isEqualTo("تکلیف شما تصحیح شد");
    }

    @Test
    @DisplayName("شناسه پیام در مسیر ب null است — وفادار به API واقعی")
    void messageIdIsNullForPrivatePath() {
        // 🔴 سند رسمی: پاسخ مسیر ب رشته "success" است، نه شیئی با
        // message_id. پس شناسه‌ای وجود ندارد. ساختنش در شبیه‌ساز یعنی
        // آزمودن چیزی که هرگز رخ نمی‌دهد.
        EitaaSendResult result = adapter.sendToUser(null, EITAA_USER_ID, "متن");

        assertThat(result.success()).isTrue();
        assertThat(result.messageId()).isNull();
        assertThat(adapter.lastMessage().messageId()).isNull();
    }

    @Test
    @DisplayName("شناسه ۵۲ بیتی بدون افت دقت حمل می‌شود")
    void carriesLargeUserIdExactly() {
        adapter.sendToUser(null, EITAA_USER_ID, "متن");

        assertThat(adapter.lastMessage().target())
                .isEqualTo("4503599627370495");
    }

    @Test
    @DisplayName("شناسه خالی، خطای روشن می‌دهد")
    void rejectsNullUserId() {
        // ⚠️ این یکی برخلاف شکست ارسال، واقعاً استثناست: فرستادن
        // پیام به «هیچ‌کس» یک خطای برنامه‌نویسی است، نه یک خرابی موقت.
        assertThatThrownBy(() -> adapter.sendToUser(null, null, "متن"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("شناسه کاربر");
    }

    // ─────────────────────────── اعلان ───────────────────────────

    @Test
    @DisplayName("اعلان، متنش را به پیام خصوصی تبدیل می‌کند")
    void notificationBecomesPrivateMessage() {
        adapter.notifyUser(EITAA_USER_ID, EitaaNotification.of("آزمون فردا ساعت ۱۰"));

        SentMessage sent = adapter.lastMessage();
        assertThat(sent.kind()).isEqualTo(Kind.PRIVATE);
        assertThat(sent.text()).isEqualTo("آزمون فردا ساعت ۱۰");
    }

    @Test
    @DisplayName("اعلان بدون گزینه، پیش‌فرض می‌گیرد")
    void notificationWithoutOptionsUsesDefaults() {
        adapter.notifyUser(EITAA_USER_ID, EitaaNotification.of("متن"));

        assertThat(adapter.lastMessage().options()).isEqualTo(EitaaSendOptions.defaults());
    }

    @Test
    @DisplayName("اعلان خالی رد می‌شود")
    void rejectsNullNotification() {
        assertThatThrownBy(() -> adapter.notifyUser(EITAA_USER_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("اعلان");
    }

    @Test
    @DisplayName("ساخت اعلان با متن خالی ناممکن است")
    void notificationRejectsBlankText() {
        // ⚠️ این بررسی در سازنده رکورد است، نه در notifyUser. یعنی
        // نمی‌شود یک اعلان بی‌متن <i>ساخت</i> — پس هیچ مسیری به
        // ارسال پیام خالی نمی‌رسد.
        assertThatThrownBy(() -> EitaaNotification.of(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EitaaNotification("   ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("اعلان ناموفق هم مثل ارسال معمولی برمی‌گردد")
    void notificationHonoursFailureInjection() {
        adapter.failNextSends(1, "QUOTA", "اعتبار تمام شد");

        EitaaSendResult result =
                adapter.notifyUser(EITAA_USER_ID, EitaaNotification.of("متن"));

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("QUOTA");
        assertThat(adapter.outbox()).isEmpty();
    }

    // ─────────────────────────── نشت داده ───────────────────────────

    @Test
    @DisplayName("toString اعلان، متن را لو نمی‌دهد")
    void notificationToStringHidesText() {
        // ⚠️ متن اعلان ممکن است نام یا نمره دانش‌آموز باشد. در Log فقط
        // طولش می‌آید.
        String sensitive = "کارنامه زهرا محمدی";
        EitaaNotification notification = EitaaNotification.of(sensitive);

        assertThat(notification.toString())
                .doesNotContain(sensitive)
                .contains(String.valueOf(sensitive.length()));
    }

    @Test
    @DisplayName("toString نتیجه، متن پیام را حمل نمی‌کند")
    void resultToStringCarriesNoText() {
        adapter.sendToUser(null, EITAA_USER_ID, "متن محرمانه");

        assertThat(adapter.lastMessage().toString()).doesNotContain("متن محرمانه");
    }

    // ─────────────────────────── تفکیک دو مسیر ───────────────────────────

    @Test
    @DisplayName("پیام خصوصی و گروهی در جعبه نامه قابل تفکیک‌اند")
    void privateAndGroupMessagesAreDistinguishable() {
        // ⚠️ بدون فیلد kind، آزمونی که هر دو مسیر را همزمان می‌سنجد
        // نمی‌توانست بگوید کدام پیام از کدام مسیر رفته — و یک جابه‌جایی
        // اشتباه بین دو مسیر دیده نمی‌شد.
        adapter.sendToChat(null, "@group", "به گروه", null);
        adapter.sendToUser(null, EITAA_USER_ID, "به کاربر");

        assertThat(adapter.outbox())
                .extracting(SentMessage::kind)
                .containsExactly(Kind.TEXT, Kind.PRIVATE);
    }
}
