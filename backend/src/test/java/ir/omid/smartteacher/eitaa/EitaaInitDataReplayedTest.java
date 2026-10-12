package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.common.error.EitaaInitDataReplayedException;
import ir.omid.smartteacher.eitaa.internal.InMemoryReplayGuard;
import ir.omid.smartteacher.eitaa.internal.LocalHmacVerifier;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * نگه‌بان ضدبازپخش.
 *
 * <p><b>⚠️ چرا پنجره تازگی کافی نیست؟</b> چون {@code initData} تا پنج
 * دقیقه معتبر است. در آن پنجره، کسی که یک بار آن را دیده باشد (از Log
 * پروکسی، از حافظه دستگاه، از تاریخچه مرورگر) می‌تواند بی‌نهایت نشست از
 * آن بسازد. ضدبازپخش همان پنجره را به «دقیقاً یک بار» تبدیل می‌کند.
 *
 * <p>مهم‌ترین آزمون این کلاس، آزمون سوم است — و دلیلی دارد که در ادامه
 * توضیح داده شده.
 */
class EitaaInitDataReplayedTest {

    private static final long USER_ID = 777_888_999L;

    private static EitaaInitDataValidator validator() {
        EitaaProperties properties = EitaaTestData.botProperties();
        return new EitaaInitDataValidator(
                properties,
                new LocalHmacVerifier(properties),
                new InMemoryReplayGuard(),
                EitaaTestData.mapper());
    }

    private static EitaaInitDataValidator validator(ReplayGuard guard) {
        EitaaProperties properties = EitaaTestData.botProperties();
        return new EitaaInitDataValidator(
                properties, new LocalHmacVerifier(properties), guard, EitaaTestData.mapper());
    }

    // ─────────────────────────── بازپخش ───────────────────────────

