package ir.omid.smartteacher.eitaa.internal.domain;

import ir.omid.smartteacher.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** پیام زمان‌بندی‌شده خروجی — بدون inbound. */
@Entity
@Table(name = "scheduled_messages")
public class ScheduledMessage extends TenantAwareEntity {

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "content_text", nullable = false)
    private String contentText;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "recurrence", nullable = false, length = 20)
    private String recurrence = "NONE";

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "eitaa_message_id", length = 50)
    private String eitaaMessageId;

    @Column(name = "error_message")
    private String errorMessage;

    protected ScheduledMessage() {
    }

    public ScheduledMessage(String targetType, UUID targetId, String contentText, Instant scheduledAt) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.contentText = contentText;
        this.scheduledAt = scheduledAt;
    }

    public String getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public String getContentText() { return contentText; }
    public Instant getScheduledAt() { return scheduledAt; }
    public String getRecurrence() { return recurrence; }
    public void setRecurrence(String recurrence) { this.recurrence = recurrence; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public String getEitaaMessageId() { return eitaaMessageId; }
    public void setEitaaMessageId(String eitaaMessageId) { this.eitaaMessageId = eitaaMessageId; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
