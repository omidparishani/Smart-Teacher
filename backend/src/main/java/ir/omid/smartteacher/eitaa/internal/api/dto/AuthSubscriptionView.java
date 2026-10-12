package ir.omid.smartteacher.eitaa.internal.api.dto;

import java.time.Instant;

/**
 * نمای اشتراک فعال در پاسخ‌های احراز هویت.
 *
 * <p><b>🔴 در فاز ۲ این مقدار همیشه {@code null} است.</b>
 *
 * <p>شکل آن از نمونه {@code docs/api.md} بخش ۵ آمده
 * ({@code planCode}, {@code planName}, {@code status}, {@code endDate})،
 * ولی جدول‌های اشتراک ({@code subscriptions}, {@code plans}) تا
 * <b>فاز ۱۰</b> ساخته نمی‌شوند. پس داده‌ای وجود ندارد که نمایش داده
 * شود.
 *
 * <p><b>⚠️ چرا {@code FREE} جعل نمی‌شود؟</b> چون نمونه سند
 * {@code "planCode": "FREE"} دارد و وسوسه‌کننده است که همان را
 * hardcode کنیم تا پاسخ «کامل» به نظر برسد. این کار یعنی کلاینت و
 * کاربر باور کنند اشتراک رایگانی وجود دارد که در دیتابیس نیست — و
 * بعداً هر منطق سهمیه‌ای که روی این فرض ساخته شود، بی‌صدا غلط کار
 * می‌کند. {@code null} صادقانه می‌گوید «هنوز چیزی نیست»؛ کلاینت
 * باید همان را مدیریت کند.
 *
 * <p>این تعویق در {@code docs/verification-log.md} ثبت شده است.
 *
 * @param planCode کد پلن
 * @param planName نام پلن
 * @param status   وضعیت اشتراک
 * @param endDate  تاریخ پایان
 */
public record AuthSubscriptionView(
        String planCode,
        String planName,
        String status,
        Instant endDate
) {
}
