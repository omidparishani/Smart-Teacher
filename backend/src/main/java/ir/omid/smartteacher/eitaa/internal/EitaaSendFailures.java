package ir.omid.smartteacher.eitaa.internal;

import ir.omid.smartteacher.common.error.EitaaApiException;
import ir.omid.smartteacher.eitaa.EitaaSendResult;

/**
 * تبدیل شکست HTTP ایتا به {@link EitaaSendResult} — یک‌جا، برای هر دو مسیر.
 *
 * <p><b>🔴 چرا این کلاس وجود دارد؟</b> چون لایه HTTP شکست را با استثنا
 * خبر می‌دهد ({@link EitaaApiException}) ولی قرارداد
 * {@link ir.omid.smartteacher.eitaa.EitaaAdapter} صریح می‌گوید شکست ارسال
 * باید <i>مقدار</i> باشد، نه استثنا ({@code docs/eitaa.md} بخش ۹).
 *
 * <p>تفاوت این دو، در لحظه‌ای معلوم می‌شود که ایتا قطع است: اگر
 * {@code EitaaApiException} از {@code sendToChat} بیرون بزند، تراکنش
 * انتشار تکلیف برمی‌گردد و کار معلم از بین می‌رود — در حالی که تکلیف در
 * سیستم ما ذخیره شده بود و فقط اعلانش نرسیده. ایتا یک کانال توزیع است،
 * نه منبع حقیقت.
 *
 * <p><b>⚠️ چرا fallback برای کد خطا لازم است؟</b> چون
 * {@code remoteErrorCode} وقتی ایتا حتی پاسخ نداده ({@code null}) است.
 * ولی {@code EitaaSendResult.errorCode} نباید {@code null} بماند: هر
 * مصرف‌کننده‌ای روی آن {@code switch} می‌کند یا در متریک می‌نویسد، و
 * {@code null} در آن مسیرها یعنی {@code NullPointerException} در
 * دوردست — درست همان‌وقت که ایتا خراب است.
 *
 * <p>این کلاس حالت ندارد و در حال حاضر فقط یک متد دارد؛ جدا نگه داشته
 * شده تا این سیاست <b>یک</b> محل تعریف داشته باشد، نه سه تا که می‌توانند
 * واگرا شوند.
 */
final class EitaaSendFailures {

    private EitaaSendFailures() {
        throw new AssertionError("کلاس ابزاری نباید نمونه‌سازی شود");
    }

    /**
     * تبدیل شکست ایتا به نتیجه قابل‌برگشت.
     *
     * <p>کد خطا، کد خود ایتا است اگر داشت؛ وگرنه کد دسته‌بندی خودمان.
     * پیام، همان پیام عمومی است — جزئیات فنی ({@code technicalDetail})
     * عمداً اینجا نمی‌آید، چون {@code EitaaSendResult} در Log و در API
     * می‌چرخد و آن متن ممکن است جزئیات داخلی ایتا را حمل کند.
     */
    static EitaaSendResult from(EitaaApiException failure) {
        String remoteCode = failure.remoteErrorCode();
        String code = (remoteCode == null || remoteCode.isBlank())
                ? failure.getErrorCode().name()
                : remoteCode;

        return EitaaSendResult.failure(code, failure.getMessage());
    }
}
