package ir.omid.smartteacher.eitaa;

import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;

/**
 * قابلیتی درخواست شد که ایتا <b>ندارد</b>
 * ({@code docs/eitaa.md} بخش ۸.۱).
 *
 * <p><b>چرا این استثنا وجود دارد؟</b> چون جایگزین «شبیه‌سازی» است.
 * اگر کسی می‌خواست پیام ورودی بخواند یا پیام را ویرایش کند، دو راه
 * داشت: یا بی‌صدا کاری کند که کار نمی‌کند، یا صریح بگوید ممکن نیست.
 * اولی بدتر است، چون باگ در زمان اجرا و در Production کشف می‌شود.
 *
 * <p>کد ۵۰۱ است، نه ۵۰۰: این خرابی ما نیست، ایتا آن قابلیت را
 * ارائه نمی‌دهد.
 *
 * <p><b>⚠️ چرا در {@code common/error} نیست، در حالی که بقیه
 * استثناهای ایتا آنجا هستند؟</b> چون این کلاس به {@link EitaaCapability}
 * ارجاع می‌دهد و آن در ماژول {@code eitaa} است. اگر اینجا می‌ماند،
 * وابستگی {@code common → eitaa} ساخته می‌شد و با وابستگی موجود
 * {@code eitaa → common} یک <b>چرخه ماژول</b> می‌ساخت که
 * {@code ApplicationModules.verify()} آن را رد می‌کند.
 * سه استثنای دیگر {@code initData} هیچ نوعی از ایتا را لمس نمی‌کنند،
 * پس بی‌اشکال در {@code common/error} مانده‌اند.
 */
public class EitaaCapabilityNotSupportedException extends AppException {

    private final transient EitaaCapability requestedCapability;

    public EitaaCapabilityNotSupportedException(EitaaCapability requestedCapability) {
        super(ErrorCode.EITAA_CAPABILITY_NOT_SUPPORTED,
                "قابلیت «" + humanName(requestedCapability) + "» در ایتا وجود ندارد.");
        this.requestedCapability = requestedCapability;
    }

    /** قابلیتی که درخواست شد — برای حسابرسی و Log. */
    public EitaaCapability requestedCapability() {
        return requestedCapability;
    }

    private static String humanName(EitaaCapability capability) {
        return (capability == null) ? "نامعلوم" : capability.name();
    }
}
