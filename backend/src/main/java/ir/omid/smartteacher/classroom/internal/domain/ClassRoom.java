package ir.omid.smartteacher.classroom.internal.domain;

import ir.omid.smartteacher.tenant.SoftDeletableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * کلاس — مثل «پنجم الف» ({@code docs/database.md} بخش ۳).
 *
 * <p>مستأجر این موجودیت را از {@link SoftDeletableTenantEntity} می‌گیرد،
 * پس هم فیلتر خودکار مستأجر دارد و هم حذف نرم.
 *
 * <p><b>🔴 دو ارجاع، دو رفتار متفاوت — و تفاوتشان اتفاقی نیست:</b>
 * <ul>
 *   <li>{@link #subjectId} — به جدول {@code subjects} همان مستأجر.
 *       یک <b>کلید خارجی ترکیبی</b> روی {@code (tenant_id, subject_id)}
 *       در دیتابیس تضمین می‌کند ارجاع از مرز مستأجر عبور نکند. یعنی
 *       حتی اگر لایه سرویس باگ داشته باشد، دیتابیس جلوی کلاس مدرسه الف
 *       که به درس مدرسه ب وصل شده را می‌گیرد.</li>
 *   <li>{@link #homeroomTeacherId} — به {@code users} که جدول
 *       سطح <i>پلتفرم</i> است و ستون {@code tenant_id} ندارد. پس اینجا
 *       کلید خارجی ترکیبی ممکن نیست و نمی‌شود. اعتبارسنجی «آیا این
 *       کاربر عضو همین مستأجر است؟» کار لایه سرویس است.</li>
 * </ul>
 *
 * <p><b>⚠️ چرا این کلاس در دامنه {@code classroom} است و {@code subject}
 * هم همین‌جا؟</b> چون {@code docs/architecture.md} بخش ۴ ماژولی به نام
 * {@code subject} ندارد؛ درس بخشی از ماژول «کلاس‌ها و ثبت‌نام» است.
 * ساختن ماژول تازه یعنی ماژول تازه‌ای که هیچ‌کس مصرفش نمی‌کند.
 *
 * <p><b>⚠️ هیچ ارتباط JPA وجود ندارد</b> — {@code subjectId} و
 * {@code homeroomTeacherId} ستون‌های {@code UUID} ساده‌اند.
 */
@Entity
@Table(name = "classrooms")
public class ClassRoom extends SoftDeletableTenantEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** پایه تحصیلی — مثل «پنجم». */
    @Column(name = "grade_level", length = 50)
    private String gradeLevel;

    /** سال تحصیلی — مثل «1404-1405». قالب آزاد است و اعتبارسنجی نمی‌شود. */
    @Column(name = "academic_year", length = 20)
    private String academicYear;

    /** درس این کلاس. اختیاری — کلاس می‌تواند چنددرس باشد یا بی‌درس. */
    @Column(name = "subject_id")
    private UUID subjectId;

    /**
     * معلمِ سرکلاس.
     *
     * <p><b>⚠️ در فاز ۳ فقط خواندنی است از دید API:</b> سرویس همیشه
     * همین را برابر کاربر جاری می‌گذارد. دلیلش یک نشت واقعی است: اگر
     * معلم می‌توانست شناسه معلم دیگری را بفرستد و ما آن را بدون بررسی
     * عضویت می‌پذیرفتیم، آن کاربرِ بیرون از مستأجر این کلاس را در
     * فهرست خودش می‌دید — چون فهرست بر اساس همین ستون فیلتر می‌شود.
     * انتقال کلاس بین معلمان به پنل مدیریت (فاز ۴) می‌ماند، جایی که
     * فهرست اعضای مستأجر در دسترس است و می‌توان عضویت را بررسی کرد.
     */
    @Column(name = "homeroom_teacher_id")
    private UUID homeroomTeacherId;

    /** ظرفیت کلاس. اختیاری. */
    @Column(name = "capacity")
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ClassRoomStatus status = ClassRoomStatus.ACTIVE;

    /** سازنده بدون آرگومان برای JPA. */
    protected ClassRoom() {
    }

    public ClassRoom(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(String gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(UUID subjectId) {
        this.subjectId = subjectId;
    }

    public UUID getHomeroomTeacherId() {
        return homeroomTeacherId;
    }

    public void setHomeroomTeacherId(UUID homeroomTeacherId) {
        this.homeroomTeacherId = homeroomTeacherId;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public ClassRoomStatus getStatus() {
        return status;
    }

    public void setStatus(ClassRoomStatus status) {
        this.status = status;
    }
}
