package ir.omid.smartteacher.file;

import java.io.IOException;
import java.io.InputStream;

/**
 * قرارداد ذخیره‌سازی بایت‌های فایل — بدون وابسته بودن به دیسک یا ابر.
 *
 * <p><b>⚠️ این اینترفیس فقط مسیر نسبی و جریان بایت می‌شناسد.</b>
 * متادیتا (نام اصلی، نوع محتوا، مالک مستأجر) کار {@link FileCatalog}
 * است. جدا کردنشان یعنی می‌توان Local را با MinIO عوض کرد بدون اینکه
 * جدول {@code files} عوض شود.
 *
 * <p>پیاده‌سازی فعلی: {@code LocalFileStorage} زیر
 * {@code app.storage.local.base-path}.
 */
public interface FileStorage {

    /**
     * ذخیره محتوا زیر کلید نسبی.
     *
     * @param relativeKey مسیر نسبی داخل ریشه ذخیره‌سازی — هرگز نباید
     *                    خارج از ریشه برود
     * @param content     جریان بایت؛ بسته نمی‌شود توسط فراخواننده پس از بازگشت
     * @param sizeHint    اندازهٔ اعلام‌شده؛ ممکن است برای بهینه‌سازی باشد
     * @throws IOException اگر نوشتن شکست بخورد یا کلید نامعتبر باشد
     */
    void store(String relativeKey, InputStream content, long sizeHint) throws IOException;

    /**
     * باز کردن جریان خواندن برای کلید نسبی.
     *
     * @param relativeKey مسیر نسبی
     * @return جریان باز؛ فراخواننده باید ببندد
     * @throws IOException اگر فایل نباشد یا کلید نامعتبر باشد
     */
    InputStream open(String relativeKey) throws IOException;

    /**
     * حذف فایل فیزیکی اگر وجود داشته باشد.
     *
     * @param relativeKey مسیر نسبی
     * @throws IOException اگر حذف شکست بخورد یا کلید نامعتبر باشد
     */
    void delete(String relativeKey) throws IOException;

    /**
     * آیا فایل فیزیکی برای این کلید وجود دارد؟
     *
     * @param relativeKey مسیر نسبی
     * @return {@code true} اگر فایل موجود باشد
     */
    boolean exists(String relativeKey);
}
