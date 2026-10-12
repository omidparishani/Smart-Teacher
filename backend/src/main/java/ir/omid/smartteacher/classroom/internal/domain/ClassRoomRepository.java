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
 * دسترسی به کلاس‌ها.
 *
 * <p>همان قاعده {@link SubjectRepository} اینجا هم برقرار است: هیچ
 * {@code findById} خامی استفاده نمی‌شود، چون {@code deleted_at} فیلتر
 * خودکار ندارد.
 *
 * <p>فیلتر مستأجر خودکار است و اینجا نوشته نمی‌شود. <b>اما توجه:</b>
 * این فیلتر «کدام مستأجر» را جواب می‌دهد، نه «کدام معلم». محدودکردن
 * معلم به کلاس‌های خودش یک تصمیم کسب‌وکار است و جای آن
 * {@code ClassRoomService} است، نه اینجا.
 */
public interface ClassRoomRepository extends JpaRepository<ClassRoom, UUID> {

    /** یک کلاس فعال با شناسه — شکل امن دسترسی به یک رکورد مشخص. */
    Optional<ClassRoom> findByIdAndDeletedAtIsNull(UUID id);

    /** چند کلاس فعال با شناسه — برای نام‌های دسته‌ای. */
    List<ClassRoom> findByIdInAndDeletedAtIsNull(Collection<UUID> ids);

    /**
     * فهرست کلاس‌های فعال با فیلترهای اختیاری.
     *
     * <p><b>⚠️ چرا یک کوئری با پارامترهای اختیاری و نه چهار متد
     * مشتق‌شده؟</b> چون چهار متد یعنی چهار جای فراموش‌کردنِ
     * {@code deletedAt is null} و چهار جای افزودن یک فیلتر بعدی.
     * یک کوئری، یک جا.
     *
     * <p>هر پارامتر <i>در یک مقایسه نوع‌دار</i> هم ظاهر می‌شود
     * ({@code c.status = :status}) و همین باعث می‌شود Hibernate بتواند
     * نوعش را استنتاج کند — بدون آن، الگوی {@code :param is null} خطای
     * «could not determine type» می‌دهد.
     *
     * @param pattern   الگوی آماده‌ی جست‌وجو در نام؛ {@code null} یعنی بدون فیلتر
     * @param status    وضعیت کلاس؛ {@code null} یعنی همه وضعیت‌ها
     * @param teacherId اگر مقدار داشته باشد، فقط کلاس‌های این معلم؛
     *                  {@code null} یعنی بدون محدودیت معلم
     */
    @Query("""
            select c from ClassRoom c
            where c.deletedAt is null
              and (:pattern is null or lower(c.name) like :pattern escape '\\')
              and (:status is null or c.status = :status)
              and (:teacherId is null or c.homeroomTeacherId = :teacherId)
            """)
    Page<ClassRoom> findForListing(@Param("pattern") String pattern,
                                   @Param("status") ClassRoomStatus status,
                                   @Param("teacherId") UUID teacherId,
                                   Pageable pageable);

    /**
     * شناسه کلاس‌هایی که با این نام در این سال تحصیلی تعارض دارند.
     *
     * <p>{@code coalesce(academicYear, '')} عمدی است: دو کلاس هم‌نام که
     * هر دو سال تحصیلی خالی دارند باید تعارض بگیرند، ولی در SQL
     * {@code NULL = NULL} هرگز درست نیست. همین منطق در ایندکس یکتای
     * {@code uk_classrooms_name_year} هم تکرار شده — سرویس اینجا
     * بررسی می‌کند تا پیام فارسی بدهد، و آن ایندکس تضمین می‌کند
     * در مسابقه همزمانی داده خراب نشود.
     *
     * @return شناسه رکوردهای متعارض، تا سرویس بتواند خودِ رکورد را کنار بگذارد
     */
    @Query("""
            select c.id from ClassRoom c
            where c.deletedAt is null
              and lower(c.name) = lower(:name)
              and coalesce(c.academicYear, '') = coalesce(:academicYear, '')
            """)
    List<UUID> findConflictingIds(@Param("name") String name,
                                  @Param("academicYear") String academicYear);

    /** تعداد کلاس‌های فعالی که به این درس ارجاع می‌دهند. */
    long countBySubjectIdAndDeletedAtIsNull(UUID subjectId);

    /**
     * شناسه کلاس‌های قابل‌دسترس برای فهرست‌های ماژول‌های دیگر.
     *
     * @param teacherId {@code null} یعنی همه کلاس‌های مستأجر (مالک/مدیر)؛
     *                  در غیر این صورت فقط کلاس‌های این معلم سرکلاس
     */
    @Query("""
            select c.id from ClassRoom c
            where c.deletedAt is null
              and (:teacherId is null or c.homeroomTeacherId = :teacherId)
            """)
    List<UUID> findAccessibleIds(@Param("teacherId") UUID teacherId);
}
