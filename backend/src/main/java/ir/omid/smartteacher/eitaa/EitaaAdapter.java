package ir.omid.smartteacher.eitaa;

import java.util.Set;

/**
 * دروازه گفت‌وگو با ایتا ({@code docs/eitaa.md} بخش ۸).
 *
 * <p><b>🔴 این اینترفیس فقط می‌فرستد.</b> هیچ متدی برای دریافت پیام
 * ندارد، چون ایتا هیچ مسیری برای آن ندارد: نه {@code getUpdates}، نه
 * {@code setWebhook}، نه Long-Polling. ربات ایتا یک کانال یک‌طرفه است.
 *
 * <p><b>⚠️ چرا {@link #capabilities()} وجود دارد؟</b> چون پیاده‌سازی‌های
 * مختلف این اینترفیس، همه قابلیت‌ها را ندارند:
 * <ul>
 *   <li>{@link EitaaCapability#SEND_TO_USER} فقط وقتی ممکن است که توکن
 *       برنامک تنظیم شده باشد <i>و</i> کاربر اجازه ارسال داده باشد.</li>
 *   <li>{@link EitaaCapability#SEND_FILE} تا وقتی نام درست متد با آزمون
 *       واقعی تأیید نشده، ممکن است در دسترس نباشد.</li>
 * </ul>
 * پس فراخوان باید <b>پیش از</b> صدا زدن، {@code capabilities()} را
 * بپرسد. اگر متدی صدا زده شود که پشتیبانی نمی‌شود،
 * {@link EitaaCapabilityNotSupportedException} پرتاب می‌شود — نه یک
 * تلاش بی‌ثمر که بی‌صدا شکست بخورد.
 *
 * <p><b>⚠️ هرگز استثنا پرتاب نمی‌شود برای شکست ارسال.</b> شکست ارسال
 * به‌صورت {@link EitaaSendResult#success()} = {@code false} برمی‌گردد،
 * چون کار معلم نباید به‌خاطر قطعی ایتا از بین برود
 * ({@code docs/eitaa.md} بخش ۹). استثنا فقط برای «این کار ممکن نیست»
 * است، نه «این کار الآن نشد».
 */
public interface EitaaAdapter {

    /**
     * اطلاعات ربات — متد {@code getMe}.
     *
     * @param token توکن ربات
     * @return اطلاعات ربات
     * @throws ir.omid.smartteacher.common.error.EitaaApiException اگر ایتا پاسخ ناموفق داد
     */
    EitaaBotInfo getBotInfo(EitaaBotToken token);

    /**
     * ارسال متن به گروه یا کانال — مسیر الف.
     *
     * <p>اگر متن از سقف طول پیام بلندتر باشد، پیش از ارسال تقسیم
     * می‌شود و همه بخش‌ها فرستاده می‌شوند. نتیجه، نتیجه <b>آخرین</b>
     * بخش است؛ اگر هر بخشی شکست بخورد، همان شکست برگردانده می‌شود.
     *
     * @param token   توکن ربات
     * @param chatId  شناسه یا نام کاربری گروه/کانال
     * @param text    متن پیام
     * @param options گزینه‌های ارسال؛ {@code null} یعنی پیش‌فرض
     * @return نتیجه ارسال
     */
    EitaaSendResult sendToChat(EitaaBotToken token,
                               String chatId,
                               String text,
                               EitaaSendOptions options);

    /**
     * ارسال فایل به گروه یا کانال — مسیر الف، متد {@code sendFile}.
     *
     * @param token   توکن ربات
     * @param chatId  شناسه یا نام کاربری گروه/کانال
     * @param file    فایل؛ برای گیف و استیکر از
     *                {@link EitaaFilePayload#asGif()} و
     *                {@link EitaaFilePayload#asSticker()} استفاده کنید
     * @param caption متن همراه فایل — در این متد جای {@code text} را می‌گیرد
     * @param options گزینه‌های ارسال؛ {@code null} یعنی پیش‌فرض
     * @return نتیجه ارسال
     */
    EitaaSendResult sendFileToChat(EitaaBotToken token,
                                   String chatId,
                                   EitaaFilePayload file,
                                   String caption,
                                   EitaaSendOptions options);

    /**
     * ارسال پیام خصوصی به کاربر — مسیر ب.
     *
     * <p>⚠️ پیش‌نیاز: کاربر باید به ما اجازه ارسال داده باشد. بدون آن،
     * ایتا پاسخ {@code ok: false} می‌دهد و نتیجه شکست برمی‌گردد.
     *
     * @param token       توکن برنامک
     * @param eitaaUserId شناسه کاربر در ایتا — {@code Long} و نه {@code int}
     * @param text        متن پیام
     * @return نتیجه ارسال
     */
    EitaaSendResult sendToUser(EitaaAppToken token, Long eitaaUserId, String text);

    /**
     * اعلان به کاربر با انتخاب خودکار مسیر.
     *
     * <p>این متد مسیر ب را انتخاب می‌کند (پیام خصوصی) و در صورت نبودِ
     * توکن برنامک، {@link EitaaCapabilityNotSupportedException} پرتاب
     * می‌کند به‌جای آنکه بی‌صدا کاری کند.
     *
     * <p><b>⚠️ این متد درباره اجازه کاربر تصمیم نمی‌گیرد.</b> سیاست
     * محصولی «اگر کاربر اجازه نداده، نپرس و نفرست» در لایه بالاتر
     * اعمال می‌شود، چون به وضعیت ذخیره‌شده کاربر وابسته است نه به
     * ایتا.
     *
     * @param eitaaUserId  شناسه کاربر در ایتا
     * @param notification متن و گزینه‌های اعلان
     * @return نتیجه ارسال
     */
    EitaaSendResult notifyUser(Long eitaaUserId, EitaaNotification notification);

    /**
     * قابلیت‌هایی که این پیاده‌سازی <b>واقعاً</b> دارد.
     *
     * <p>فهرست باید بر پایه پیکربندی و توانایی واقعی ساخته شود، نه
     * ثابت. آداپتوری که توکن برنامک ندارد نباید
     * {@link EitaaCapability#SEND_TO_USER} را گزارش کند.
     */
    Set<EitaaCapability> capabilities();

    /**
     * بررسی سلامت اتصال — با {@code getMe}.
     *
     * <p>این متد استثنا پرتاب <b>نمی‌کند</b>؛ خرابی به‌صورت
     * {@link EitaaHealth#healthy()} = {@code false} برمی‌گردد. دلیلش
     * این است که بررسی سلامت، خودش ابزار تشخیص خرابی است و اگر در
     * خرابی استثنا پرتاب کند، داشبورد به‌جای نمایش وضعیت، خطا می‌دهد.
     */
    EitaaHealth checkHealth(EitaaBotToken token);
}
