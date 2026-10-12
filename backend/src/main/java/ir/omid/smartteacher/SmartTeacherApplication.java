package ir.omid.smartteacher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * نقطه ورود برنامه «دستیار هوشمند معلم».
 *
 * <p><b>معماری:</b> مونولیت ماژولار. همه ماژول‌ها در یک فرآیند اجرا
 * می‌شوند اما مرزهایشان با Spring Modulith و ArchUnit اجبار می‌شود —
 * ماژولی نمی‌تواند به کلاس داخلی ماژول دیگر دست بزند. این یعنی
 * می‌شود بعداً یک ماژول را به سرویس جدا برد بدون بازنویسی.
 *
 * <p><b>چرا مونولیت و نه میکروسرویس از ابتدا؟</b> چون محصول هنوز
 * مرزهای دامنه‌اش تثبیت نشده. شکستن زودهنگام به میکروسرویس، هزینه
 * عملیاتی و پیچیدگی توزیع‌شده را تحمیل می‌کند بدون آنکه سودی بدهد.
 *
 * <p>یادداشت‌های پیکربندی:
 * <ul>
 *   <li>{@code @ConfigurationPropertiesScan} کلاس‌های
 *       {@code @ConfigurationProperties} را خودکار پیدا می‌کند؛ لازم
 *       نیست هر کدام با {@code @EnableConfigurationProperties} ثبت شود.</li>
 *   <li>{@code @EnableJpaAuditing} ستون‌های {@code created_at} و
 *       {@code created_by} را پر می‌کند. نام Bean حسابرسی صریحاً
 *       {@code auditorAware} تعیین شده تا اگر روزی Bean دومی از همان
 *       نوع اضافه شد، Spring دوتایی نشود.</li>
 *   <li>{@code @EnableAsync} برای کارهای پس‌زمینه. <b>⚠️ توجه:</b>
 *       {@code ScopedValue} زمینه مستأجر به Thread دیگر منتقل نمی‌شود،
 *       پس هر متد {@code @Async} که به داده مستأجر دست می‌زند باید
 *       زمینه را صریحاً با {@code TenantContext.callAs} دوباره برقرار
 *       کند.</li>
 * </ul>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableAsync
@EnableScheduling
public class SmartTeacherApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartTeacherApplication.class, args);
    }
}
