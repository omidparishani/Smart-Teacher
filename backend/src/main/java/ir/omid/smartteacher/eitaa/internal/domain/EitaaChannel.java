package ir.omid.smartteacher.eitaa.internal.domain;

import ir.omid.smartteacher.tenant.SoftDeletableTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/** کانال ایتا — فقط خروجی. */
@Entity
@Table(name = "eitaa_channels")
public class EitaaChannel extends SoftDeletableTenantEntity {

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "eitaa_chat_id", nullable = false, length = 100)
    private String eitaaChatId;

    @Column(name = "title", length = 250)
    private String title;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "subscriber_count")
    private Integer subscriberCount;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "can_post", nullable = false)
    private boolean canPost = true;

    protected EitaaChannel() {
    }

    public EitaaChannel(String eitaaChatId, String title) {
        this.eitaaChatId = eitaaChatId;
        this.title = title;
    }

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public String getEitaaChatId() { return eitaaChatId; }
    public void setEitaaChatId(String eitaaChatId) { this.eitaaChatId = eitaaChatId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Integer getSubscriberCount() { return subscriberCount; }
    public void setSubscriberCount(Integer subscriberCount) {
        this.subscriberCount = subscriberCount;
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isCanPost() { return canPost; }
    public void setCanPost(boolean canPost) { this.canPost = canPost; }
}
