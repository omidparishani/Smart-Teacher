package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.eitaa.internal.MockEitaaAdapter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * «قابلیت نیست» باید صریح گفته شود، نه شبیه‌سازی.
 *
 * <p><b>🔴 مسئله‌ای که این کلاس می‌بندد:</b> وقتی توکن برنامک تنظیم نشده،
 * ارسال پیام خصوصی واقعاً ممکن نیست. دو راه پیش پای کد بود:
 * <ol>
 *   <li>بی‌صدا کاری کند که کار نمی‌کند — یک {@code success=true} دروغ،
 *       یا یک درخواست با توکن خالی که خطای مبهم می‌گیرد.</li>
 *   <li>صریح بگوید ممکن نیست.</li>
 * </ol>
 * راه اول باگ را به Production می‌برد. این کلاس ثابت می‌کند راه دوم
 * انتخاب شده — و روی <b>آداپتور واقعی</b> سنجیده می‌شود، نه شبیه‌ساز،
 * چون مشکل دقیقاً در پیکربندی واقعی رخ می‌دهد.
 *
 * <p>کد وضعیت ۵۰۱ است، نه ۵۰۰: این خرابی ما نیست، ایتا آن قابلیت را
 * ندارد یا توکنش را نداریم.
 */
class CapabilityNotSupportedTest {

    // ─────────────────────────── آداپتور واقعی ───────────────────────────

    @Test
    @DisplayName("آداپتور با توکن ربات، فقط قابلیت‌های مسیر الف را اعلام می‌کند")
    void botTokenOnlyAnnouncesBotCapabilities() {
        EitaaAdapter adapter = EitaaRealStack.botAdapter(EitaaTestData.botProperties());

        assertThat(adapter.capabilities())
                .as("با توکن ربات، مسیر الف کامل است")
                .contains(EitaaCapability.GET_BOT_INFO,
                        EitaaCapability.SEND_MESSAGE,
                        EitaaCapability.SEND_FILE,
                        EitaaCapability.SEND_TO_GROUP,
                        EitaaCapability.SEND_TO_CHANNEL,
                        EitaaCapability.SCHEDULED_SEND,
                        EitaaCapability.PIN_SENT_MESSAGE,
                        EitaaCapability.AUTO_DELETE_ON_VIEW)
                .as("ولی بدون توکن برنامه، پیام خصوصی ممکن نیست")
                .doesNotContain(EitaaCapability.SEND_TO_USER);
    }

    @Test
    @DisplayName("آداپتور با هر دو توکن، پیام خصوصی را هم اعلام می‌کند")
    void bothTokensAnnouncePrivatePath() {
        EitaaAdapter adapter = EitaaRealStack.botAdapter(EitaaTestData.fullProperties());

        assertThat(adapter.capabilities()).contains(EitaaCapability.SEND_TO_USER);
    }

    @Test
    @DisplayName("آداپتور بی‌توکن، هیچ قابلیتی اعلام نمی‌کند")
    void noTokensAnnounceNothing() {
        // 🔴 این مهم‌ترین بخش «اعلام بر پایه پیکربندی» است: اگر آداپتور
        // بی‌توکن چیزی اعلام می‌کرد، فراخوانی که به capabilities اعتماد
        // کرده بود، در زمان اجرا می‌شکست.
        EitaaAdapter adapter = EitaaRealStack.botAdapter(EitaaTestData.emptyProperties());

        assertThat(adapter.capabilities()).isEmpty();
    }

    @Test
    @DisplayName("اعلان به کاربر بدون توکن برنامه، صریح رد می‌شود")
    void notifyUserWithoutAppTokenFails() {
        EitaaAdapter adapter = EitaaRealStack.botAdapter(EitaaTestData.botProperties());

        assertThatThrownBy(() -> adapter.notifyUser(123L, EitaaNotification.of("متن")))
                .as("اعلان شخصی فقط از مسیر ب می‌رود؛ مسیر الف به کاربر خصوصی پیام نمی‌دهد")
                .isInstanceOf(EitaaCapabilityNotSupportedException.class)
                .satisfies(thrown -> assertThat(
                        ((EitaaCapabilityNotSupportedException) thrown).requestedCapability())
                        .isEqualTo(EitaaCapability.SEND_TO_USER));
    }

    @Test
    @DisplayName("ارسال خصوصی بدون توکن برنامه، صریح رد می‌شود")
    void sendToUserWithoutAppTokenFails() {
        EitaaAdapter adapter = EitaaRealStack.botAdapter(EitaaTestData.botProperties());

        assertThatThrownBy(() -> adapter.sendToUser(null, 123L, "متن"))
                .isInstanceOf(EitaaCapabilityNotSupportedException.class)
                .satisfies(thrown -> assertThat(
                        ((EitaaCapabilityNotSupportedException) thrown).requestedCapability())
                        .isEqualTo(EitaaCapability.SEND_TO_USER));
    }

    @Test
    @DisplayName("ارسال به گروه بدون هیچ توکنی، قابلیت پیام را نام می‌برد")
    void sendToChatWithoutAnyTokenNamesMessageCapability() {
        EitaaAdapter adapter = EitaaRealStack.botAdapter(EitaaTestData.emptyProperties());

        assertThatThrownBy(() -> adapter.sendToChat(null, "@group", "متن", null))
                .isInstanceOf(EitaaCapabilityNotSupportedException.class)
                .satisfies(thrown -> assertThat(
                        ((EitaaCapabilityNotSupportedException) thrown).requestedCapability())
                        .isEqualTo(EitaaCapability.SEND_MESSAGE));
    }

