package ir.omid.smartteacher.exam.internal.domain;

import ir.omid.smartteacher.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** تلاش دانش‌آموز در یک آزمون. */
@Entity
@Table(name = "exam_attempts")
public class ExamAttempt extends TenantAwareEntity {

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "score", precision = 10, scale = 2)
    private BigDecimal score;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber = 1;

    protected ExamAttempt() {
    }

    public ExamAttempt(UUID examId, UUID studentId, int attemptNumber) {
        this.examId = examId;
        this.studentId = studentId;
        this.attemptNumber = attemptNumber;
    }

    public UUID getExamId() { return examId; }
    public UUID getStudentId() { return studentId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public AttemptStatus getStatus() { return status; }
    public void setStatus(AttemptStatus status) { this.status = status; }
    public int getAttemptNumber() { return attemptNumber; }
}
