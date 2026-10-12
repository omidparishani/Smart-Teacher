package ir.omid.smartteacher.eitaa;

import java.time.Instant;

/**
 * نتیجه یک ارسال به ایتا ({@code docs/eitaa.md} بخش ۱۰).
 *
 * <p><b>⚠️ چرا شکست، استثنا نیست؟</b> چون شکست ارسال به ایتا
 * <i>خطای کاربر نیست</i>. تکلیف در سیستم ما ذخیره شده و از Mini App
 * قابل مشاهده است؛ ایتا فقط یک کانال توزیع است. اگر شکست ارسال را
 * استثنا می‌کردیم، تراکنش انتشار تکلیف برمی‌گشت و کار معلم از بین
 * می‌رفت ({@code docs/eitaa.md} بخش ۹).
 *
 * <p>پس نتیجه به‌صورت مقدار برگردانده می‌شود تا فراخوان بتواند تصمیم
 * بگیرد: هشدار بدهد، دوباره تلاش کند، یا فقط ثبت کند.
 *
 * @param success      آیا ایتا پاسخ {@code ok: true} داد؟
 * @param messageId    {@code result.message_id} — فقط در مسیر الف (ربات) برمی‌گردد.
 *                     در مسیر ب پاسخ {@code "success"} است، پس {@code null} می‌ماند.
 * @param sentAt       زمان ارسال؛ در شکست {@code null}
 * @param errorCode    کد خطا در صورت شکست؛ در غیر این صورت {@code null}
 * @param errorMessage پیام خطا برای <b>لاگ</b> — نباید مستقیم به کاربر نشان داده شود
 */
public record EitaaSendResult(
        boolean success,
        String messageId,
        Instant sentAt,
        String errorCode,
        String errorMessage
) {

    /** یک ارسال موفق. */
    public static EitaaSendResult success(String messageId, Instant sentAt) {
        return new EitaaSendResult(true, messageId, sentAt, null, null);
    }

    /** یک ارسال ناموفق — دلیلش برای لاگ نگه داشته می‌شود. */
    public static EitaaSendResult failure(String errorCode, String errorMessage) {
        return new EitaaSendResult(false, null, null, errorCode, errorMessage);
    }

    /**
     * ⚠️ عمداً بازنویسی شده تا متن پیام — که ممکن است حاوی داده
     * دانش‌آموز باشد — در Log نیفتد. این رکورد متن پیام را نگه
     * نمی‌دارد، ولی بازنویسی صریح، وابستگی به همین واقعیت را تثبیت
     * می‌کند.
     */
    @Override
    public String toString() {
        return "EitaaSendResult{success=" + success + ", messageId=" + messageId
                + ", sentAt=" + sentAt + ", errorCode=" + errorCode + '}';
    }
}
