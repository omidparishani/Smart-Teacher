package ir.omid.smartteacher.audit;

import ir.omid.smartteacher.common.util.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * یک رکورد حسابرسی.
 *
 * <p><b>⚠️ چرا {@code BaseEntity} نیست؟</b> چون جدول {@code audit_logs}
 * در سطح دیتابیس فقط-افزودنی است (تریگر {@code prevent_audit_mutation}
 * در V5). یعنی نه ستون {@code updated_at} دارد و نه {@code version}.
 * ارث‌بری از {@code BaseEntity} این دو ستون را طلب می‌کرد و
 * {@code ddl-auto: validate} در زمان راه‌اندازی شکست می‌خورد. اینجا
 * نگاشت باید دقیقاً شکل واقعی جدول را منعکس کند، نه یک الگوی عمومی را.
 *
 * <p><b>⚠️ چرا {@code @TenantId} ندارد؟</b> با اینکه ستون
 * {@code tenant_id} دارد. دو دلیل:
 * <ol>
 *   <li>لاگ حسابرسی باید <b>همیشه</b> نوشته شود، حتی وقتی زمینه مستأجر
 *       خراب یا غایب است — و دقیقاً همان لحظه است که بیشترین نیاز را
 *       به آن داریم. فیلتر خودکار می‌توانست نوشتن را در دامنه پلتفرم به
 *       {@code tenant_id = 0} ببرد و رکورد را از دید مستأجر پنهان کند.</li>
 *   <li>ادمین پلتفرم باید بتواند رویدادهای همه مستأجرها را ببیند.</li>
 * </ol>
 * دسترسی مستأجری در سرویس با شرط صریح {@code tenant_id} اعمال می‌شود.
 *
 * <p><b>قاعده افزودن ستون:</b> هر ستون حساس (کلید API، توکن) که در
 * {@code metadata} ثبت می‌شود باید پیش از ثبت پوشانده شود. لاگ حسابرسی
 * فقط-افزودنی است — رازی که یک بار داخلش بنشیند، برای همیشه آنجاست و
 * با هیچ مهاجرتی پاک نمی‌شود.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id = UuidV7Generator.generate();

    @Column(name = "tenant_id", updatable = false)
    private UUID tenantId;

    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private ActorType actorType = ActorType.USER;

    /** نام رویداد، مثل {@code SECURITY_IDOR_ATTEMPT}. */
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state")
    private String beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state")
    private String afterState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity = Severity.INFO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** سازنده بدون آرگومان برای JPA. */
    protected AuditLog() {
    }

    public AuditLog(String action, ActorType actorType, Severity severity) {
        this.action = action;
        this.actorType = actorType;
        this.severity = severity;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(UUID actorUserId) {
        this.actorUserId = actorUserId;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public void setActorType(ActorType actorType) {
        this.actorType = actorType;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public String getBeforeState() {
        return beforeState;
    }

    public void setBeforeState(String beforeState) {
        this.beforeState = beforeState;
    }

    public String getAfterState() {
        return afterState;
    }

    public void setAfterState(String afterState) {
        this.afterState = afterState;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
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

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Severity getSeverity() {
        return severity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
