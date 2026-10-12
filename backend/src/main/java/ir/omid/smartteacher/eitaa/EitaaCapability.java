package ir.omid.smartteacher.eitaa;

/**
 * قابلیت‌های <b>واقعی</b> ایتا ({@code docs/eitaa.md} بخش ۸.۱).
 *
 * <p><b>⚠️ چرا این Enum کوتاه است؟</b> چون فهرست کامل متدهای Bot API
 * ایتایار فقط سه متد است: {@code getMe}، {@code sendMessage} و
 * {@code sendFile}. هر چیزی که اینجا نیست، <i>در API وجود ندارد</i> —
 * نه اینکه ما پیاده‌اش نکرده باشیم.
 *
 * <p>نبودنشان در Enum عمدی است: اگر ثابتی مثل {@code RECEIVE_MESSAGE}
 * اینجا بود، کسی می‌توانست باور کند می‌شود پیام ورودی خواند و کدی
 * بر همان پایه نوشته شود. نبودنش، آن باور را غیرقابل نوشتن می‌کند.
 *
 * <p>هر جا کدی بخواهد از قابلیتی استفاده کند که ایتا ندارد، باید
 * به‌جای شبیه‌سازی، استثنای روشن بدهد — نه پیاده‌سازی جعلی.
 */
public enum EitaaCapability {

    /** پاسخ به {@code getMe}. */
    GET_BOT_INFO,

    /** ارسال پیام متنی. */
    SEND_MESSAGE,

    /** ارسال فایل/عکس/ویدیو/گیف/استیکر. */
    SEND_FILE,

    /** ارسال به گروه. */
    SEND_TO_GROUP,

    /** ارسال به کانال. */
    SEND_TO_CHANNEL,

    /** ارسال به کاربر خصوصی — مسیر برنامک، نیازمند اجازه کاربر. */
    SEND_TO_USER,

    /** پارامتر {@code date} — زمان‌بندی بومی ایتا. */
    SCHEDULED_SEND,

    /** پارامتر {@code pin} — سنجاق پیام همین ارسال. */
    PIN_SENT_MESSAGE,

    /** پارامتر {@code viewCountForDelete} — حذف خودکار پس از N بازدید. */
    AUTO_DELETE_ON_VIEW

    // ❌ عمداً غایب — این‌ها در API ایتا وجود ندارند:
    //    RECEIVE_MESSAGE   (getUpdates وجود ندارد)
    //    WEBHOOK           (setWebhook وجود ندارد)
    //    LONG_POLLING      (متدی برای آن نیست)
    //    EDIT_MESSAGE      (editMessageText وجود ندارد)
    //    DELETE_MESSAGE    (deleteMessage وجود ندارد)
    //    SEND_POLL         (sendPoll وجود ندارد)
    //    GET_MEMBERS       (getChatMembersCount وجود ندارد)
    //    GET_ADMINS        (getChatAdministrators وجود ندارد)
    //    INLINE_KEYBOARD   (InlineKeyboard وجود ندارد)
}
