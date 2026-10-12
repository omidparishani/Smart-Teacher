package ir.omid.smartteacher.exam.internal.domain;

import ir.omid.smartteacher.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** پاسخ یک سؤال در یک تلاش. */
@Entity
@Table(name = "exam_answers")
public class ExamAnswer extends TenantAwareEntity {

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "answer_text")
    private String answerText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_options", columnDefinition = "jsonb")
    private String selectedOptions;

    @Column(name = "awarded_score", precision = 10, scale = 2)
    private BigDecimal awardedScore;

    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "ai_graded", nullable = false)
    private boolean aiGraded;

    @Column(name = "ai_feedback")
    private String aiFeedback;

    protected ExamAnswer() {
    }

    public ExamAnswer(UUID attemptId, UUID questionId) {
        this.attemptId = attemptId;
        this.questionId = questionId;
    }

    public UUID getAttemptId() { return attemptId; }
    public UUID getQuestionId() { return questionId; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public String getSelectedOptions() { return selectedOptions; }
    public void setSelectedOptions(String selectedOptions) { this.selectedOptions = selectedOptions; }
    public BigDecimal getAwardedScore() { return awardedScore; }
    public void setAwardedScore(BigDecimal awardedScore) { this.awardedScore = awardedScore; }
    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }
    public boolean isAiGraded() { return aiGraded; }
    public void setAiGraded(boolean aiGraded) { this.aiGraded = aiGraded; }
    public String getAiFeedback() { return aiFeedback; }
    public void setAiFeedback(String aiFeedback) { this.aiFeedback = aiFeedback; }
}
