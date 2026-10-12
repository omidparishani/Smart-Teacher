package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;

import ir.omid.smartteacher.eitaa.internal.InMemoryReplayGuard;
import ir.omid.smartteacher.eitaa.internal.LocalHmacVerifier;
import ir.omid.smartteacher.eitaa.internal.api.dto.EitaaLoginRequest;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * شناسه کاربر ایتا باید ۵۲ بیتی باشد.
 *
 * <p><b>🔴 چرا این کلاس وجود دارد؟</b> سند رسمی ایتا می‌گوید شناسه کاربر
 * می‌تواند تا ۵۲ بیت اهمیت داشته باشد — بزرگ‌تر از {@code Integer}
 * (۳۱ بیت). اگر کسی این فیلد را {@code Integer} بگیرد، نه خطایی رخ
 * می‌دهد و نه هشداری: عدد <i>بی‌صدا</i> سرریز می‌شود و مقدار اشتباهی
 * ذخیره می‌شود. نتیجه این است که دو کاربر مختلف به یک حساب نگاشته
 * می‌شوند — یعنی یک نقص امنیتی جدی، با ظاهر یک باگ داده‌ای.
 *
 * <p>پس آزمون فقط مقدار را نمی‌سنجد؛ <b>نوع</b> را هم می‌سنجد. یک آزمون
 * مقداری که فقط با شناسه‌های کوچک کار کند، همین اشتباه را می‌پوشاند.
 */
class EitaaUserIdIsLongTest {

    /**
     * بزرگ‌ترین عدد ۵۲ بیتی ({@code 2^52 - 1}) — سقف اعلام‌شده سند.
     *
     * <p>بدون {@code L}، همین ثابت در Java به‌عنوان {@code int} تفسیر
     * می‌شد و کامپایل نمی‌شد — که خودش نشان می‌دهد {@code int} کافی
     * نیست.
     */
    private static final long MAX_52_BIT_ID = 4_503_599_627_370_495L;

    /** یک بالاتر از سقف {@code int} — کوچک‌ترین شناسه‌ای که int را می‌شکند. */
    private static final long FIRST_ID_BEYOND_INT = (long) Integer.MAX_VALUE + 1;

    private static EitaaInitDataValidator validator() {
        EitaaProperties properties = EitaaTestData.botProperties();
        return new EitaaInitDataValidator(
                properties,
                new LocalHmacVerifier(properties),
                new InMemoryReplayGuard(),
                EitaaTestData.mapper());
    }

    private static EitaaInitData validateForUser(long eitaaUserId) {
        return validator().validate(EitaaTestData.validInitData(eitaaUserId));
    }

    // ─────────────────────────── نوع ───────────────────────────

