package ir.omid.smartteacher.classroom.internal.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * دسترسی به درس‌ها.
 *
 * <p><b>⚠️ چرا هیچ‌جا {@code findById} صدا زده نمی‌شود؟</b> چون
 * {@code deleted_at} فیلتر خودکار ندارد (دلیلش در Javadoc
 * {@link ir.omid.smartteacher.tenant.SoftDeletableTenantEntity}). متد
 * {@code findById} که از {@link JpaRepository} به ارث می‌رسد، رکورد حذف‌شده
 * را هم برمی‌گرداند. پس همه متدهای این اینترفیس صریحاً
 * {@code DeletedAtIsNull} دارند و هیچ‌کدام نباید حذف شود.
 *
 * <p>فیلتر مستأجر برعکس، خودکار است ({@code @TenantId}) و در این
 * کوئری‌ها نوشته <i>نمی‌شود</i> — نوشتنش هم بی‌فایده است و هم این
 * توهم را می‌سازد که امنیت از همین شرط می‌آید.
 */
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    /** یک درس فعال با شناسه — شکل امن دسترسی به یک رکورد مشخص. */
    Optional<Subject> findByIdAndDeletedAtIsNull(UUID id);

    Page<Subject> findByDeletedAtIsNull(Pageable pageable);

    /**
     * جست‌وجو در نام درس.
     *
     * <p>الگو باید <b>از قبل</b> ساخته شده باشد: حروف کوچک و
     * {@code %} و {@code _} فرار داده‌شده. ساختنش در سرویس است تا
     * کاربر نتواند با فرستادن {@code %} همه رکوردها را به‌عنوان نتیجه
     * بگیرد (و ایندکس trigram بی‌اثر شود).
     *
     * @param pattern الگوی آماده مثل {@code %ریاضی%}؛ {@code null} یعنی بدون فیلتر
     */
    @Query("""
            select s from Subject s
            where s.deletedAt is null
              and (:pattern is null or lower(s.name) like :pattern escape '\\')
            """)
    Page<Subject> searchByPattern(@Param("pattern") String pattern, Pageable pageable);

    /**
     * شناسه درس‌هایی که این کد را دارند.
     *
     * <p><b>⚠️ فقط کد، و نه نام — و این آینه‌ی دقیق قید دیتابیس است.</b>
     * ایندکس یکتای {@code uk_subjects_code} روی کد است، نه نام. نام
     * عمداً یکتا نشد: «ریاضی» پایه پنجم و «ریاضی» پایه ششم دو درس
     * متفاوت‌اند. اگر این متد نام را هم بررسی می‌کرد، سرویس چیزی را رد
     * می‌کرد که دیتابیس اجازه می‌دهد — و روزی که کسی ایندکس را با
     * کوئری مقایسه کند، هیچ‌کدام مرجع نیست.
     *
     * <p>⚠️ اگر کد {@code null} باشد این متد اصلاً صدا زده نمی‌شود،
     * چون {@code lower(null)} در SQL {@code NULL} می‌دهد و هیچ رکوردی
     * مطابقت نمی‌کند — یعنی دو درس هر دو بی‌کد، به‌درستی، تعارض
     * نمی‌گیرند.
     *
     * @param code کد درس — نباید {@code null} باشد
     * @return شناسه رکوردهای متعارض (بدون احتساب حذف‌شده‌ها)
     */
    @Query("""
            select s.id from Subject s
            where s.deletedAt is null
              and lower(s.code) = lower(:code)
            """)
    List<UUID> findConflictingCodeIds(@Param("code") String code);

    /** درس‌های فعال با این شناسه‌ها — برای نام‌گذاری دسته‌ای و بدون N+1. */
    List<Subject> findByIdInAndDeletedAtIsNull(Collection<UUID> ids);
}
