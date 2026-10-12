package ir.omid.smartteacher.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * آزمون تولیدکننده UUID نسخه ۷.
 *
 * <p>آنچه اینجا مهم است، درست‌بودن چیدمان بیت‌هاست. اگر بیت‌های نسخه یا
 * variant اشتباه نوشته شوند، شناسه‌ها همچنان تولید می‌شوند و در دیتابیس
 * ذخیره می‌گردند — ولی دیگر UUIDv7 معتبر نیستند و همه چیزهایی که به
 * ترتیب زمانی یا ایندکس‌پذیری آن تکیه دارند بی‌صدا خراب می‌شوند. این
 * نوع خطا تا مرحله تولید لو نمی‌رود، پس باید اینجا گرفته شود.
 */
class UuidV7GeneratorTest {

    /** زمان نمونه: نوامبر ۲۰۲۳ — به‌راحتی در ۴۸ بیت جا می‌شود. */
    private static final long SAMPLE_EPOCH_MILLI = 1_700_000_000_000L;

    // ─────────────────────── ساختار ───────────────────────

    @Test
    @DisplayName("بیت‌های نسخه برابر ۷ است")
    void hasVersionSeven() {
        assertThat(UuidV7Generator.generate().version()).isEqualTo(7);
    }

    @Test
    @DisplayName("بیت‌های variant مطابق استاندارد IETF است")
    void hasIetfVariant() {
        // variant() مقدار ۲ را برای الگوی 10xx برمی‌گرداند که همان
        // چیزی است RFC 9562 الزام می‌کند.
        assertThat(UuidV7Generator.generate().variant()).isEqualTo(2);
    }

    @Test
    @DisplayName("generate شناسه نسخه ۷ می‌سازد")
    void generateProducesVersionSeven() {
        assertThat(UuidV7Generator.isVersion7(UuidV7Generator.generate())).isTrue();
    }

    // ─────────────────────── زمان ───────────────────────

    @Test
    @DisplayName("زمان تزریق‌شده دقیقاً برمی‌گردد")
    void timestampRoundTrips() {
        UUID uuid = UuidV7Generator.generate(SAMPLE_EPOCH_MILLI);

        assertThat(UuidV7Generator.extractTimestamp(uuid)).isEqualTo(SAMPLE_EPOCH_MILLI);
    }

    @Test
    @DisplayName("زمان صفر هم درست رمزگذاری می‌شود")
    void handlesEpochZero() {
        UUID uuid = UuidV7Generator.generate(0L);

        assertThat(UuidV7Generator.extractTimestamp(uuid)).isZero();
        assertThat(uuid.version()).isEqualTo(7);
    }

    @Test
    @DisplayName("شناسه‌های با زمان بیشتر، ترتیب زمانی دارند")
    void ordersChronologically() {
        UUID earlier = UuidV7Generator.generate(SAMPLE_EPOCH_MILLI);
        UUID later = UuidV7Generator.generate(SAMPLE_EPOCH_MILLI + 1);

        // همان دلیلی که نسخه ۷ را به نسخه ۴ ترجیح دادیم: پیشوند زمانی
        // باعث درج ترتیبی در B-tree می‌شود و صفحه‌آرایی تصادفی را از
        // بین می‌برد.
        assertThat(UuidV7Generator.extractTimestamp(earlier))
                .isLessThan(UuidV7Generator.extractTimestamp(later));
    }

    @Test
    @DisplayName("generate() از ساعت سیستم استفاده می‌کند")
    void generateUsesCurrentTime() {
        long before = System.currentTimeMillis();
        UUID uuid = UuidV7Generator.generate();
        long after = System.currentTimeMillis();

        assertThat(UuidV7Generator.extractTimestamp(uuid)).isBetween(before, after);
    }

    @Test
    @DisplayName("زمان پیش از مبدأ یونیکس پذیرفته نمی‌شود")
    void rejectsNegativeTimestamp() {
        assertThatThrownBy(() -> UuidV7Generator.generate(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─────────────────────── یکتایی و تصادفی‌بودن ───────────────────────

    @Test
    @DisplayName("هزار شناسه پیاپی همه یکتا هستند")
    void generatesUniqueIdentifiers() {
        Set<UUID> generated = new HashSet<>();
        int count = 1000;

        for (int i = 0; i < count; i++) {
            generated.add(UuidV7Generator.generate());
        }

        assertThat(generated).hasSize(count);
    }

    @Test
    @DisplayName("دو شناسه با زمان یکسان هم متفاوت‌اند")
    void sameTimestampStillYieldsDifferentIds() {
        // بخش تصادفی باید فضای UUID را پر کند حتی وقتی زمان تکرار
        // می‌شود (که در بار بالا مرتب رخ می‌دهد).
        UUID first = UuidV7Generator.generate(SAMPLE_EPOCH_MILLI);
        UUID second = UuidV7Generator.generate(SAMPLE_EPOCH_MILLI);

        assertThat(first).isNotEqualTo(second);
    }

    // ─────────────────────── بررسی نسخه ───────────────────────

    @Test
    @DisplayName("isVersion7 برای نسخه ۴ نادرست است")
    void isVersionSevenRejectsVersionFour() {
        assertThat(UuidV7Generator.isVersion7(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("isVersion7 برای null نادرست است و استثنا نمی‌دهد")
    void isVersionSevenHandlesNull() {
        assertThat(UuidV7Generator.isVersion7(null)).isFalse();
    }

    @Test
    @DisplayName("استخراج زمان از UUID غیر نسخه ۷ استثنا می‌دهد")
    void extractTimestampRejectsNonVersionSeven() {
        assertThatThrownBy(() -> UuidV7Generator.extractTimestamp(UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─────────────────────── کلاس ابزاری ───────────────────────

    @Test
    @DisplayName("کلاس ابزاری قابل نمونه‌سازی نیست")
    void cannotBeInstantiated() throws Exception {
        var constructor = UuidV7Generator.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThatThrownBy(constructor::newInstance)
                .hasCauseInstanceOf(AssertionError.class);
    }
}
