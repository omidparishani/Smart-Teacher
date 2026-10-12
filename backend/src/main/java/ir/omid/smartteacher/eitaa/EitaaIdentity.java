package ir.omid.smartteacher.eitaa;

import ir.omid.smartteacher.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * پیوند یک کاربر ایتا به یک کاربر سیستم ما.
 *
 * <p><b>⚠️ چرا {@code BaseEntity} و نه {@code AuditableEntity}؟</b>
 * چون جدول {@code eitaa_identities} — که در Migration
 * {@code V2__identity_and_tenancy.sql} ساخته شده — ستون‌های
 * {@code created_by} و {@code updated_by} ندارد. اگر موجودیت را از
 * {@code AuditableEntity} می‌ساختیم، Hibernate با {@code ddl-auto:
 * validate} هنگام بالا آمدن کد را رد می‌کرد. این محدودیت خودِ Schema
 * است، نه یک انتخاب سلیقه‌ای.
 *
 * <p>{@code tenant_id} ندارد و {@code @TenantId} نمی‌گیرد — عمدی است.
 * این جدول در زمان <b>ورود</b> خوانده می‌شود، یعنی پیش از آنکه
 * مستأجری معلوم باشد. اگر فیلتر خودکار Hibernate رویش اعمال می‌شد،
 * کوئری «کدام کاربر این ایتا آیدی است» هیچ‌وقت نتیجه نمی‌داد و ورود
 * کاربر برای همیشه می‌شکست — همان استدلالی که درباره
 * {@code tenant_members} و {@code refresh_tokens} هم برقرار است.
 * یکتایی {@code eitaa_user_id} در سطح پلتفرم، از پیوند یک حساب ایتا
 * به دو کاربر جلوگیری می‌کند.
 *
 * <p><b>⚠️ {@code phoneNumber}، {@code firstName} و
 * {@code eitaaUsername}</b> داده شخصی‌اند و در {@code toString()}
 * نمی‌آیند.
 */
