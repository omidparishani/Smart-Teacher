package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.common.error.EitaaInitDataExpiredException;
import ir.omid.smartteacher.common.error.EitaaInitDataInvalidException;
import ir.omid.smartteacher.eitaa.internal.InMemoryReplayGuard;
import ir.omid.smartteacher.eitaa.internal.LocalHmacVerifier;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * پنجره تازگی {@code auth_date}.
 *
 * <p><b>⚠️ چرا «تازگی» لازم است، در حالی که امضا قبلاً درستی را ثابت
 * کرده؟</b> چون امضا فقط می‌گوید «این داده از ایتا آمده و دست نخورده».
 * نمی‌گوید <i>کِی</i>. یک {@code initData} که کسی از Log یا حافظه
 * کلاینت برداشته، سال دیگر هم امضایش معتبر است. پنجره تازگی همان چیزی
 * است که «دزدیده‌شده» را از «تازه» جدا می‌کند.
 *
 * <p>این کلاس هر دو لبه پنجره را می‌سنجد: قدیمی و آینده. لبه آینده
 * اغلب فراموش می‌شود و بدون آن، یک {@code auth_date} در آینده
 * <b>هرگز</b> منقضی نمی‌شود.
 */
class EitaaInitDataExpiredTest {

    private static final long USER_ID = 555_000_111L;

    private static EitaaInitDataValidator validator(EitaaProperties properties) {
        return new EitaaInitDataValidator(
                properties,
                new LocalHmacVerifier(properties),
                new InMemoryReplayGuard(),
                EitaaTestData.mapper());
    }

    private static EitaaInitDataValidator validator() {
        return validator(EitaaTestData.botProperties());
    }

    private static String initDataIssuedAt(Instant issuedAt) {
        return EitaaTestData.signed(EitaaTestData.baseParams(USER_ID, issuedAt));
    }

    // ─────────────────────────── گذشته ───────────────────────────

    @Test
    @DisplayName("auth_date قدیمی‌تر از پنجره رد می‌شود")
    void rejectsOldAuthDate() {
        Instant tooOld = Instant.now().minus(EitaaTestData.MAX_AGE).minusSeconds(60);

        assertThatThrownBy(() -> validator().validate(initDataIssuedAt(tooOld)))
                .as("initData دزدیده‌شده و کهنه نباید پذیرفته شود")
                .isInstanceOf(EitaaInitDataExpiredException.class);
    }

    @Test
    @DisplayName("auth_date خیلی قدیمی (چند روز) رد می‌شود")
    void rejectsVeryOldAuthDate() {
        Instant weekAgo = Instant.now().minusSeconds(7 * 24 * 3600L);

        assertThatThrownBy(() -> validator().validate(initDataIssuedAt(weekAgo)))
                .isInstanceOf(EitaaInitDataExpiredException.class);
    }

    @Test
    @DisplayName("auth_date درست داخل پنجره پذیرفته می‌شود")
    void acceptsAuthDateInsideWindow() {
        Instant justInside = Instant.now().minus(EitaaTestData.MAX_AGE).plusSeconds(30);

        assertThatCode(() -> validator().validate(initDataIssuedAt(justInside)))
                .as("نزدیک لبه ولی داخل پنجره باید بگذرد")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("auth_date همین حالا پذیرفته می‌شود")
    void acceptsCurrentAuthDate() {
        assertThatCode(() -> validator().validate(initDataIssuedAt(Instant.now())))
                .doesNotThrowAnyException();
    }

    // ─────────────────────────── آینده ───────────────────────────

    @Test
    @DisplayName("auth_date در آینده دور رد می‌شود")
    void rejectsFutureAuthDate() {
        Instant tomorrow = Instant.now().plusSeconds(24 * 3600L);

        // 🔴 بدون این بررسی، یک auth_date در آینده هرگز منقضی نمی‌شود:
        // «قدیمی‌تر از پنجره» برای مقداری که در آینده است هیچ‌وقت صادق
        // نمی‌شود. پس تازگی عملاً بی‌اثر می‌شد.
        assertThatThrownBy(() -> validator().validate(initDataIssuedAt(tomorrow)))
                .as("auth_date آینده باید رد شود، وگرنه پنجره تازگی بی‌معنا می‌شود")
                .isInstanceOf(EitaaInitDataInvalidException.class)
                .isNotInstanceOf(EitaaInitDataExpiredException.class);
    }

    @Test
    @DisplayName("کجی ساعت چند دقیقه‌ای تحمل می‌شود")
    void toleratesSmallClockSkew() {
        // ساعت دستگاه‌های واقعی جلو و عقب می‌رود. رد کردن این حالت،
        // کاربران بی‌گناه را بیرون می‌گذارد — و آن‌ها هیچ راهی برای
        // فهمیدن علت ندارند.
        Instant slightlyAhead = Instant.now().plusSeconds(60);

        assertThatCode(() -> validator().validate(initDataIssuedAt(slightlyAhead)))
                .as("یک دقیقه جلو بودن ساعت باید تحمل شود")
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("کجی ساعت بزرگ‌تر از حد، رد می‌شود")
    void rejectsLargeClockSkew() {
        Instant wayAhead = Instant.now().plusSeconds(3600);

        assertThatThrownBy(() -> validator().validate(initDataIssuedAt(wayAhead)))
                .isInstanceOf(EitaaInitDataInvalidException.class);
    }

    // ─────────────────────────── پنجره پیکربندی‌پذیر ───────────────────────────

    @Test
    @DisplayName("پنجره کوتاه‌تر، داده را زودتر منقضی می‌کند")
    void shorterWindowExpiresSooner() {
        // ⚠️ این آزمون چیزی را می‌سنجد که آزمون‌های بالا نمی‌سنجند:
        // اینکه مقدار پیکربندی <i>واقعاً اعمال می‌شود</i>. اگر کسی
        // initDataMaxAge را نادیده بگیرد و همیشه ۵ دقیقه فرض کند،
        // آزمون‌های دیگر سبز می‌مانند و اینجا می‌افتد.
        String initData = initDataIssuedAt(Instant.now().minusSeconds(120));

        assertThatCode(() -> validator(EitaaTestData.botProperties()).validate(initData))
                .as("با پنجره پیش‌فرض ۵ دقیقه‌ای، داده دو دقیقه‌ای باید بگذرد")
                .doesNotThrowAnyException();

        EitaaProperties strict = EitaaTestData.withMaxAge(Duration.ofSeconds(30));

        assertThatThrownBy(() -> validator(strict).validate(initData))
                .as("با پنجره ۳۰ ثانیه‌ای، همان داده باید منقضی شود")
                .isInstanceOf(EitaaInitDataExpiredException.class);
    }

    @Test
    @DisplayName("auth_date با فاصله اضافی هم خوانده می‌شود")
    void toleratesWhitespaceAroundAuthDate() {
        Map<String, String> params = EitaaTestData.baseParams(USER_ID);
        params.put("auth_date", " " + Instant.now().getEpochSecond() + " ");

        assertThatCode(() -> validator().validate(EitaaTestData.signed(params)))
                .as("پارامتر نباید به‌خاطر یک فاصله رد شود")
                .doesNotThrowAnyException();
    }
}
