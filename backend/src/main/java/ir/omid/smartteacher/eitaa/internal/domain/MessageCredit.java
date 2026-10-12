package ir.omid.smartteacher.eitaa.internal.domain;

import ir.omid.smartteacher.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** اعتبار پیام رایگان برنامک ایتا (۵۰٬۰۰۰) — اختیاری. */
@Entity
@Table(name = "message_credits")
public class MessageCredit extends TenantAwareEntity {

    @Column(name = "balance", nullable = false)
    private int balance = 50_000;

    @Column(name = "free_quota", nullable = false)
    private int freeQuota = 50_000;

    protected MessageCredit() {
    }

    public MessageCredit(int balance, int freeQuota) {
        this.balance = balance;
        this.freeQuota = freeQuota;
    }

    public int getBalance() { return balance; }
    public void setBalance(int balance) { this.balance = balance; }
    public int getFreeQuota() { return freeQuota; }
    public void setFreeQuota(int freeQuota) { this.freeQuota = freeQuota; }
}