    @Test
    @DisplayName("ارسال فایل بدون هیچ توکنی، قابلیت فایل را نام می‌برد")
    void sendFileWithoutAnyTokenNamesFileCapability() {
        EitaaAdapter adapter = EitaaRealStack.botAdapter(EitaaTestData.emptyProperties());
        EitaaFilePayload file = new EitaaFilePayload("t.mp3", "audio/mpeg", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> adapter.sendFileToChat(null, "@group", file, "توضیح", null))
                .isInstanceOf(EitaaCapabilityNotSupportedException.class)
                .satisfies(thrown -> assertThat(
                        ((EitaaCapabilityNotSupportedException) thrown).requestedCapability())
                        .isEqualTo(EitaaCapability.SEND_FILE));
    }

    @Test
    @DisplayName("اطلاعات ربات بدون هیچ توکنی، رد می‌شود")
    void getBotInfoWithoutAnyTokenFails() {
        EitaaAdapter adapter = EitaaRealStack.botAdapter(EitaaTestData.emptyProperties());

        assertThatThrownBy(() -> adapter.getBotInfo(null))
                .isInstanceOf(EitaaCapabilityNotSupportedException.class)
                .satisfies(thrown -> assertThat(
                        ((EitaaCapabilityNotSupportedException) thrown).requestedCapability())
                        .isEqualTo(EitaaCapability.GET_BOT_INFO));
    }

    // ─────────────────────────── نگاشت خطا ───────────────────────────

    @Test
    @DisplayName("خطای قابلیت، کد ۵۰۱ می‌دهد نه ۵۰۰")
    void mapsToNotImplemented() {
        // ⚠️ تفاوت ۵۰۱ و ۵۰۰ در عمل مهم است: ۵۰۰ در داشبورد یعنی «ما
        // خرابیم» و صفحه هشدار می‌سازد؛ ۵۰۱ یعنی «این کار با این
        // پیکربندی ممکن نیست» و باید راهنمای پیکربندی بدهد.
        EitaaCapabilityNotSupportedException failure =
                new EitaaCapabilityNotSupportedException(EitaaCapability.SEND_TO_USER);

        assertThat(failure.getErrorCode().httpStatus()).isEqualTo(501);
    }

    @Test
    @DisplayName("پیام خطا نام قابلیت را می‌گوید ولی هیچ توکنی را لو نمی‌دهد")
    void messageNamesCapabilityWithoutLeakingSecrets() {
        EitaaCapabilityNotSupportedException failure =
                new EitaaCapabilityNotSupportedException(EitaaCapability.SEND_FILE);

        assertThat(failure.getMessage()).contains("SEND_FILE");
        assertThat(failure.getMessage())
                .as("پیام خطا یک متن عمومی است، نه جای راز")
                .doesNotContain(EitaaTestData.BOT_TOKEN)
                .doesNotContain(EitaaTestData.APP_TOKEN);
    }

    @Test
    @DisplayName("قابلیت نامعلوم هم پیام قابل‌فهم می‌دهد")
    void handlesNullCapability() {
        // ⚠️ این حالت نباید NullPointerException بدهد: مسیر خطا که خودش
        // خطا بدهد، تشخیص را غیرممکن می‌کند.
        EitaaCapabilityNotSupportedException failure =
                new EitaaCapabilityNotSupportedException(null);

        assertThat(failure.getMessage()).isNotBlank();
        assertThat(failure.getErrorCode().httpStatus()).isEqualTo(501);
    }

    // ─────────────────────────── ساختار Enum ───────────────────────────

    @Test
    @DisplayName("قابلیت‌های ناموجود ایتا در Enum نیستند")
    void enumContainsNoImpossibleCapabilities() {
        // 🔴 این آزمون از یک باور غلط جلوگیری می‌کند. اگر ثابتی مثل
        // RECEIVE_MESSAGE یا WEBHOOK اینجا بود، کسی می‌توانست کدی بر
        // پایه‌اش بنویسد و منتظر بماند تا در Production بفهمد ایتا
        // getUpdates ندارد. نبودنشان، آن کد را غیرقابل‌نوشتن می‌کند.
        Set<String> names = Arrays.stream(EitaaCapability.values())
                .map(capability -> capability.name())
                .collect(Collectors.toSet());

        assertThat(names)
                .as("هیچ‌کدام از این‌ها در API ایتا وجود ندارد")
                .doesNotContain("RECEIVE_MESSAGE", "WEBHOOK", "LONG_POLLING",
                        "EDIT_MESSAGE", "DELETE_MESSAGE", "SEND_POLL",
                        "GET_MEMBERS", "GET_ADMINS", "INLINE_KEYBOARD");
    }

    @Test
    @DisplayName("فهرست قابلیت‌ها فقط همان نُه مورد مستندشده است")
    void enumHasExactlyDocumentedCapabilities() {
        // ⚠️ سقف صریح: اگر کسی قابلیتی اضافه کرد، این آزمون می‌افتد و
        // باید ثابت کند آن قابلیت در سند رسمی هست.
        assertThat(EitaaCapability.values()).hasSize(9);
    }

    // ─────────────────────────── تفاوت با شبیه‌ساز ───────────────────────────

    @Test
    @DisplayName("شبیه‌ساز عمداً همه قابلیت‌ها را اعلام می‌کند")
    void mockAnnouncesEverythingByDesign() {
        // ⚠️ این تفاوت را باید دید، نه اینکه باگ فرض شود. شبیه‌ساز برای
        // این است که توسعه‌دهنده بدون توکن همه مسیرها را بیازماید؛
        // آداپتور واقعی فقط چیزی را می‌گوید که واقعاً می‌تواند.
        assertThat(new MockEitaaAdapter(EitaaTestCredits.noop()).capabilities())
                .containsExactlyInAnyOrder(EitaaCapability.values());
    }
}
