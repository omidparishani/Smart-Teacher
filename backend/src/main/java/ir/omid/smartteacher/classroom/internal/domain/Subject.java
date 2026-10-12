package ir.omid.smartteacher.classroom.internal.domain;

import ir.omid.smartteacher.tenant.SoftDeletableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * درس — مثل «ریاضی» یا «علوم» ({@code docs/database.md} بخش ۳).
 *
 * <p><b>چرا جدول جدا و نه یک ستون متنی روی کلاس؟</b> چون «ریاضی» در یک
 * مدرسه <i>یک</i> چیز است که ده کلاس به آن اشاره می‌کنند. با ستون متنی،
 * نام درس ده بار تکرار می‌شد، غلط املایی در یکی از ده‌تا باقی می‌ماند،
 * و «همه کلاس‌های ریاضی» به جست‌وجوی رشته‌ای تبدیل می‌شد.
 *
 * <p>مستأجر این موجودیت را از {@link SoftDeletableTenantEntity} می‌گیرد،
 * پس فیلتر خودکار Hibernate روی هر کوئری‌اش اعمال می‌شود.
 *
 * <p><b>⚠️ هیچ ارتباط JPA (ManyToOne/OneToMany) وجود ندارد.</b> ارجاع
 * کلاس به درس یک ستون {@code UUID} ساده است. دلیل کاملش در Javadoc
 * {@link ir.omid.smartteacher.tenant.TenantMember} آمده: عبور از مرز
 * ماژول با ارجاع مستقیم موجودیت، قید معماری Spring Modulith را می‌شکند
 * و Lazy Loading را به منطق کسب‌وکار نشت می‌دهد.
 */
@Entity
@Table(name = "subjects")
public class Subject extends SoftDeletableTenantEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * کد درس — اختیاری و در محدوده یک مستأجر یکتا.
     *
     * <p>یکتا نیست در سطح جهانی: دو مدرسه مختلف می‌توانند هر دو
     * {@code MATH-5} داشته باشند. دلیلش در migration V8 آمده.
     */
    @Column(name = "code", length = 50)
    private String code;

    /** پایه تحصیلی — مثل «پنجم». اختیاری چون درس می‌تواند چندپایه باشد. */
    @Column(name = "grade_level", length = 50)
    private String gradeLevel;

    /** سازنده بدون آرگومان برای JPA. */
    protected Subject() {
    }

    public Subject(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(String gradeLevel) {
        this.gradeLevel = gradeLevel;
    }
}
