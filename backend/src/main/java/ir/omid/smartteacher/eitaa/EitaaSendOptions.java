package ir.omid.smartteacher.eitaa;

import java.time.Instant;

/**
 * گزینه‌های ارسال — نگاشت مستقیم پارامترهای مستندشده
 * ({@code docs/eitaa.md} بخش ۴.۵).
 *
 * <p>هر فیلد این رکورد یک پارامتر <b>تأییدشده</b> در سند رسمی
 * ایتایار است. هیچ فیلد اضافه‌ای اینجا نیست، چون پارامتری که در سند
 * نیست، وجود ندارد.
 *
 * @param disableNotification   پارامتر {@code disable_notification} — ارسال بی‌صدا
 * @param replyToMessageId      پارامتر {@code reply_to_message_id} — پاسخ به پیام دیگر
 * @param scheduledAt           پارامتر {@code date} — ارسال زمان‌بندی‌شده بومی ایتا.
 *                              ⚠️ این <b>جایگزین</b> زمان‌بند ما نیست، لایه دوم
 *                              اطمینان است ({@code docs/eitaa.md} بخش ۴.۵).
 *                              منطقه زمانی این پارامتر با آزمون واقعی تأیید
 *                              نشده؛ فعلاً Unix Timestamp در UTC فرض می‌شود.
 * @param pinAfterSend          پارامتر {@code pin} — سنجاق پیام پس از ارسال
 * @param viewCountForDelete    پارامتر {@code viewCountForDelete} — حذف خودکار پس از N بازدید
 */
public record EitaaSendOptions(
        boolean disableNotification,
        String replyToMessageId,
        Instant scheduledAt,
        boolean pinAfterSend,
        Integer viewCountForDelete
) {

    /** گزینه‌های پیش‌فرض — ارسال معمولی بدون هیچ تنظیم ویژه. */
    public static EitaaSendOptions defaults() {
        return new EitaaSendOptions(false, null, null, false, null);
    }

    /** ارسال زمان‌بندی‌شده با پارامتر بومی {@code date}. */
    public static EitaaSendOptions scheduledAt(Instant when) {
        return new EitaaSendOptions(false, null, when, false, null);
    }

    /** آیا این ارسال به پارامتر {@code date} نیاز دارد؟ */
    public boolean hasScheduling() {
        return scheduledAt != null;
    }

    /** آیا مقدار {@code viewCountForDelete} معتبر است (مثبت)؟ */
    public boolean hasAutoDelete() {
        return viewCountForDelete != null && viewCountForDelete > 0;
    }
}
