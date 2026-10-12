package ir.omid.smartteacher.eitaa;

/**
 * اطلاعات ربات — پاسخ {@code getMe} ({@code docs/eitaa.md} بخش ۴.۴).
 *
 * @param id        شناسه عددی ربات
 * @param username  نام کاربری ربات در ایتا — در ساخت لینک {@code start_param} استفاده می‌شود
 * @param firstName نام ربات
 * @param lastName  نام خانوادگی ربات؛ می‌تواند {@code null} باشد
 */
public record EitaaBotInfo(Long id, String username, String firstName, String lastName) {

    /** نام نمایشی — برای لاگ و رابط کاربری. */
    public String displayName() {
        if (firstName == null || firstName.isBlank()) {
            return (username == null) ? String.valueOf(id) : username;
        }
        return (lastName == null || lastName.isBlank())
                ? firstName
                : firstName + " " + lastName;
    }

    /**
     * ⚠️ عمداً بازنویسی شده: هیچ فیلد حساسی اینجا نیست، ولی
     * سازگاری با بقیه ماژول ایتا را حفظ می‌کند.
     */
    @Override
    public String toString() {
        return "EitaaBotInfo{id=" + id + ", username=" + username + '}';
    }
}
