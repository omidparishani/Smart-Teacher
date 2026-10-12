package ir.omid.smartteacher.student;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * قرارداد عمومی ماژول {@code student} — تنها راه دسترسی ماژول‌های دیگر
 * به داده دانش‌آموز.
 *
 * <p><b>🔴 چرا این اینترفیس وجود دارد؟</b> چون قاعده ۴
 * {@code docs/architecture.md} می‌گوید «هیچ ماژولی نباید
 * {@code Repository} ماژول دیگر را تزریق کند؛ فقط از طریق Facade یا
 * رویداد». ماژول {@code classroom} برای نشان‌دادن فهرست دانش‌آموزان یک
 * کلاس به نام و کد ملی آن‌ها نیاز دارد، ولی مالک جدول {@code students}
 * این ماژول است.
 *
 * <p><b>⚠️ چرا این اینترفیس در بسته ریشه است؟</b> چون ماژول
 * {@code student} بسته (CLOSED) است و در حالت بسته، مدولیت فقط بسته
 * ریشه و بسته‌های {@code @NamedInterface} را بیرون می‌دهد. اگر این
 * اینترفیس داخل {@code student.internal} بود، {@code classroom} حتی
 * نمی‌توانست به آن اشاره کند و {@code ApplicationModulesTest} ردش
 * می‌کرد.
 *
 * <p><b>⚠️ چرا اینترفیس جدا از پیاده‌سازی؟</b> چون پیاده‌سازی‌اش
 * {@code StudentRepository} را تزریق می‌کند و آن داخل
 * {@code student.internal} است. اگر مصرف‌کننده‌ها مستقیم به کلاس
 * پیاده‌سازی وابسته می‌شدند، همه جزئیات داخلی هم بیرون می‌ریخت.
 *
 * <p><b>⚠️ چه چیزی عمداً اینجا نیست؟</b> هیچ متدی که رکورد را
 * <i>تغییر</i> بدهد. ماژول‌های دیگر نباید بتوانند دانش‌آموز بسازند یا
 * عوض کنند؛ فقط بخوانند. نوشتن کار {@code StudentController} و
 * {@code StudentService} است.
 */
public interface StudentDirectory {

    /**
     * خلاصه دانش‌آموزان فعال با این شناسه‌ها — برای یک کوئری دسته‌ای.
     *
     * <p>⚠️ شناسه‌هایی که دانش‌آموز فعالی ندارند (ناموجود، حذف‌نرم‌شده،
     * یا متعلق به مستأجر دیگر) در خروجی <b>غایب</b> می‌مانند؛ خطا
     * نمی‌دهند. مصرف‌کننده باید غیبت را «دیده‌نشده» تفسیر کند.
     *
     * @param studentIds شناسه‌ها؛ اگر خالی باشد بدون کوئری برمی‌گردد
     * @return نگاشت شناسه به خلاصه — فقط برای دانش‌آموزان فعال
     */
    Map<UUID, StudentSummary> summariesOf(Collection<UUID> studentIds);

    /**
     * زیرمجموعه شناسه‌هایی که دانش‌آموز فعال دارند.
     *
     * <p>برای اعتبارسنجی پیش از نوشتن: اگر می‌خواهید دانش‌آموزی را در
     * کلاسی ثبت‌نام کنید، اول باید مطمئن شوید واقعاً وجود دارد و مال
     * همین مستأجر است. فیلتر {@code @TenantId} این را خودکار تضمین
     * می‌کند، پس فقط <i>فعال‌بودن</i> می‌ماند.
     *
     * @param studentIds شناسه‌ها
     * @return شناسه‌های معتبر، نه بیشتر
     */
    Set<UUID> activeIdsAmong(Collection<UUID> studentIds);

    /**
     * خلاصه یک دانش‌آموز — کمینه داده‌ای که فهرست کلاس لازم دارد.
     *
     * <p>⚠️ {@code nationalCode} اینجا هست چون معلم برای تشخیص
     * دانش‌آموزان هم‌نام به آن نیاز دارد. ولی {@code guardianMobile} و
     * {@code metadata} عمداً نیستند: آن‌ها داده تماس‌اند و نباید در
     * فهرست کلاس بیرون بیایند.
     *
     * @param id           شناسه دانش‌آموز
     * @param fullName     نام کامل
     * @param nationalCode کد ملی؛ ممکن است {@code null} باشد
     */
    record StudentSummary(UUID id, String fullName, String nationalCode) {
    }
}
