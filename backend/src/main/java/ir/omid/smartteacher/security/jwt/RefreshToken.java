package ir.omid.smartteacher.security.jwt;

import ir.omid.smartteacher.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * توکن تازه‌سازی ذخیره‌شده.
 *
 * <p><b>توکن خام هرگز اینجا نیست</b> — فقط {@code SHA-256} آن. اگر
 * دیتابیس لو برود، مهاجم هیچ توکن قابل استفاده‌ای به دست نمی‌آورد؛
 * نمی‌تواند از هش، توکن اصلی را بازسازی کند.
 *
 * <p><b>چرا در دیتابیس و نه فقط امضاشده؟</b> چون توکن امضاشده تا لحظه
 * انقضا معتبر است و نمی‌شود باطلش کرد. ذخیره‌سازی امکان «خروج از
 * حساب»، «باطل‌سازی همه نشست‌ها بعد از تغییر رمز» و «تشخیص استفاده
 * مجدد» را می‌دهد — هیچ‌کدام با توکن صرفاً امضاشده ممکن نیست.
 *
 * <p><b>خانواده توکن:</b> هر بار چرخش، توکن جدید {@code familyId} قبلی
 * را به ارث می‌برد. اگر توکنی که قبلاً مصرف شده دوباره بیاید، یعنی
 * سرقت رخ داده و <b>کل خانواده</b> باطل می‌شود — نه فقط همین توکن.
 *
 * <p>{@code tenant_id} دارد اما {@link ir.omid.smartteacher.tenant.TenantAwareEntity}
 * نیست: در زمان تازه‌سازی خوانده می‌شود، پیش از آنکه مستأجری در زمینه
 * باشد.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** مستأجری که توکن در آن صادر شده — می‌تواند {@code null} باشد. */
    @Column(name = "tenant_id", updatable = false)
    private UUID tenantId;

    /** SHA-256 به‌صورت hex — ۶۴ کاراکتر. */
    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private TokenScope scope;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason", length = 100)
    private String revokedReason;

    /** توکنی که جایگزین این یکی شده — زنجیره چرخش را قابل ردیابی می‌کند. */
    @Column(name = "replaced_by_id")
    private UUID replacedById;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /** سازنده بدون آرگومان برای JPA. */
    protected RefreshToken() {
    }

    public RefreshToken(UUID userId,
                        UUID tenantId,
                        String tokenHash,
                        UUID familyId,
                        TokenScope scope,
                        Instant expiresAt) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.scope = scope;
        this.expiresAt = expiresAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public TokenScope getScope() {
        return scope;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public UUID getReplacedById() {
        return replacedById;
    }

    public void setReplacedById(UUID replacedById) {
        this.replacedById = replacedById;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /** آیا این توکن باطل شده است؟ */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /** آیا از نظر زمانی منقضی شده است؟ */
    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    /** آیا این توکن در این لحظه قابل استفاده است؟ */
    public boolean isUsable() {
        return !isRevoked() && !isExpired();
    }

    /**
     * باطل‌کردن توکن.
     *
     * @param reason دلیل — برای حسابرسی امنیتی، نه برای کاربر
     */
    public void revoke(String reason) {
        revoke(reason, null);
    }

    /**
     * باطل‌کردن توکن همراه با ثبت جانشین.
     *
     * @param reason       دلیل باطل‌سازی
     * @param replacedById شناسه توکن جانشین در چرخش
     */
    public void revoke(String reason, UUID replacedById) {
        if (this.revokedAt == null) {
            this.revokedAt = Instant.now();
            this.revokedReason = reason;
            this.replacedById = replacedById;
        }
    }

    /**
     * ⚠️ عمداً بازنویسی شده: هرچند فیلد {@code tokenHash} هش است و نه
     * توکن خام، در Log نباید بیفتد.
     */
    @Override
    public String toString() {
        return "RefreshToken{id=" + getId() + ", userId=" + userId
                + ", familyId=" + familyId + ", scope=" + scope + '}';
    }
}
