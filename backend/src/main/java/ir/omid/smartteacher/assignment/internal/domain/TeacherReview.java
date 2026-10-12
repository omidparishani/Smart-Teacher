package ir.omid.smartteacher.assignment.internal.domain;

import ir.omid.smartteacher.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * بازبینی معلم — جدول {@code teacher_reviews} (Append-Only).
 *
 * <p>هیچ متد setter برای بازنویسی «تاریخچه» نیست؛ فقط ساخت ردیف تازه.
 */
@Entity
@Table(name = "teacher_reviews")
public class TeacherReview extends TenantAwareEntity {

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "reviewer_user_id", nullable = false)
    private UUID reviewerUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private ReviewAction action;

    @Column(name = "previous_score", precision = 8, scale = 2)
    private BigDecimal previousScore;

    @Column(name = "new_score", precision = 8, scale = 2)
    private BigDecimal newScore;

    @Column(name = "comment")
    private String comment;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt = Instant.now();

    protected TeacherReview() {
    }

    public TeacherReview(UUID submissionId,
                         UUID reviewerUserId,
                         ReviewAction action,
                         BigDecimal previousScore,
                         BigDecimal newScore,
                         String comment) {
        this.submissionId = submissionId;
        this.reviewerUserId = reviewerUserId;
        this.action = action;
        this.previousScore = previousScore;
        this.newScore = newScore;
        this.comment = comment;
        this.reviewedAt = Instant.now();
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public UUID getReviewerUserId() {
        return reviewerUserId;
    }

    public ReviewAction getAction() {
        return action;
    }

    public BigDecimal getPreviousScore() {
        return previousScore;
    }

    public BigDecimal getNewScore() {
        return newScore;
    }

    public String getComment() {
        return comment;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
