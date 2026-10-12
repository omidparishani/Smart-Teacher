package ir.omid.smartteacher.exam.internal.domain;

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

/** سؤال آزمون. */
@Entity
@Table(name = "exam_questions")
public class ExamQuestion extends TenantAwareEntity {

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private QuestionType type;

    @Column(name = "question_text", nullable = false)
    private String questionText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb")
    private String options;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "score", nullable = false, precision = 10, scale = 2)
    private BigDecimal score = BigDecimal.ONE;

    @Column(name = "difficulty", length = 10)
    private String difficulty;

    @Column(name = "explanation")
    private String explanation;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated;

    protected ExamQuestion() {
    }

    public ExamQuestion(UUID examId, QuestionType type, String questionText) {
        this.examId = examId;
        this.type = type;
        this.questionText = questionText;
    }

    public UUID getExamId() { return examId; }
    public QuestionType getType() { return type; }
    public void setType(QuestionType type) { this.type = type; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public boolean isAiGenerated() { return aiGenerated; }
    public void setAiGenerated(boolean aiGenerated) { this.aiGenerated = aiGenerated; }
}
