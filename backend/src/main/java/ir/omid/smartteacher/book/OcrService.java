package ir.omid.smartteacher.book;

import java.io.InputStream;

/**
 * قرارداد OCR — بدون وابسته بودن به ارائه‌دهنده ابری.
 *
 * <p><b>🔴 هیچ شکل API گوگل ویژن یا ارائه‌دهنده دیگری اینجا حدس زده
 * نمی‌شود.</b> این اینترفیس فقط «تصویر بده → متن بگیر» است. پیاده‌سازی
 * واقعی وقتی مستندات رسمی تأیید شد اضافه می‌شود؛ تا آن روز
 * {@code MockOcrService} متن جایگزین برمی‌گرداند.
 */
public interface OcrService {

    /**
     * استخراج متن از تصویر صفحه.
     *
     * @param imageContent جریان بایت تصویر
     * @param contentType  نوع MIME تصویر؛ می‌تواند {@code null} باشد
     * @return نتیجه OCR
     */
    OcrResult extractText(InputStream imageContent, String contentType);

    /**
     * خروجی OCR.
     *
     * @param text       متن استخراج‌شده
     * @param confidence اطمینان ۰ تا ۱؛ {@code null} اگر ارائه‌دهنده ندهد
     * @param skipped    {@code true} اگر OCR عمداً اجرا نشده (مثلاً Mock خاموش)
     */
    record OcrResult(String text, Double confidence, boolean skipped) {

        /** نتیجهٔ جایگزین برای محیط بدون OCR واقعی. */
        public static OcrResult placeholder() {
            return new OcrResult(
                    "[OCR غیرفعال — متن جایگزین گام ۱ فاز ۶]",
                    null,
                    true);
        }
    }
}
