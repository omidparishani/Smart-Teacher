package ir.omid.smartteacher.user;

import ir.omid.smartteacher.common.jpa.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * حساب کاربری.
 *
 * <p><b>سطح پلتفرم است، نه مستأجر:</b> یک کاربر می‌تواند هم‌زمان در
 * چند مستأجر عضو باشد (مدرسه الف معلم، مدرسه ب ولی). پس {@code users}
 * نمی‌تواند {@code tenant_id} داشته باشد و از
 * {@link ir.omid.smartteacher.tenant.TenantAwareEntity} ارث‌بری نمی‌کند.
 *
 * <p><b>⚠️ امنیتی:</b> این موجودیت {@code passwordHash} دارد و هرگز
 * نباید مستقیم به API برگردد. همه پاسخ‌ها باید از DTO عبور کنند.
 * {@link #toString()} هم عمداً بازنویسی شده تا هش رمز در Log نیفتد.
 */
@Entity
@Table(name = "users")
public class User extends AuditableEntity {

    /** نام کاربری — یکتا و بدون حساسیت به بزرگی و کوچکی حروف. */
    @Column(name = "username", length = 100)
    private String username;

    /**
     * هش رمز عبور (bcrypt/Argon2) — <b>هرگز</b> متن خام.
     *
     * <p>قابل {@code null} است چون کاربری که فقط از طریق ایتا وارد
     * می‌شود رمز عبور ندارد.
     */
    @Column(name = "password_hash", columnDefinition = "TEXT")
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "mobile", length = 20)
    private String mobile;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "preferred_locale", nullable = false, length = 10)
    private String preferredLocale = "fa";

    @Enumerated(EnumType.STRING)
    @Column(name = "theme", nullable = false, length = 10)
    private Theme theme = Theme.SYSTEM;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * راز TOTP رمزنگاری‌شده — هرگز خام در DB یا Log.
     */
    @jakarta.persistence.Convert(converter = ir.omid.smartteacher.security.crypto.EncryptedStringConverter.class)
    @Column(name = "totp_secret", columnDefinition = "TEXT")
    private String totpSecret;

    @Column(name = "totp_enabled", nullable = false)
    private boolean totpEnabled;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** سازنده بدون آرگومان برای JPA. */
    protected User() {
    }

    public User(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getPreferredLocale() {
        return preferredLocale;
    }

    public void setPreferredLocale(String preferredLocale) {
        this.preferredLocale = preferredLocale;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }

    public boolean isTotpEnabled() {
        return totpEnabled;
    }

    public void setTotpEnabled(boolean totpEnabled) {
        this.totpEnabled = totpEnabled;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * حذف نرم — حساب از فهرست‌ها خارج می‌شود؛ نشست‌ها باید جدا باطل شوند.
     */
    public void softDelete() {
        if (deletedAt == null) {
            deletedAt = Instant.now();
        }
        this.status = UserStatus.DISABLED;
    }

    /** آیا حساب در این لحظه قابل استفاده است؟ */
    public boolean isUsable() {
        return status == UserStatus.ACTIVE
                && deletedAt == null
                && (lockedUntil == null || lockedUntil.isBefore(Instant.now()));
    }

    /**
     * ⚠️ عمداً بازنویسی شده: {@code passwordHash} هرگز نباید در Log
     * یا پیام خطا ظاهر شود.
     */
    @Override
    public String toString() {
        return "User{id=" + getId() + ", status=" + status + '}';
    }
}