    @Test
    @DisplayName("همان initData بار دوم رد می‌شود")
    void rejectsSecondUseOfSameInitData() {
        EitaaInitDataValidator validator = validator();
        String initData = EitaaTestData.validInitData(USER_ID);

        assertThatCode(() -> validator.validate(initData))
                .as("استفاده اول باید بگذرد")
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> validator.validate(initData))
                .as("همان رشته دقیقاً بار دوم نباید بگذرد")
                .isInstanceOf(EitaaInitDataReplayedException.class);
    }

    @Test
    @DisplayName("سه بار پشت‌سرهم هم رد می‌شود")
    void rejectsRepeatedUse() {
        EitaaInitDataValidator validator = validator();
        String initData = EitaaTestData.validInitData(USER_ID);

        validator.validate(initData);

        for (int attempt = 0; attempt < 3; attempt++) {
            assertThatThrownBy(() -> validator.validate(initData))
                    .isInstanceOf(EitaaInitDataReplayedException.class);
        }
    }

    /**
     * 🔴 مهم‌ترین آزمون این کلاس.
     *
     * <p>ترتیب پارامترها در {@code initData} امضا را باطل نمی‌کند: امضا
     * روی نسخه <i>مرتب‌شده</i> حساب می‌شود. پس مهاجمی که یک
     * {@code initData} دزدیده را با ترتیب دیگری بفرستد، رشته خام متفاوتی
     * می‌سازد.
     *
     * <p>اگر اثر انگشت ضدبازپخش از رشته خام ساخته شود، این دو رشته دو
     * اثر انگشت متفاوت می‌دهند و نگه‌بان دور زده می‌شود — با یک
     * {@code ArrayList.reverse} ساده. راه درست، اثر انگشت از رشته
     * canonical است.
     */
    @Test
    @DisplayName("همان داده با ترتیب متفاوت پارامترها هم بازپخش است")
    void rejectsSameInitDataWithReorderedParams() {
        EitaaInitDataValidator validator = validator();

        Map<String, String> params = EitaaTestData.baseParams(USER_ID);
        String original = EitaaTestData.signed(params);

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.reverse(keys);
        Map<String, String> reordered = new LinkedHashMap<>();
        keys.forEach(key -> reordered.put(key, params.get(key)));
        String shuffled = EitaaTestData.signed(reordered);

        assertThat(shuffled)
                .as("پیش‌شرط آزمون: دو رشته باید واقعاً متفاوت باشند، وگرنه آزمون چیزی نمی‌سنجد")
                .isNotEqualTo(original);

        assertThatCode(() -> validator.validate(original))
                .as("رشته اصلی باید بگذرد")
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> validator.validate(shuffled))
                .as("جابه‌جایی ترتیب پارامترها نباید نگه‌بان بازپخش را دور بزند")
                .isInstanceOf(EitaaInitDataReplayedException.class);
    }

    // ─────────────────────────── مواردی که نباید رد شوند ───────────────────────────

    @Test
    @DisplayName("initData تازه برای همان کاربر پذیرفته می‌شود")
    void acceptsFreshInitDataForSameUser() {
        EitaaInitDataValidator validator = validator();

        assertThatCode(() -> validator.validate(EitaaTestData.validInitData(USER_ID)))
                .doesNotThrowAnyException();

        // ورود دوباره در آینده باید کار کند. اگر اثر انگشت به کاربر
        // گره می‌خورد، کاربر تا ابد از ورود محروم می‌شد.
        Map<String, String> later = EitaaTestData.baseParams(
                USER_ID, Instant.now().plusSeconds(1));

        assertThatCode(() -> validator.validate(EitaaTestData.signed(later)))
                .as("initData تازه با auth_date متفاوت، بازپخش نیست")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("initData کاربر دیگر پذیرفته می‌شود")
    void acceptsInitDataForAnotherUser() {
        EitaaInitDataValidator validator = validator();

        validator.validate(EitaaTestData.validInitData(USER_ID));

        assertThatCode(() -> validator.validate(EitaaTestData.validInitData(USER_ID + 1)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("دو نمونه مستقل، همدیگر را بازپخش نمی‌بینند")
    void separateInstancesDoNotShareState() {
        String initData = EitaaTestData.validInitData(USER_ID);

        assertThatCode(() -> validator().validate(initData)).doesNotThrowAnyException();

        // ⚠️ این «سبز بودن» یک <b>محدودیت مستندشده</b> را تثبیت می‌کند،
        // نه یک قابلیت. نگه‌بان فعلی در حافظه است، پس روی چند نمونه،
        // یک initData می‌تواند به هر نمونه یک بار بگذرد. فاز ۱۲ با
        // Redis این را می‌بندد. آزمون اینجاست تا اگر روزی پیاده‌سازی
        // عوض شد، این تفاوت <i>دیده</i> شود نه اینکه بی‌صدا رد شود.
        assertThatCode(() -> validator().validate(initData))
                .as("محدودیت شناخته‌شده: نگه‌بان درون‌حافظه‌ای بین نمونه‌ها مشترک نیست")
                .doesNotThrowAnyException();
    }

    // ─────────────────────────── نگه‌بان ───────────────────────────

    @Test
    @DisplayName("نگه‌بان اثر انگشت تکراری را false می‌دهد")
    void guardReportsDuplicate() {
        InMemoryReplayGuard guard = new InMemoryReplayGuard();
        Duration window = Duration.ofMinutes(5);

        assertThat(guard.markUsed("fingerprint-1", window)).isTrue();
        assertThat(guard.markUsed("fingerprint-1", window)).isFalse();
        assertThat(guard.markUsed("fingerprint-2", window)).isTrue();
    }

    @Test
    @DisplayName("نگه‌بان اثر انگشت خالی را رد می‌کند")
    void guardRejectsBlankFingerprint() {
        InMemoryReplayGuard guard = new InMemoryReplayGuard();

        // ⚠️ رد کردن و نه پذیرفتن: بدون اثر انگشت نمی‌شود بازپخش را
        // تشخیص داد. پذیرفتن یعنی یک مسیر بی‌محافظ که هیچ‌کس نمی‌بیند.
        assertThat(guard.markUsed("", Duration.ofMinutes(5))).isFalse();
        assertThat(guard.markUsed(null, Duration.ofMinutes(5))).isFalse();
        assertThat(guard.markUsed("   ", Duration.ofMinutes(5))).isFalse();
    }

    @Test
    @DisplayName("اثر انگشت منقضی‌شده دوباره قابل استفاده است")
    void guardAllowsReuseAfterWindowExpires() {
        InMemoryReplayGuard guard = new InMemoryReplayGuard();

        // پنجره صفر ثانیه‌ای: رکورد بلافاصله کهنه می‌شود.
        assertThat(guard.markUsed("fingerprint", Duration.ZERO)).isTrue();
        assertThat(guard.markUsed("fingerprint", Duration.ZERO))
                .as("رکورد کهنه نباید یک ورودی تازه را بازپخش اعلام کند")
                .isTrue();
    }

    @Test
    @DisplayName("نگه‌بان با نگه‌داشتن اثر انگشت، همان initData را رد می‌کند")
    void validatorUsesGuard() {
        InMemoryReplayGuard guard = new InMemoryReplayGuard();
        EitaaInitDataValidator validator = validator(guard);

        validator.validate(EitaaTestData.validInitData(USER_ID));

        assertThat(guard.size())
                .as("اعتبارسنجی موفق باید یک اثر انگشت ثبت کرده باشد")
                .isEqualTo(1);
    }
}
