package ir.omid.smartteacher.student.internal.domain;

import ir.omid.smartteacher.tenant.SoftDeletableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * دانش‌آموز — {@code docs/database.md} بخش ۳.
 *
 * <p>مستأجر و حذف نرم را از {@link SoftDeletableTenantEntity} می‌گیرد.
 *
 * <p><b>⚠️ چرا {@code teacherId} یا {@code classroomId} اینجا نیست؟</b>
 * چون دانش‌آموز به <i>کلاس</i> تعلق ندارد، به <i>مدرسه</i> تعلق دارد —
 * همان چیزی که ERD هم می‌گوید ({@code TENANTS ||--o{ STUDENTS}). یک
 * دانش‌آموز می‌تواند هم‌زمان در چند کلاس باشد (ریاضی و علوم جدا)، پس
 * رابطه در جدول اتصالی {@code class_students} مدل شده، نه با یک ستون
 * اینجا. اگر کلاس را روی دانش‌آموز می‌گذاشتیم، ثبت‌نام در کلاس دوم
 * یعنی بازنویسی کلاس اول.
 *
 * <p>⚠️ هیچ ارتباط JPA وجود ندارد — هیچ ستونی موجودیت دیگر را نگه
 * نمی‌دارد.
 */
@Entity
@Table(name = "students")
public class Student extends SoftDeletableTenantEntity {

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /**
     * کد ملی — یکتا در مستأجر، بین دانش‌آموزان فعال.
     *
     * <p>⚠️ اختیاری است. اجباری‌کردنش یعنی دانش‌آموزی که هنوز کد ملی
     * نگرفته نتواند ثبت شود، و این در یک محصول واقعی غیرقابل قبول است.
     */
    @Column(name = "national_code", length = 20)
    private String nationalCode;

    @Column(name = "guardian_name", length = 150)
    private String guardianName;

    @Column(name = "guardian_mobile", length = 20)
    private String guardianMobile;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StudentStatus status = StudentStatus.ACTIVE;

    /**
     * داده اضافی با شکل نامشخص، به‌صورت JSONB.
     *
     * <p>⚠️ به‌صورت {@code String} نگاشته شده، همان الگوی
     * {@code Tenant.settings}: اگر {@code Map<String,Object>}
     * می‌گذاشتیم، به پیکربندی Jackson گره می‌خورد. برای فاز ۳ که فقط
     * نگه‌داری لازم است، {@code String} کم‌ریسک‌تر است.
     *
     * <p>⚠️ عمداً هیچ فیلد پرکاربردی داخل این JSON نمی‌رود: چیزی که در
     * کوئری و ایندکس لازم است باید ستون نوع‌دار باشد، وگرنه جست‌وجو
     * رویش پیمایش کامل می‌شود و هیچ ایندکسی کمکش نمی‌کند.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;

    /** سازنده بدون آرگومان برای JPA. */
    protected Student() {
    }

    public Student(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getNationalCode() {
        return nationalCode;
    }

    public void setNationalCode(String nationalCode) {
        this.nationalCode = nationalCode;
    }

    public String getGuardianName() {
        return guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = guardianName;
    }

    public String getGuardianMobile() {
        return guardianMobile;
    }

    public void setGuardianMobile(String guardianMobile) {
        this.guardianMobile = guardianMobile;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    /**
     * ⚠️ بازنویسی عمدی: {@code toString} پیش‌فرض فقط شناسه و مستأجر را
     * می‌دهد، ولی اگر روزی کسی فیلد اضافه کند و بازنویسی را بردارد،
     * نام و <b>موبایل ولی</b> ممکن است در لاگ بیفتد. اینجا صریح گفته
     * می‌شود که چه چیزی هرگز نباید لاگ شود.
     */
    @Override
    public String toString() {
        return "Student{id=" + getId() + ", tenantId=" + getTenantId() + '}';
    }
}
