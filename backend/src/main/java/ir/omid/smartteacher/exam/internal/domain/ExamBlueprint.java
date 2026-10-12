package ir.omid.smartteacher.exam.internal.domain;

import ir.omid.smartteacher.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** طرح تولید آزمون با AI — جدول exam_blueprints. */
@Entity
@Table(name = "exam_blueprints")
public class ExamBlueprint extends TenantAwareEntity {

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(name = "page_from", nullable = false)
    private int pageFrom;

    @Column(name = "page_to", nullable = false)
    private int pageTo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "difficulty_distribution", columnDefinition = "jsonb")
    private String difficultyDistribution;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_type_distribution", columnDefinition = "jsonb")
    private String questionTypeDistribution;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "ai_usage_id")
    private UUID aiUsageId;

    protected ExamBlueprint() {
    }

    public ExamBlueprint(UUID examId, UUID bookId, int pageFrom, int pageTo) {
        this.examId = examId;
        this.bookId = bookId;
        this.pageFrom = pageFrom;
        this.pageTo = pageTo;
    }

    public UUID getExamId() { return examId; }
    public UUID getBookId() { return bookId; }
    public int getPageFrom() { return pageFrom; }
    public int getPageTo() { return pageTo; }
    public String getDifficultyDistribution() { return difficultyDistribution; }
    public void setDifficultyDistribution(String difficultyDistribution) {
        this.difficultyDistribution = difficultyDistribution;
    }
    public String getQuestionTypeDistribution() { return questionTypeDistribution; }
    public void setQuestionTypeDistribution(String questionTypeDistribution) {
        this.questionTypeDistribution = questionTypeDistribution;
    }
    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
    public UUID getAiUsageId() { return aiUsageId; }
    public void setAiUsageId(UUID aiUsageId) { this.aiUsageId = aiUsageId; }
}
