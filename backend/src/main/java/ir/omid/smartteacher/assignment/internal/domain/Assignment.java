package ir.omid.smartteacher.assignment.internal.domain;

import ir.omid.smartteacher.tenant.SoftDeletableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * تکلیف — جدول {@code assignments} ({@code docs/database.md} بخش ۵).
 *
 * <p>متعلق به کلاس است؛ دسترسی معلم از طریق
 * {@link ir.omid.smartteacher.classroom.ClassRoomAccess#requireAccessible}
 * اعمال می‌شود، نه فقط فیلتر مستأجر.
 *
 * <p><b>⚠️ حذف نرم است</b> — ارسال‌ها و ارزیابی‌های بعدی به همین شناسه
 * گره می‌خورند.
 */
@Entity
@Table(name = "assignments")
public class Assignment extends SoftDeletableTenantEntity {

    @Column(name = "classroom_id", nullable = false)
    private UUID classroomId;

    @Column(name = "book_id")
    private UUID bookId;

    @Column(name = "page_from")
    private Integer pageFrom;

    @Column(name = "page_to")
    private Integer pageTo;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AssignmentType type = AssignmentType.HOMEWORK;

    @Column(name = "max_score", precision = 8, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "due_at")
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.DRAFT;

    @Column(name = "ai_evaluation_enabled", nullable = false)
    private boolean aiEvaluationEnabled;

    @Column(name = "prompt_version_id")
    private UUID promptVersionId;

    @Column(name = "eitaa_chat_id", length = 100)
    private String eitaaChatId;

    @Column(name = "eitaa_message_id", length = 100)
    private String eitaaMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "eitaa_send_status", length = 20)
    private EitaaSendStatus eitaaSendStatus;

    protected Assignment() {
    }

    public Assignment(String title, UUID classroomId) {
        this.title = title;
        this.classroomId = classroomId;
    }

    public UUID getClassroomId() {
        return classroomId;
    }

    public void setClassroomId(UUID classroomId) {
        this.classroomId = classroomId;
    }

    public UUID getBookId() {
        return bookId;
    }

    public void setBookId(UUID bookId) {
        this.bookId = bookId;
    }

    public Integer getPageFrom() {
        return pageFrom;
    }

    public void setPageFrom(Integer pageFrom) {
        this.pageFrom = pageFrom;
    }

    public Integer getPageTo() {
        return pageTo;
    }

    public void setPageTo(Integer pageTo) {
        this.pageTo = pageTo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AssignmentType getType() {
        return type;
    }

    public void setType(AssignmentType type) {
        this.type = type;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public AssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssignmentStatus status) {
        this.status = status;
    }

    public boolean isAiEvaluationEnabled() {
        return aiEvaluationEnabled;
    }

    public void setAiEvaluationEnabled(boolean aiEvaluationEnabled) {
        this.aiEvaluationEnabled = aiEvaluationEnabled;
    }

    public UUID getPromptVersionId() {
        return promptVersionId;
    }

    public void setPromptVersionId(UUID promptVersionId) {
        this.promptVersionId = promptVersionId;
    }

    public String getEitaaChatId() {
        return eitaaChatId;
    }

    public void setEitaaChatId(String eitaaChatId) {
        this.eitaaChatId = eitaaChatId;
    }

    public String getEitaaMessageId() {
        return eitaaMessageId;
    }

    public void setEitaaMessageId(String eitaaMessageId) {
        this.eitaaMessageId = eitaaMessageId;
    }

    public EitaaSendStatus getEitaaSendStatus() {
        return eitaaSendStatus;
    }

    public void setEitaaSendStatus(EitaaSendStatus eitaaSendStatus) {
        this.eitaaSendStatus = eitaaSendStatus;
    }
}
