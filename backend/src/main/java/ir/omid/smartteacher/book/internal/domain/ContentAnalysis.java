package ir.omid.smartteacher.book.internal.domain;

import ir.omid.smartteacher.tenant.SoftDeletableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * تحلیل ذخیره‌شده صفحه/فصل/کتاب — جدول {@code content_analyses}.
 */
@Entity
@Table(name = "content_analyses")
public class ContentAnalysis extends SoftDeletableTenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private AnalysisTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_type", nullable = false, length = 30)
    private AnalysisType analysisType;

    @Column(name = "result_text")
    private String resultText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_json", columnDefinition = "jsonb")
    private String resultJson;

    @Column(name = "ai_usage_id")
    private UUID aiUsageId;

    @Column(name = "prompt_version_id")
    private UUID promptVersionId;

    protected ContentAnalysis() {
    }

    public ContentAnalysis(AnalysisTargetType targetType,
                           UUID targetId,
                           AnalysisType analysisType,
                           String resultText) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.analysisType = analysisType;
        this.resultText = resultText;
    }

    public AnalysisTargetType getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public AnalysisType getAnalysisType() {
        return analysisType;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText = resultText;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
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
}
