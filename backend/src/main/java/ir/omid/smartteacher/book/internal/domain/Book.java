package ir.omid.smartteacher.book.internal.domain;

import ir.omid.smartteacher.tenant.SoftDeletableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * کتاب درسی — جدول {@code books} ({@code docs/database.md} بخش ۴).
 *
 * <p>دارایی <i>مستأجر</i> است (مثل دانش‌آموز)، نه مالکیت فردی معلم:
 * همه کارمندان مستأجر کتاب‌های مدرسه را می‌بینند. فیلتر
 * {@code @TenantId} تمام مرز لازم است.
 */
@Entity
@Table(name = "books")
public class Book extends SoftDeletableTenantEntity {

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "publisher", length = 150)
    private String publisher;

    @Column(name = "grade_level", length = 50)
    private String gradeLevel;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookStatus status = BookStatus.DRAFT;

    @Column(name = "cover_file_id")
    private UUID coverFileId;

    protected Book() {
    }

    public Book(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(String gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(UUID subjectId) {
        this.subjectId = subjectId;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public BookStatus getStatus() {
        return status;
    }

    public void setStatus(BookStatus status) {
        this.status = status;
    }

    public UUID getCoverFileId() {
        return coverFileId;
    }

    public void setCoverFileId(UUID coverFileId) {
        this.coverFileId = coverFileId;
    }
}
