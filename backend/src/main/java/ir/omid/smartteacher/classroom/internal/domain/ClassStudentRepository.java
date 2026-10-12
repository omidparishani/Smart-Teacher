package ir.omid.smartteacher.classroom.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * دسترسی به ثبت‌نام‌های کلاس.
 *
 * <p>فیلتر مستأجر خودکار است ({@code @TenantId}). حذف نرم ندارد — جدول
 * {@code class_students} ستون {@code deleted_at} ندارد.
 */
public interface ClassStudentRepository extends JpaRepository<ClassStudent, UUID> {

    /** همه ثبت‌نام‌های یک کلاس، به ترتیب زمان ثبت‌نام. */
    List<ClassStudent> findByClassroomIdOrderByEnrolledAtAsc(UUID classroomId);

    /** یک ثبت‌نام مشخص در یک کلاس. */
    Optional<ClassStudent> findByClassroomIdAndStudentId(UUID classroomId, UUID studentId);

    /** آیا این دانش‌آموز در این کلاس ثبت‌نام شده؟ */
    boolean existsByClassroomIdAndStudentId(UUID classroomId, UUID studentId);

    /**
     * تعداد ردیف‌های ثبت‌نام یک کلاس.
     *
     * <p>⚠️ این عدد ممکن است از تعداد دانش‌آموزان <i>قابل‌نمایش</i>
     * بیشتر باشد: دانش‌آموز حذف‌نرم‌شده هنوز ردیف دارد ولی در فهرست
     * دیده نمی‌شود. سرویس برای ظرفیت و داشبورد، روی شناسه‌های فعال
     * فیلتر می‌کند — نه روی این شمارش خام.
     */
    long countByClassroomId(UUID classroomId);
}
