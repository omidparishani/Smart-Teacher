package ir.omid.smartteacher.eitaa;

/**
 * یک اعلان برای کاربر — ورودی {@link EitaaAdapter#notifyUser}.
 *
 * <p>تفکیک این نوع از متن خام عمدی است: اعلان در آینده فیلدهای بیشتری
 * می‌گیرد (نوع رویداد، لینک {@code start_param}، اولویت) و آن‌وقت
 * امضای {@code notifyUser} عوض نمی‌شود.
 *
 * @param text    متن اعلان
 * @param options گزینه‌های ارسال — معمولاً {@link EitaaSendOptions#defaults()}
 */
public record EitaaNotification(String text, EitaaSendOptions options) {

    public EitaaNotification {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("متن اعلان نباید خالی باشد");
        }
        options = (options == null) ? EitaaSendOptions.defaults() : options;
    }

    /** اعلان ساده بدون تنظیم ویژه. */
    public static EitaaNotification of(String text) {
        return new EitaaNotification(text, EitaaSendOptions.defaults());
    }

    /**
     * ⚠️ عمداً بازنویسی شده تا متن اعلان — که ممکن است نام یا نمره
     * دانش‌آموز باشد — در Log نیفتد.
     */
    @Override
    public String toString() {
        return "EitaaNotification{length=" + text.length() + " chars}";
    }
}
