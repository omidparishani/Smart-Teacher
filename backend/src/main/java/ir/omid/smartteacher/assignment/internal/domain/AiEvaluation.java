package ir.omid.smartteacher.assignment.internal.domain;

import ir.omid.smartteacher.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * ارزیابی AI یک ارسال — جدول {@code ai_evaluations} (یک ردیف به ازای ارسال).
 */
@Entity
@Table(name = "ai_evaluations")
public class AiEvaluation extends TenantAwareEntity {

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EvaluationStatus status = EvaluationStatus.PENDING;

    @Column(name = "ai_score", precision = 8, scale = 2)
    private BigDecimal aiScore;

    @Column(name = "is_correct")
    private Boolean correct;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mistakes", columnDefinition = "jsonb")
    private String mistakes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strengths", columnDefinition = "jsonb")
    private String strengths;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weaknesses", columnDefinition = "jsonb")
    private String weaknesses;

    @Column(name = "feedback")
    private String feedback;

    @Column(name = "teacher_note")
    private String teacherNote;

    @Column(name = "confidence", precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(name = "ai_usage_id")
    private UUID aiUsageId;

    @Column(name = "prompt_version_id")
    private UUID promptVersionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private String rawResponse;

    protected AiEvaluation() {
    }

    public AiEvaluation(UUID submissionId) {
        this.submissionId = submissionId;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public EvaluationStatus getStatus() {
        return status;
    }

    public void setStatus(EvaluationStatus status) {
        this.status = status;
    }

    public BigDecimal getAiScore() {
        return aiScore;
    }

    public void setAiScore(BigDecimal aiScore) {
        this.aiScore = aiScore;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public String getMistakes() {
        return mistakes;
    }

    public void setMistakes(String mistakes) {
        this.mistakes = mistakes;
    }

    public String getStrengths() {
        return strengths;
    }

    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }

    public String getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(String weaknesses) {
        this.weaknesses = weaknesses;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getTeacherNote() {
        return teacherNote;
    }

    public void setTeacherNote(String teacherNote) {
        this.teacherNote = teacherNote;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public UUID getAiUsageId() {
        return aiUsageId;
    }

    public void setAiUsageId(UUID aiUsageId) {
        this.aiUsageId = aiUsageId;
    }

    public UUID getPromptVersionId() {
        return promptVersionId;
    }

    public void setPromptVersionId(UUID promptVersionId) {
        this.promptVersionId = promptVersionId;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
}