@Entity
@Table(name = "eitaa_identities")
public class EitaaIdentity extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** شناسه کاربر در ایتا — ۵۲ بیتی، پس {@code Long}. */
    @Column(name = "eitaa_user_id", nullable = false, updatable = false)
    private Long eitaaUserId;

    @Column(name = "eitaa_username", length = 100)
    private String eitaaUsername;

    @Column(name = "first_name", length = 200)
    private String firstName;

    @Column(name = "last_name", length = 200)
    private String lastName;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @Column(name = "is_premium", nullable = false)
    private boolean premium;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    /**
     * آیا کاربر به ما اجازه ارسال پیام خصوصی داده است؟
     *
     * <p>مقدار از فیلد {@code allows_write_to_pm} در {@code initData}
     * می‌آید و در هر ورود تازه می‌شود — چون کاربر می‌تواند بعداً اجازه
     * بدهد یا پس بگیرد.
     */
    @Column(name = "allows_write_to_pm", nullable = false)
    private boolean allowsWriteToPm;

    /**
     * زمان آخرین {@code requestWriteAccess}.
     *
     * <p>برای اجرای سیاست «اگر کاربر رد کرد، هرگز دوباره نپرس». بدون
     * این ستون نمی‌شود «هنوز نپرسیده‌ایم» را از «پرسیده‌ایم و رد
     * کرده» تشخیص داد — هر دو {@code allowsWriteToPm = false} می‌دهند.
     */
    @Column(name = "write_access_asked_at")
    private Instant writeAccessAskedAt;

    /** سازنده بدون آرگومان برای JPA. */
    protected EitaaIdentity() {
    }

    /**
     * پیوند یک حساب ایتا به یک کاربر سیستم.
     *
     * @param userId شناسه کاربر داخلی ما
     * @param user   اطلاعات کاربر از {@code initData}
     */
    public EitaaIdentity(UUID userId, EitaaUserData user) {
        if (userId == null) {
            throw new IllegalArgumentException("شناسه کاربر نباید خالی باشد");
        }
        if (user == null || user.id() == null) {
            throw new IllegalArgumentException("داده کاربر ایتا نباید خالی باشد");
        }

        this.userId = userId;
        this.eitaaUserId = user.id();
        this.linkedAt = Instant.now();

        refreshFrom(user);
    }

    /**
     * به‌روزرسانی اطلاعات از یک {@code initData} تازه.
     *
     * <p>در هر ورود صدا زده می‌شود. نام و نام کاربری ممکن است در ایتا
     * تغییر کرده باشد و ما نباید نسخه قدیمی را نگه داریم.
     *
     * <p><b>⚠️ چرا {@code allowsWriteToPm} را فقط وقتی مقدار گزارش شده
     * به‌روز می‌کنیم؟</b> چون اگر ایتا فیلد را نفرستد (که در برخی
     * پلتفرم‌ها ممکن است)، {@code null} معنی «گزارش نشده» می‌دهد و
     * نباید اجازه‌ای که کاربر قبلاً داده، بی‌صدا پاک شود.
     */
    public void refreshFrom(EitaaUserData user) {
        if (user == null) {
            return;
        }
        this.eitaaUsername = user.username();
        this.firstName = user.firstName();
        this.lastName = user.lastName();
        this.languageCode = user.languageCode();
        this.premium = Boolean.TRUE.equals(user.isPremium());
        this.lastSeenAt = Instant.now();

        if (user.allowsWriteToPm() != null) {
            this.allowsWriteToPm = user.hasWriteAccess();
        }
    }

    /** ثبت اینکه درخواست دسترسی ارسال از کاربر پرسیده شد. */
    public void markWriteAccessAsked(Instant askedAt) {
        this.writeAccessAskedAt = askedAt;
    }

    /**
     * آیا باید از کاربر اجازه ارسال پرسیده شود؟
     *
     * <p>سیاست محصولی ({@code docs/eitaa.md} بخش ۵.۲): یک بار بپرس؛ اگر
     * رد کرد، هرگز دوباره نپرس. پرسیدن دوباره در هر ورود، آزاردهنده است
     * و کاربر را از محصول فراری می‌دهد.
     */
    public boolean shouldAskWriteAccess() {
        return !allowsWriteToPm && writeAccessAskedAt == null;
    }

    /** ثبت تأیید شماره تلفن — از مسیر {@code requestContact}. */
    public void verifyPhone(String phone) {
        this.phoneNumber = phone;
        this.phoneVerified = phone != null && !phone.isBlank();
    }

    public UUID getUserId() {
        return userId;
    }

    public Long getEitaaUserId() {
        return eitaaUserId;
    }

    public String getEitaaUsername() {
        return eitaaUsername;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public boolean isPremium() {
        return premium;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public boolean isAllowsWriteToPm() {
        return allowsWriteToPm;
    }

    public Instant getWriteAccessAskedAt() {
        return writeAccessAskedAt;
    }

    /**
     * نام نمایشی کاربر ایتا — برای رابط کاربری.
     *
     * <p>در موجودیت نگه داشته می‌شود (نه در یک Mapper جدا) چون هیچ
     * منطق دامنه‌ای ندارد و فقط انتخاب بین سه فیلد موجود است.
     */
    public String getDisplayName() {
        if (firstName != null && !firstName.isBlank()) {
            return (lastName == null || lastName.isBlank())
                    ? firstName
                    : firstName + " " + lastName;
        }
        if (eitaaUsername != null && !eitaaUsername.isBlank()) {
            return eitaaUsername;
        }
        return String.valueOf(eitaaUserId);
    }

    /**
     * ⚠️ عمداً بازنویسی شده: {@code phoneNumber}، نام و نام کاربری،
     * داده شخصی هستند و در Log عمومی نباید پخش شوند. شناسه ایتا برای
     * تشخیص کافی است.
     */
    @Override
    public String toString() {
        return "EitaaIdentity{id=" + getId() + ", eitaaUserId=" + eitaaUserId
                + ", allowsWriteToPm=" + allowsWriteToPm + '}';
    }
}
