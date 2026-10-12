package ir.omid.smartteacher.ai.internal.domain;

import ir.omid.smartteacher.common.jpa.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * یک نسخه از متن پرامپت — {@code prompt_versions}.
 */
@Entity
@Table(name = "prompt_versions")
public class PromptVersionEntity extends AuditableEntity {

    @Column(name = "prompt_id", nullable = false)
    private UUID promptId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "user_prompt", nullable = false, columnDefinition = "TEXT")
    private String userPrompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variables", nullable = false)
    private String variables = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_schema")
    private String responseSchema;

    @Column(name = "temperature", precision = 4, scale = 2)
    private BigDecimal temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PromptVersionStatus status = PromptVersionStatus.DRAFT;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "change_note")
    private String changeNote;

    protected PromptVersionEntity() {
    }

    public PromptVersionEntity(UUID promptId, int versionNumber,
                               String systemPrompt, String userPrompt) {
        this.promptId = promptId;
        this.versionNumber = versionNumber;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
    }

    public void publish(UUID actorId) {
        this.status = PromptVersionStatus.PUBLISHED;
        this.publishedBy = actorId;
        this.publishedAt = Instant.now();
    }

    public void archive() {
        this.status = PromptVersionStatus.ARCHIVED;
    }

    public UUID getPromptId() {
        return promptId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public String getVariables() {
        return variables;
    }

    public void setVariables(String variables) {
        this.variables = variables == null || variables.isBlank() ? "{}" : variables;
    }

    public String getResponseSchema() {
        return responseSchema;
    }

    public void setResponseSchema(String responseSchema) {
        this.responseSchema = responseSchema;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public PromptVersionStatus getStatus() {
        return status;
    }

    public void setStatus(PromptVersionStatus status) {
        this.status = status;
    }

    public UUID getPublishedBy() {
        return publishedBy;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getChangeNote() {
        return changeNote;
    }

    public void setChangeNote(String changeNote) {
        this.changeNote = changeNote;
    }
}
