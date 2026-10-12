package ir.omid.smartteacher.config;

import ir.omid.smartteacher.tenant.CurrentTenantIdentifierResolverImpl;
import ir.omid.smartteacher.tenant.TenantContext;
import java.util.UUID;
import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

/**
 * پیکربندی Hibernate برای چند‌مستأجری و حسابرسی خودکار.
 *
 * <p><b>استراتژی {@code DISCRIMINATOR}:</b> همه مستأجرها در یک دیتابیس
 * و یک schema هستند و با ستون {@code tenant_id} از هم جدا می‌شوند.
 * مقایسه با گزینه‌های دیگر:
 * <ul>
 *   <li><b>DATABASE</b> (یک دیتابیس برای هر مستأجر): جداسازی قوی‌تر،
 *       اما با هزار معلم یعنی هزار دیتابیس — مهاجرت، پشتیبان‌گیری و
 *       پایش از کنترل خارج می‌شود.</li>
 *   <li><b>SCHEMA</b> (یک schema برای هر مستأجر): تعداد schema‌ها
 *       دیتابیس را کند می‌کند و ابزار مهاجرت را پیچیده؛ در عمل برای
 *       ده‌ها مستأجر خوب است، برای هزاران نه.</li>
 * </ul>
 * انتخاب فعلی برای مقیاس هدف (هزاران معلم) درست است. اگر روزی
 * مشتری سازمانی جداسازی فیزیکی خواست، مسیرش افزودن یک لایه schema
 * برای همان مستأجر است، نه بازنویسی کل مدل.
 */
@Configuration
public class HibernateTenantConfig {

    /**
     * ثبت resolver مستأجر برای Hibernate.
     *
     * <p>عمداً اینجا انجام می‌شود و نه در {@code application.yml}: با
     * این روش همان <i>نمونه Bean اسپرینگی</i> به Hibernate داده می‌شود.
     * اگر نام کلاس را در yml می‌دادیم، Hibernate نمونه خودش را می‌ساخت
     * و دو نمونه موازی داشتیم — که فعلاً بی‌ضرر است چون resolver
     * حالت‌دار نیست، اما با اولین تغییری که حالت اضافه کند به باگ
     * خاموش تبدیل می‌شود.
     *
     * <p><b>⚠️ تفاوت با Hibernate 6:</b> در Hibernate 6 برای این کار دو
     * تنظیم لازم بود — {@code hibernate.multiTenancy=DISCRIMINATOR} و
     * همین resolver. در Hibernate 7 (که Boot 4.1.1 می‌آورد) تنظیم اول
     * <b>حذف شده است</b>: کلاس {@code org.hibernate.cfg.MultiTenancyStrategy}
     * دیگر وجود ندارد و {@code AvailableSettings.MULTI_TENANT} هم نیست.
     * دلیلش این است که {@code hibernate.multiTenancy} فقط به جداسازی
     * <i>سطح اتصال</i> (DATABASE/SCHEMA) مربوط بود؛ جداسازی سطح سطری که
     * ما استفاده می‌کنیم فقط با {@code @TenantId} و همین resolver کار
     * می‌کند و از ابتدا به آن تنظیم نیازی نداشت.
     *
     * <p>این با خطای کامپایل واقعی کشف شد، نه با حدس — ثبت‌شده در
     * {@code docs/verification-log.md} بخش ۹.
     */
    @Bean
    public HibernatePropertiesCustomizer multiTenancyCustomizer(
            CurrentTenantIdentifierResolverImpl tenantIdentifierResolver) {
        return properties -> properties.put(
                MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                tenantIdentifierResolver);
    }

    /**
     * پرکردن {@code created_by} و {@code updated_by}.
     *
     * <p>مقدار از {@link TenantContext} می‌آید — یعنی از JWT — هرگز از
     * ورودی کاربر. این تفاوت مهم است: اگر مقدار از بدنه درخواست خوانده
     * می‌شد، هر کاربری می‌توانست عمل خودش را به نام دیگری ثبت کند و کل
     * لاگ حسابرسی بی‌ارزش می‌شد.
     *
     * <p>وقتی کاربری وجود ندارد (Job زمان‌بندی‌شده، مهاجرت داده)
     * {@code empty} برمی‌گردد و ستون {@code null} می‌ماند — که درست
     * است، چون واقعاً کاربری در کار نبوده.
     */
    @Bean
    public AuditorAware<UUID> auditorAware() {
        return TenantContext::currentUserId;
    }
}