    @Test
    @DisplayName("نوع فیلد id در EitaaUserData دقیقاً Long است")
    void idComponentTypeIsLong() {
        // 🔴 آزمون ساختاری — همان چیزی که یک تغییر آینده به Integer را
        // می‌گیرد. سنجش مقدار به‌تنهایی این را نمی‌گیرد: کد با Integer
        // هم برای همه شناسه‌های آزمون (که کوچک‌اند) درست کار می‌کند.
        RecordComponent idComponent = Arrays.stream(EitaaUserData.class.getRecordComponents())
                .filter(component -> "id".equals(component.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("کامپوننت id در EitaaUserData پیدا نشد"));

        assertThat(idComponent.getType())
                .as("شناسه ایتا ۵۲ بیتی است؛ Integer بی‌صدا سرریز می‌کند")
                .isEqualTo(Long.class);
    }

    @Test
    @DisplayName("درخواست ورود هیچ فیلد شناسه‌ای از کاربر نمی‌پذیرد")
    void loginRequestAcceptsNoCallerSuppliedIdentity() {
        // 🔴 مکمل آزمون بالا. حتی اگر نوع داخلی Long باشد، کافی نیست:
        // اگر DTO ورودی فیلدی به نام شناسه داشت، مهاجم می‌توانست
        // شناسه دلخواه بفرستد و هویت خودش را اعلام کند. هویت باید
        // <i>فقط</i> از initData امضاشده بیاید، پس تنها ورودی مجاز
        // همان رشته initData است.
        RecordComponent[] components = EitaaLoginRequest.class.getRecordComponents();

        assertThat(components)
                .as("درخواست ورود باید دقیقاً یک ورودی داشته باشد: initData")
                .hasSize(1);
        assertThat(components[0].getName()).isEqualTo("initData");
        assertThat(components[0].getType()).isEqualTo(String.class);
    }

    // ─────────────────────────── مقدار ───────────────────────────

    @Test
    @DisplayName("شناسه ۵۲ بیتی بدون تغییر خوانده می‌شود")
    void readsLargeUserIdExactly() {
        EitaaInitData initData = validateForUser(MAX_52_BIT_ID);

        assertThat(initData.user().id())
                .as("بزرگ‌ترین شناسه مجاز باید دقیقاً بماند")
                .isEqualTo(MAX_52_BIT_ID);
    }

    @Test
    @DisplayName("شناسه بزرگ‌تر از سقف int سرریز نمی‌کند")
    void doesNotOverflowBeyondIntRange() {
        // ⚠️ اگر فیلد Integer بود، این مقدار به عددی منفی تبدیل می‌شد
        // و آزمون اینجا می‌افتاد — با پیامی که فوراً علت را نشان می‌دهد.
        EitaaInitData initData = validateForUser(FIRST_ID_BEYOND_INT);

        assertThat(initData.user().id())
                .as("سرریز int باید صریحاً گرفته شود، نه بی‌صدا")
                .isEqualTo(FIRST_ID_BEYOND_INT)
                .isPositive();
    }

    @Test
    @DisplayName("شناسه صفر و شناسه منفی هم پارس می‌شوند")
    void handlesBoundaryValues() {
        // ایتا شناسه صفر یا منفی نمی‌دهد، ولی پارس نباید آنها را به
        // استثنا تبدیل کند؛ تصمیم «مجاز است یا نه» جای دیگری است.
        assertThat(validateForUser(0L).user().id()).isZero();
        assertThat(validateForUser(-1L).user().id()).isEqualTo(-1L);
    }

    @Test
    @DisplayName("دو شناسه بزرگ، دو کاربر متفاوت می‌دهند")
    void largeIdsRemainDistinct() {
        // 🔴 آزمون نهایی سرریز: اگر هر دو به یک مقدار int تا بخورند،
        // دو کاربر واقعی روی یک حساب می‌افتند — یک نقص امنیتی، نه یک
        // باگ نمایشی.
        long first = MAX_52_BIT_ID;
        long second = MAX_52_BIT_ID - 4_294_967_296L; // اختلاف 2^32 — دقیقاً یک دور int کامل

        EitaaInitData firstUser = validateForUser(first);
        EitaaInitData secondUser = validateForUser(second);

        assertThat(firstUser.user().id()).isNotEqualTo(secondUser.user().id());
        assertThat(firstUser.user().id()).isEqualTo(first);
        assertThat(secondUser.user().id()).isEqualTo(second);
    }

    @Test
    @DisplayName("شناسه به‌صورت رشته عددی در JSON هم خوانده می‌شود")
    void readsIdGivenAsNumericString() {
        // ⚠️ برخی کلاینت‌ها عدد بزرگ را برای جلوگیری از سرریز در
        // JavaScript، به‌صورت رشته می‌فرستند. Jackson 3 این را به Long
        // تبدیل می‌کند، پس نباید ورود را بشکند.
        Map<String, String> params = EitaaTestData.baseParams(1L);
        params.put("user", EitaaTestData.encode(
                "{\"id\":\"" + MAX_52_BIT_ID + "\",\"first_name\":\"علی\"}"));

        EitaaInitData initData = validator().validate(EitaaTestData.signed(params));

        assertThat(initData.user().id()).isEqualTo(MAX_52_BIT_ID);
    }
}
