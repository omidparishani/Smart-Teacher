package ir.omid.smartteacher.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

/**
 * ریشه موجودیت‌های مستأجری که <b>حذف نرم</b> می‌شوند.
 *
 * <p>بیشتر جدول‌های دامنه آموزش ({@code classrooms}، {@code students}،
 * {@code books}، {@code assignments}، {@code exams}) ستون
 * {@code deleted_at} دارند: معلمی که یک کلاس را حذف می‌کند نباید
 * نمره‌ها و تکالیف آن کلاس را هم با خودش ببرد. حذف واقعی (Hard Delete)
 * برای پاک‌سازی‌های اداری محفوظ می‌ماند.
 *
 * <p><b>⚠️ چرا این کلاس در ماژول {@code tenant} است و نه
 * {@code common}؟</b> چون از {@link TenantAwareEntity} ارث‌بری می‌کند و
 * آن کلاس اینجاست. اگر در {@code common} می‌بود، {@code common} به
 * {@code tenant} وابسته می‌شد، در حالی که {@code tenant} خودش به
 * {@code common} وابسته است — یک وابستگی دوطرفه که
 * {@code ApplicationModules.verify()} ردش می‌کند.
 *
 * <p><b>⚠️ چرا {@code deletedAt} در {@link ir.omid.smartteacher.common.jpa.BaseEntity}
 * نیست؟</b> چون همه جدول‌ها این ستون را ندارند: {@code tenant_members}،
 * {@code refresh_tokens}، {@code audit_logs} و {@code roles} حذف نرم
 * ندارند (عضویت با {@code status = REMOVED} مدل شده، توکن باطل می‌شود،
 * و لاگ حسابرسی هرگز حذف نمی‌شود). گذاشتنش در ریشه، همه آن‌ها را وادار
 * می‌کرد ستونی بی‌معنا حمل کنند.
 *
 * <p><b>🔴 نکته‌ای که باید بگیری:</b> {@code deleted_at} اینجا <i>فیلتر
 * خودکار نمی‌سازد.</i> هیچ {@code @SQLRestriction} یا {@code @Where} در
 * این کلاس نیست و عمدی است. یعنی {@code repository.findById(...)} رکورد
 * حذف‌شده را <b>برمی‌گرداند</b>. پس هر کوئری خواندن باید خودش شرط
 * {@code deletedAt is null} را بگذارد (الگوی موجود در
 * {@link ir.omid.smartteacher.user.User#isUsable()} هم همین است).
 *
 * <p>چرا فیلتر خودکار نه؟ سه دلیل:
 * <ul>
 *   <li>Hibernate فیلتر روی <b>Native Query</b> اعمال نمی‌کند؛ یعنی
 *       فیلتری که فکر می‌کنیم همه‌جا هست، در گزارش‌های بعدی نیست و
 *       همان‌جا داده حذف‌شده بیرون می‌آید.</li>
 *   <li>بازیابی رکورد حذف‌شده با فیلتر خودکار نیاز به API دور زدن
 *       فیلتر دارد که خودش یک درِ پشتی می‌شود.</li>
 *   <li>شرط صریح در کوئری، در Code Review <i>دیده</i> می‌شود؛ فیلتر
 *       خودکار دیده نمی‌شود و همین چیزی است که فراموش‌شدنش کسی را
 *       متوجه نمی‌کند.</li>
 * </ul>
 */
@MappedSuperclass
public abstract class SoftDeletableTenantEntity extends TenantAwareEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Instant getDeletedAt() {
        return deletedAt;
    }

    /** آیا این رکورد حذف نرم شده است؟ */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * حذف نرم — رکورد در دیتابیس می‌ماند ولی از دید کاربر می‌رود.
     *
     * <p>⚠️ این متد <b>idempotent</b> است: صدا زدن دوباره‌اش زمان حذف
     * نخست را عوض نمی‌کند. اگر هر بار {@code Instant.now()} بگذاشتیم،
     * دو درخواست حذف همزمان دو زمان متفاوت می‌نوشتند و «کِی حذف شد»
     * بی‌معنا می‌شد.
     */
    public void softDelete() {
        if (deletedAt == null) {
            deletedAt = Instant.now();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{id=" + getId() + ", tenantId=" + getTenantId()
                + ", deleted=" + isDeleted() + '}';
    }
}
