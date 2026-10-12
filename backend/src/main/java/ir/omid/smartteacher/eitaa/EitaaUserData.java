package ir.omid.smartteacher.eitaa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * کاربر ایتا — همان چیزی که در فیلد {@code user} داخل {@code initData}
 * می‌آید، ولی <b>URL-decoded</b> ({@code docs/eitaa.md} بخش ۲.۵).
 *
 * <p><b>⚠️ چرا فقط {@code id} قابل اتکاست؟</b> سند رسمی برای فیلد
 * {@code user} فقط {@code id}، {@code first_name}، {@code last_name}،
 * {@code language_code} و {@code allows_write_to_pm} را فهرست می‌کند.
 * بقیه فیلدها اینجا اختیاری‌اند: اگر ایتا نفرستد، {@code null} می‌مانند
 * و کد باید با نبودشان کار کند. هیچ فیلدی اجبار نمی‌شود، چون نبودِ یک
 * فیلد نباید ورود کاربر را بشکند.
 *
 * <p><b>🔴 {@code id} باید {@code Long} باشد.</b> سند رسمی صریح می‌گوید
 * این عدد ممکن است تا ۵۲ بیت اهمیت داشته باشد. با {@code Integer}،
 * شناسه کاربر <i>بی‌صدا</i> خراب می‌شود — بدون خطا، با مقدار اشتباه.
 * این بدترین نوع باگ است: داده به کاربر دیگری نسبت داده می‌شود.
 *
 * @param id               شناسه کاربر در ایتا — ۵۲ بیتی
 * @param firstName        نام
 * @param lastName         نام خانوادگی
 * @param username         نام کاربری — ممکن است نباشد
 * @param languageCode     کد زبان ایتا، مثل {@code fa}
 * @param isPremium        آیا کاربر اشتراک ویژه دارد؟
 * @param allowsWriteToPm  آیا کاربر اجازه ارسال پیام خصوصی داده است؟
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EitaaUserData(
        @JsonProperty("id") Long id,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("username") String username,
        @JsonProperty("language_code") String languageCode,
        @JsonProperty("is_premium") Boolean isPremium,
        @JsonProperty("allows_write_to_pm") Boolean allowsWriteToPm
) {

    /**
     * آیا کاربر اجازه ارسال پیام خصوصی داده است؟
     *
     * <p>نبودِ فیلد به‌معنای «نه» تفسیر می‌شود، نه «شاید». این انتخاب
     * عمدی و امن است: سیاست ما «هرگز بدون اجازه ارسال نکن» است، پس
     * نبودِ اطلاعات باید به سمت نرفتن چرخ بخورد، نه فرض‌کردن اجازه.
     */
    public boolean hasWriteAccess() {
        return Boolean.TRUE.equals(allowsWriteToPm);
    }

    /** نام نمایشی — نام و نام خانوادگی، یا نام کاربری، یا شناسه. */
    public String displayName() {
        if (firstName != null && !firstName.isBlank()) {
            return (lastName == null || lastName.isBlank())
                    ? firstName
                    : firstName + " " + lastName;
        }
        if (username != null && !username.isBlank()) {
            return username;
        }
        return String.valueOf(id);
    }

    /**
     * ⚠️ عمداً بازنویسی شده: شناسه ایتا داده شخصی است و در Log عمومی
     * نباید پخش شود. برای تشخیص، شناسه کافی است ولی نام نه.
     */
    @Override
    public String toString() {
        return "EitaaUserData{id=" + id + ", languageCode=" + languageCode
                + ", allowsWriteToPm=" + allowsWriteToPm + '}';
    }
}
