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
 * ارسال پاسخ دانش‌آموز — جدول {@code assignment_submissions}.
 *
 * <p>حذف نرم ندارد. {@code submitted_by_user_id} برای قاعده
 * «دانش‌آموز فقط پاسخ خودش را می‌بیند» نگه داشته می‌شود تا وقتی لینک
 * user↔student کامل شود، همان فیلد کافی باشد.
 */
@Entity
@Table(name = "assignment_submissions")
public class AssignmentSubmission extends TenantAwareEntity {

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "answer_text")
    private String answerText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    @Column(name = "submitted_by_user_id")
    private UUID submittedByUserId;

    @Column(name = "final_score", precision = 8, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "teacher_overrode_ai", nullable = false)
    private boolean teacherOverrodeAi;

    protected AssignmentSubmission() {
    }

    public AssignmentSubmission(UUID assignmentId, UUID studentId, String answerText) {
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.answerText = answerText;
    }

    public UUID getAssignmentId() {
        return assignmentId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public UUID getSubmittedByUserId() {
        return submittedByUserId;
    }

    public void setSubmittedByUserId(UUID submittedByUserId) {
        this.submittedByUserId = submittedByUserId;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    public boolean isTeacherOverrodeAi() {
        return teacherOverrodeAi;
    }

    public void setTeacherOverrodeAi(boolean teacherOverrodeAi) {
        this.teacherOverrodeAi = teacherOverrodeAi;
    }
}
