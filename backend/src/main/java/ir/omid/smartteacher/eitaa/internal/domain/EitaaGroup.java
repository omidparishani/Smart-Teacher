package ir.omid.smartteacher.eitaa.internal.domain;

import ir.omid.smartteacher.tenant.SoftDeletableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** گروه ایتا متصل به مستأجر — docs/database.md بخش ۷. */
@Entity
@Table(name = "eitaa_groups")
public class EitaaGroup extends SoftDeletableTenantEntity {

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "eitaa_chat_id", nullable = false, length = 100)
    private String eitaaChatId;

    @Column(name = "title", length = 250)
    private String title;

    @Column(name = "type", nullable = false, length = 20)
    private String type = "GROUP";

    @Column(name = "member_count")
    private Integer memberCount;

    @Column(name = "classroom_id")
    private UUID classroomId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "can_send_messages", nullable = false)
    private boolean canSendMessages = true;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    protected EitaaGroup() {
    }

    public EitaaGroup(String eitaaChatId, String title) {
        this.eitaaChatId = eitaaChatId;
        this.title = title;
    }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public String getEitaaChatId() { return eitaaChatId; }
    public void setEitaaChatId(String eitaaChatId) { this.eitaaChatId = eitaaChatId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
    public UUID getClassroomId() { return classroomId; }
    public void setClassroomId(UUID classroomId) { this.classroomId = classroomId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isCanSendMessages() { return canSendMessages; }
    public void setCanSendMessages(boolean canSendMessages) { this.canSendMessages = canSendMessages; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
}
