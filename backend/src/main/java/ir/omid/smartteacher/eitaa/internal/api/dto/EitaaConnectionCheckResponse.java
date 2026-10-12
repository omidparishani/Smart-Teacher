package ir.omid.smartteacher.eitaa.internal.api.dto;

import java.time.Instant;

/**
 * نتیجه بررسی اتصال به ایتا.
 *
 * <p>🔴 <b>این پاسخ دقیقاً می‌گوید چه چیزی تأیید شد و چه چیزی نه.</b>
 * ایتا هیچ متدی برای پرسیدن «آیا این گفت‌وگو وجود دارد و ربات به آن
 * دسترسی دارد؟» ندارد — نه {@code getChat}، نه {@code getChatMember}.
 * پس این بررسی <b>هرگز</b> ادعا نمی‌کند دسترسی ارسال تأیید شده است.
 * تنها راه قطعی اطمینان، فرستادن یک پیام آزمایشی واقعی است
 * ({@code POST /api/v1/eitaa/channels/{id}/send-message}).
 *
 * <p>⚠️ این تفکیک عمدی است. اگر این Endpoint می‌گفت «اتصال سالم است»
 * در حالی که فقط توکن بررسی شده، کاربر با یک باگ بی‌صدا رها می‌شد:
 * فکر می‌کرد همه‌چیز درست است و پیام‌ها نمی‌رسیدند.
 *
 * @param botTokenConfigured آیا {@code APP_EITAA_BOT_TOKEN} تنظیم شده است
 * @param mockMode           آیا {@code app.mock.eitaa=true} است.
 *                           🔴 در این حالت {@code getMe} همیشه موفق
 *                           می‌شود، پس نتیجه <b>اتصال واقعی را تأیید
 *                           نمی‌کند</b> — و این فیلد همان چیزی است که
 *                           جلوی این سوءبرداشت را می‌گیرد.
 * @param botHealthy         آیا {@code getMe} پاسخ {@code ok: true} داد
 * @param botUsername        نام کاربری ربات؛ {@code null} اگر بررسی نشد
 * @param botDisplayName     نام نمایشی ربات؛ {@code null} اگر بررسی نشد
 * @param chatId             شناسه گفت‌وگوی بررسی‌شده؛ برای بررسی کلی
 *                           ربات {@code null} است
 * @param chatIdFormatValid  آیا قالب {@code chatId} سرانگشتی معتبر است.
 *                           ⚠️ این <b>تأیید وجود یا دسترسی نیست</b>.
 * @param detail             توضیح وضعیت — پیام امن و قابل‌نمایش.
 *                           🔴 پیام خام ایتا اینجا <b>نمی‌آید</b>؛
 *                           {@code EitaaApiException} عمداً آن را حمل
 *                           نمی‌کند و متن خام فقط به Log می‌رود.
 * @param checkedAt          زمان بررسی
 */
public record EitaaConnectionCheckResponse(
        boolean botTokenConfigured,
        boolean mockMode,
        boolean botHealthy,
        String botUsername,
        String botDisplayName,
        String chatId,
        boolean chatIdFormatValid,
        String detail,
        Instant checkedAt
) {
}
