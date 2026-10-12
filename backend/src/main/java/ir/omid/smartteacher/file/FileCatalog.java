package ir.omid.smartteacher.file;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

/**
 * درِ خروجی ماژول {@code file} برای ماژول‌های دامنه (مثل {@code book}).
 *
 * <p>ذخیرهٔ فیزیکی + ثبت متادیتا در جدول {@code files} را یک‌جا انجام
 * می‌دهد تا مصرف‌کننده نه به {@code Repository} و نه به مسیر دیسک
 * وابسته شود.
 */
public interface FileCatalog {

    /**
     * ذخیره جریان به‌عنوان فایل متعلق به مستأجر جاری.
     *
     * @param originalFilename نام اصلی که کاربر فرستاده؛ می‌تواند تهی باشد
     * @param contentType      نوع MIME؛ می‌تواند تهی باشد
     * @param content          جریان بایت
     * @param sizeBytes        اندازه
     * @param entityType       برچسب نوع مالک منطقی — مثل {@code BOOK_PAGE}
     * @param entityId         شناسه مالک منطقی؛ می‌تواند بعداً پر شود
     * @return مرجع فایل ذخیره‌شده
     */
    StoredFileRef store(String originalFilename,
                        String contentType,
                        InputStream content,
                        long sizeBytes,
                        String entityType,
                        UUID entityId);

    /**
     * یافتن فایل فعال همین مستأجر.
     *
     * @param fileId شناسه
     * @return مرجع؛ خالی اگر نباشد یا مال مستأجر دیگر باشد
     */
    Optional<StoredFileRef> findActive(UUID fileId);

    /**
     * باز کردن جریان خواندن فایل فعال همین مستأجر.
     *
     * @param fileId شناسه
     * @return جریان؛ فراخواننده باید ببندد
     */
    InputStream open(UUID fileId);

    /**
     * پیوند فایل به موجودیت مالک پس از ساخت — مثلاً بعد از ساخت صفحه.
     *
     * @param fileId     شناسه فایل
     * @param entityType نوع مالک
     * @param entityId   شناسه مالک
     */
    void bindEntity(UUID fileId, String entityType, UUID entityId);

    /**
     * جمع {@code size_bytes} فایل‌های فعال مستأجر جاری — برای سقف
     * {@code storageBytes}.
     *
     * @return مجموع بایت‌ها (≥ ۰)
     */
    long totalSizeBytes();

    /**
     * خلاصهٔ فایل برای پاسخ API و ارجاع بین ماژول‌ها.
     *
     * @param id               شناسه
     * @param originalFilename نام اصلی
     * @param contentType      نوع محتوا
     * @param sizeBytes        اندازه به بایت
     * @param storageKey       کلید نسبی ذخیره‌سازی (داخلی — در پاسخ عمومی لو نرود مگر لازم)
     */
    record StoredFileRef(
            UUID id,
            String originalFilename,
            String contentType,
            long sizeBytes,
            String storageKey
    ) {
    }
}
