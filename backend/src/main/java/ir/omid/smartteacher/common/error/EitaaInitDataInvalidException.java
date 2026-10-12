package ir.omid.smartteacher.common.error;

/**
 * {@code initData} ایتا نامعتبر است — امضا نخواند، {@code hash} نبود،
 * یا بدنه ناقص بود.
 *
 * <p>⚠️ پیام کاربر همیشه همان پیام پیش‌فرض
 * {@link ErrorCode#EITAA_INIT_DATA_INVALID} است. دلیل دقیق فقط در Log
 * می‌ماند، چون گفتن «کدام فیلد خراب بود» به مهاجم کمک می‌کند.
 */
public class EitaaInitDataInvalidException extends EitaaInitDataException {

    private final transient String technicalReason;

    public EitaaInitDataInvalidException() {
        this(null);
    }

    private EitaaInitDataInvalidException(String technicalReason) {
        super(ErrorCode.EITAA_INIT_DATA_INVALID.defaultMessage());
        this.technicalReason = technicalReason;
    }

    /**
     * ساخت با دلیل فنی.
     *
     * <p><b>⚠️ چرا سازنده عمومی نیست؟</b> اگر بود، کسی می‌توانست
     * رشته‌ای بدهد و آن رشته در پیام کاربر ظاهر شود. متد کارخانه‌ای
     * نامش صریح است: این متن <i>هرگز</i> به کاربر نمی‌رود.
     *
     * @param reason دلیل فنی — فقط برای Log و حسابرسی امنیتی
     */
    public static EitaaInitDataInvalidException because(String reason) {
        return new EitaaInitDataInvalidException(reason);
    }

    /** دلیل فنی شکست — برای ثبت در حسابرسی؛ می‌تواند {@code null} باشد. */
    public String technicalReason() {
        return technicalReason;
    }
}
