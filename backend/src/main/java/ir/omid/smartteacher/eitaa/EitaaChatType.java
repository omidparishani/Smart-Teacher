package ir.omid.smartteacher.eitaa;

/**
 * نوع گفت‌وگوی ایتا — از فیلد {@code chat.type} در پاسخ
 * {@code sendMessage} ({@code docs/eitaa.md} بخش ۴.۵).
 *
 * <p>تنها مقادیری که در سند رسمی دیده شده اینجا هستند. اگر ایتا نوع
 * تازه‌ای اضافه کند، مقدار ناشناخته باید به {@link #UNKNOWN} نگاشت شود
 * — نه اینکه استثنا پرتاب شود؛ نوع گفت‌وگو داده‌ای است که ما کنترلش
 * نمی‌کنیم و نبودش نباید ارسال را بشکند.
 */
public enum EitaaChatType {

    /** گروه عمومی. */
    PUBLIC,

    /** گروه. */
    GROUP,

    /** کانال. */
    CHANNEL,

    /** گفت‌وگوی خصوصی. */
    PRIVATE,

    /** نوعی که در سند رسمی نیامده بود. */
    UNKNOWN;

    /** نگاشت از مقدار خام ایتا — حساس به بزرگی/کوچکی حروف نیست. */
    public static EitaaChatType fromRaw(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        try {
            return valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
