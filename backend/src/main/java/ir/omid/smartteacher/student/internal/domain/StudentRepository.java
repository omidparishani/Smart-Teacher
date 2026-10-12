package ir.omid.smartteacher.student.internal.domain;

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
 * دسترسی به دانش‌آموزان.
 *
 * <p>همان قاعده {@code SubjectRepository}: هیچ {@code findById} خامی
 * استفاده نمی‌شود، چون {@code deleted_at} فیلتر خودکار ندارد و متد
 * ارث‌بری‌شده رکورد حذف‌شده را هم برمی‌گرداند.
 *
 * <p>فیلتر مستأجر خودکار است ({@code @TenantId}) و اینجا نوشته
 * نمی‌شود.
 */
public interface StudentRepository extends JpaRepository<Student, UUID> {

    /** یک دانش‌آموز فعال با شناسه — شکل امن دسترسی به یک رکورد مشخص. */
    Optional<Student> findByIdAndDeletedAtIsNull(UUID id);

    /**
     * فهرست دانش‌آموزان فعال با فیلترهای اختیاری.
     *
     * <p>⚠️ الگوی {@code :param is null} با یک مقایسه نوع‌دار در کنارش،
     * وگرنه Hibernate خطای «could not determine type» می‌دهد.
     *
     * @param pattern الگوی آماده جست‌وجو در نام؛ {@code null} یعنی بدون فیلتر
     * @param status  وضعیت؛ {@code null} یعنی همه وضعیت‌ها
     */
    @Query("""
            select s from Student s
            where s.deletedAt is null
              and (:pattern is null or lower(s.fullName) like :pattern escape '\\')
              and (:status is null or s.status = :status)
            """)
    Page<Student> findForListing(@Param("pattern") String pattern,
                                 @Param("status") StudentStatus status,
                                 Pageable pageable);

    /**
     * شناسه دانش‌آموزانی که این کد ملی را دارند.
     *
     * <p>⚠️ برخلاف کد درس، اینجا {@code lower()} لازم نیست: کد ملی فقط
     * رقم است و حالت حروف ندارد. ایندکس یکتای
     * {@code uk_students_national_code} هم روی ستون خام است — این متد
     * آینه دقیق آن است، نه چیزی شبیه‌به‌آن.
     *
     * @param nationalCode کد ملی — نباید {@code null} باشد
     * @return شناسه رکوردهای متعارض (بدون احتساب حذف‌شده‌ها)
     */
    @Query("""
            select s.id from Student s
            where s.deletedAt is null
              and s.nationalCode = :nationalCode
            """)
    List<UUID> findConflictingNationalCodeIds(@Param("nationalCode") String nationalCode);

    /**
     * دانش‌آموزان فعال با این شناسه‌ها.
     *
     * <p>مصرف اصلی‌اش فهرست دانش‌آموزان یک کلاس است: شناسه‌ها از جدول
     * ثبت‌نام می‌آید و نام‌ها با <b>یک</b> کوئری خوانده می‌شود.
     * شناسه‌ای که دانش‌آموز فعال ندارد در نتیجه غایب می‌ماند — و همین
     * رفتار درست است: دانش‌آموز حذف‌نرم‌شده باید بی‌صدا از فهرست کلاس
     * برود، نه اینکه یک ردیف بی‌نام بسازد.
     */
    List<Student> findByIdInAndDeletedAtIsNull(Collection<UUID> ids);
}
