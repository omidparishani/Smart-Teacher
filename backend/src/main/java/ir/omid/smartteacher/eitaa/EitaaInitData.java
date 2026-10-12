package ir.omid.smartteacher.eitaa;

import java.time.Instant;

/**
 * نتیجه اعتبارسنجی موفق {@code initData}.
 *
 * <p>ساخته‌شدن این شیء یعنی «امضا خواند، منقضی نبود، قبلاً مصرف نشده
 * بود». هیچ‌جای دیگر کد نباید دوباره این بررسی‌ها را تکرار کند و هیچ‌جا
 * نباید {@code initData} خام را جداگانه تفسیر کند — چون آن‌وقت بررسی‌ها
 * می‌توانند از هم واگرا شوند.
 *
 * <p><b>🔴 {@code startParam} داده غیرقابل‌اعتماد است</b>
 * ({@code docs/eitaa.md} بخش ۱۲). کاربر می‌تواند هر مقداری در لینک
 * بگذارد. این فیلد <b>هرگز</b> نباید به‌عنوان دستور، مسیر فایل، نام
 * کلاس یا پرس‌وجوی SQL به کار رود. تنها کاربرد مجازش، پارس‌کردن
 * شناسه عددی است — و آن هم با بررسی قالب.
 *
 * @param user        کاربر ایتا — تأییدشده با امضا
 * @param authDate    زمان صدور {@code initData}
 * @param queryId     شناسه یکتای جلسه برنامک؛ ممکن است نباشد
 * @param startParam  پارامتر Deep Link — <b>داده غیرقابل‌اعتماد</b>
 * @param chatType    نوع گفت‌وگو؛ ممکن است نباشد
 * @param deviceId    شناسه دستگاه+حساب؛ ممکن است نباشد
 * @param fingerprint هش یکتای همین {@code initData} — برای تشخیص بازپخش
 */
public record EitaaInitData(
        EitaaUserData user,
        Instant authDate,
        String queryId,
        String startParam,
        String chatType,
        String deviceId,
        String fingerprint
) {

    /** آیا کاربر با پارامتر Deep Link وارد شده است؟ */
    public boolean hasStartParam() {
        return startParam != null && !startParam.isBlank();
    }

    /**
     * ⚠️ عمداً بازنویسی شده: {@code fingerprint} برای تشخیص بازپخش
     * استفاده می‌شود و اگر در Log بیفتد، کسی که Log را می‌خواند
     * می‌تواند در پنجره پنج‌دقیقه‌ای همان initData را دوباره بفرستد.
     */
    @Override
    public String toString() {
        return "EitaaInitData{userId=" + (user == null ? null : user.id())
                + ", authDate=" + authDate
                + ", hasStartParam=" + hasStartParam() + '}';
    }
}
