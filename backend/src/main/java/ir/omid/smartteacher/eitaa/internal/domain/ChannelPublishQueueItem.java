package ir.omid.smartteacher.eitaa.internal.domain;

import ir.omid.smartteacher.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** آیتم صف انتشار کانال — متن با Mock-AI تولید و در صف می‌ماند. */
@Entity
@Table(name = "channel_publish_queue")
public class ChannelPublishQueueItem extends TenantAwareEntity {

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(name = "topic", nullable = false, length = 500)
    private String topic;

    @Column(name = "content_text")
    private String contentText;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "error_message")
    private String errorMessage;

    protected ChannelPublishQueueItem() {
    }

    public ChannelPublishQueueItem(UUID channelId, String topic, String contentText) {
        this.channelId = channelId;
        this.topic = topic;
        this.contentText = contentText;
        this.status = "PENDING";
    }

    public UUID getChannelId() { return channelId; }
    public String getTopic() { return topic; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
