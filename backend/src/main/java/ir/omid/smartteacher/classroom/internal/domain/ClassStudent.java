package ir.omid.smartteacher.classroom.internal.domain;

import ir.omid.smartteacher.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * ثبت‌نام دانش‌آموز در کلاس — جدول {@code class_students}
 * ({@code docs/database.md} بخش ۳).
 *
 * <p><b>⚠️ چرا از {@link TenantAwareEntity} ارث می‌برد و نه
 * {@code SoftDeletableTenantEntity}؟</b> چون ثبت‌نام یک <i>رابطه جاری</i>
 * است، نه یک دارایی. برداشتن دانش‌آموز از کلاس یعنی خودِ رابطه تمام شده؛
 * حذف نرم اینجا یعنی هر کوئری باید {@code deleted_at} را فیلتر کند برای
 * چیزی که تاریخچه‌اش در این جدول معنا ندارد. مهاجرت {@code V10} هم ستون
 * {@code deleted_at} ندارد — این کلاس باید با آن هم‌خوان باشد.
 *
 * <p><b>⚠️ هیچ ارتباط JPA وجود ندارد</b> — {@code classroomId} و
 * {@code studentId} ستون‌های {@code UUID} ساده‌اند. نام دانش‌آموز از
 * {@code StudentDirectory} می‌آید، نه از join.
 */
@Entity
@Table(name = "class_students")
public class ClassStudent extends TenantAwareEntity {

    @Column(name = "classroom_id", nullable = false, updatable = false)
    private UUID classroomId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    /** شماره حضور — اختیاری؛ در دو کلاس مختلف می‌تواند یکی باشد. */
    @Column(name = "roll_number", length = 20)
    private String rollNumber;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt;

    /** سازنده بدون آرگومان برای JPA. */
    protected ClassStudent() {
    }

    public ClassStudent(UUID classroomId, UUID studentId) {
        this.classroomId = classroomId;
        this.studentId = studentId;
    }

    @PrePersist
    private void assignEnrolledAtIfAbsent() {
        if (enrolledAt == null) {
            enrolledAt = Instant.now();
        }
    }

    public UUID getClassroomId() {
        return classroomId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }
}
