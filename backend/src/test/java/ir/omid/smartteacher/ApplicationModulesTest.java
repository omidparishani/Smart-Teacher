package ir.omid.smartteacher;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * بازبینی ساختار ماژولار — قید پذیرش فاز ۱.
 *
 * <p>{@code verify()} سه چیز را می‌سنجد و هر سه می‌توانند بی‌صدا خراب
 * شوند تا روزی که جدا کردن یک ماژول به سرویس مستقل غیرممکن شود:
 * <ol>
 *   <li><b>دسترسی به کلاس‌های داخلی:</b> ماژولی نباید به کلاس داخلی
 *       ماژول دیگر دست بزند. فقط API عمومی‌اش مجاز است.</li>
 *   <li><b>وابستگی چرخه‌ای:</b> اگر {@code a → b} و {@code b → a} باشد،
 *       این دو در واقع یک ماژول‌اند که به دو بسته تقسیم شده و هرگز
 *       جدا نمی‌شوند.</li>
 *   <li><b>مرز ماژول‌های بسته:</b> ماژول {@code CLOSED} نمی‌تواند
 *       زیربسته عمومی داشته باشد.</li>
 * </ol>
 *
 * <p>این تست بدون کانتکست Spring اجرا می‌شود چون Modulith فقط بایت‌کد
 * را می‌خواند. یعنی سریع است و می‌تواند در هر اجرا باشد.
 *
 * <p><b>⚠️ نکته درباره‌ی سبک نوشتن این تست:</b> فقط از
 * {@code verify()} و {@code stream()} استفاده شده و از دسترسی به
 * ویژگی‌های تک‌تک ماژول‌ها پرهیز شده است. دلیلش این است که
 * {@code ApplicationModule} در نسخه‌های مختلف Modulith نام‌های
 * دسترسی متفاوتی دارد و نوشتن تستی که به آن نام‌ها گره بخورد، بیلد را
 * به نسخه کتابخانه وابسته می‌کند. بررسی موجود بودن بسته‌های اصلی در
 * {@code ArchitectureTest} و با ArchUnit انجام می‌شود.
 */
class ApplicationModulesTest {

    /**
     * ⚠️ ماژول‌ها یک بار و به‌صورت {@code static} ساخته می‌شوند.
     * ساخت هر بار، خواندن همه بایت‌کدها را تکرار می‌کند و بی‌دلیل
     * اجرای تست را کند می‌کند.
     */
    private static final ApplicationModules MODULES =
            ApplicationModules.of(SmartTeacherApplication.class);

    @Test
    @DisplayName("ساختار ماژولار بدون خطا بازبینی می‌شود")
    void verifiesModularStructure() {
        // اگر این خط شکست بخورد، پیام خطا دقیقاً می‌گوید کدام ماژول به
        // کلاس داخلی کدام ماژول دست زده یا کجا چرخه ایجاد شده است.
        MODULES.verify();
    }

    @Test
    @DisplayName("ماژول‌ها شناسایی شده‌اند")
    void detectsModules() {
        // بیشتر از یک ماژول — یعنی تشخیص ماژول کار می‌کند و همه کد در
        // «ماژول ریشه» فرو نریخته است. اگر تشخیص خراب شود، verify()
        // هم بی‌معنا می‌شود چون چیزی برای بررسی وجود ندارد.
        assertThat(MODULES.stream().count())
                .as("باید چند ماژول تشخیص داده شود، نه فقط ریشه")
                .isGreaterThan(1L);
    }
}
