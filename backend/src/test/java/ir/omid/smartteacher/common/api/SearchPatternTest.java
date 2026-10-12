package ir.omid.smartteacher.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * آزمون ساخت الگوی جست‌وجو.
 *
 * <p><b>⚠️ چرا این تست ارزش دارد؟</b> چون خروجی این کلاس مستقیم داخل
 * یک کوئری {@code LIKE} می‌نشیند. اگر فرار دادن نویسه‌های عام خراب شود،
 * نتیجه یک خطای سرور نیست — یک <i>نتیجه اشتباه</i> است. کاربری که
 * «۵۰٪» را جست‌وجو می‌کند، بی‌سروصدا همه رکوردها را می‌بیند و هیچ
 * پیام خطایی هم نمی‌گیرد. تست نکردنش یعنی این باگ تا وقتی کسی از
 * نتیجه‌اش تعجب کند، نامرئی می‌ماند.
 */
class SearchPatternTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("ورودی خالی null می‌دهد — یعنی «بدون فیلتر»")
    void blankInputMeansNoFilter(String input) {
        assertThat(SearchPattern.like(input))
                .as("خالی باید null بدهد تا شرط فیلتر از کوئری حذف شود")
                .isNull();
    }

    @Test
    @DisplayName("متن ساده در دو طرف % می‌نشیند")
    void simpleTextIsWrapped() {
        assertThat(SearchPattern.like("ریاضی")).isEqualTo("%ریاضی%");
    }

    @Test
    @DisplayName("فاصله‌های اضافه حذف می‌شوند")
    void surroundingWhitespaceIsTrimmed() {
        assertThat(SearchPattern.like("  پنجم الف  ")).isEqualTo("%پنجم الف%");
    }

    @Test
    @DisplayName("درصد کاربر فرار داده می‌شود و عام نمی‌ماند")
    void percentIsEscaped() {
        // اگر فرار نمی‌خورد، این ورودی همه رکوردهای مستأجر را برمی‌گرداند.
        assertThat(SearchPattern.like("50%")).isEqualTo("%50\\%%");
    }

    @Test
    @DisplayName("زیرخط کاربر فرار داده می‌شود — هر نویسه‌ای جای آن نمی‌نشیند")
    void underscoreIsEscaped() {
        assertThat(SearchPattern.like("a_b")).isEqualTo("%a\\_b%");
    }

    @Test
    @DisplayName("نویسه فرار کاربر اول از همه فرار داده می‌شود")
    void backslashIsEscapedFirst() {
        // ⚠️ این تست، «ترتیب» را قفل می‌کند و نه فقط نتیجه را. اگر روزی
        // کسی ترتیب replace‌ها را عوض کند، بک‌اسلشی که خودمان اضافه
        // کرده‌ایم دوباره فرار داده می‌شود و الگو خراب می‌شود — بدون
        // اینکه هیچ تست دیگری بلرزد. اینجا فقط یک بک‌اسلش وارد شده و
        // باید دقیقاً دو بک‌اسلش بیرون بیاید.
        assertThat(SearchPattern.like("a\\b")).isEqualTo("%a\\\\b%");
    }

    @Test
    @DisplayName("ترکیب بک‌اسلش و درصد — ترتیب درست، سه بک‌اسلش")
    void backslashFollowedByPercent() {
        // ورودی: \%
        // گام ۱ (فرار بک‌اسلش): \\%
        // گام ۲ (فرار درصد):     \\\%
        assertThat(SearchPattern.like("\\%")).isEqualTo("%\\\\\\%%");
    }

    @Test
    @DisplayName("لاتین به حروف کوچک تبدیل می‌شود")
    void latinIsLowercased() {
        assertThat(SearchPattern.like("MATH-5")).isEqualTo("%math-5%");
    }

    @Test
    @DisplayName("حرف I در Locale ریشه‌ای به i تبدیل می‌شود، نه ı ترکی")
    void turkishDotlessIisNotProduced() {
        // ⚠️ این تست کوچک به نظر می‌رسد ولی یک باگ واقعی را می‌گیرد:
        // String.toLowerCase() بدون آرگومان از Locale پیش‌فرض JVM
        // استفاده می‌کند. روی یک سرور با locale ترکی، «I» به «ı»
        // (i بی‌نقطه) تبدیل می‌شود و جست‌وجوی کد درس «MATH-I» هیچ
        // نتیجه‌ای نمی‌دهد. docs پروژه فارسی است ولی کدهای درس لاتین.
        assertThat(SearchPattern.like("I")).isEqualTo("%i%");
    }
}
