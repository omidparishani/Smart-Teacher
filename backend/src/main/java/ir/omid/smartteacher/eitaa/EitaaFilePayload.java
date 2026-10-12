package ir.omid.smartteacher.eitaa;

import java.util.Arrays;

/**
 * فایل آماده ارسال — برای متد {@code sendFile}.
 *
 * <p>نوع محتوا (Content-Type) صریح نگه داشته می‌شود چون ایتا بر پایه
 * آن تصمیم می‌گیرد فایل را چطور نمایش دهد.
 *
 * <p><b>⚠️ دو قاعده فرمت که در سند رسمی آمده و اینجا اجبار
 * می‌شوند</b> ({@code docs/eitaa.md} بخش ۴.۶):
 * <ul>
 *   <li>ارسال <b>گیف</b>: پسوند باید {@code .gif} باشد — حتی اگر
 *       محتوا در واقع MP4 باشد.</li>
 *   <li>ارسال <b>استیکر</b>: پسوند باید {@code .webp} باشد.</li>
 * </ul>
 * این قواعد در {@link #asGif()} و {@link #asSticker()} کپسوله شده‌اند
 * تا در محل فراخوانی فراموش نشوند.
 *
 * <p><b>⚠️ چرا {@code bytes} در {@code toString()} نمی‌آید؟</b> محتوای
 * فایل ممکن است کار دانش‌آموز یا سند محرمانه باشد. در Log فقط نام و
 * اندازه می‌آید.
 *
 * @param fileName    نام فایل همراه پسوند — ایتا بر پایه آن نوع را تشخیص می‌دهد
 * @param contentType نوع MIME، مثل {@code image/png}
 * @param bytes       محتوای فایل
 */
public record EitaaFilePayload(String fileName, String contentType, byte[] bytes) {

    /** سقف حجم فایل.
     *
     * <p>⚠️ <b>تأییدنشده:</b> حداکثر حجم فایل در سند رسمی ایتا نیامده.
     * این سقف از {@code spring.servlet.multipart.max-file-size} پروژه
     * گرفته شده تا جایی که خودمان رد می‌کنیم، یک سقف واقعی و مستند
     * داشته باشیم — نه یک عدد حدسی درباره ایتا.
     */
    public static final int MAX_BYTES = 20 * 1024 * 1024;

    public EitaaFilePayload {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("نام فایل نباید خالی باشد");
        }
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("محتوای فایل نباید خالی باشد");
        }
        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException(
                    "حجم فایل از سقف " + MAX_BYTES + " بایت بیشتر است");
        }
        contentType = (contentType == null || contentType.isBlank())
                ? "application/octet-stream"
                : contentType;
    }

    /** نسخه‌ای با پسوند {@code .gif} — قاعده ارسال گیف در ایتا. */
    public EitaaFilePayload asGif() {
        return withExtension("gif");
    }

    /** نسخه‌ای با پسوند {@code .webp} — قاعده ارسال استیکر در ایتا. */
    public EitaaFilePayload asSticker() {
        return withExtension("webp");
    }

    private EitaaFilePayload withExtension(String extension) {
        int dot = fileName.lastIndexOf('.');
        String base = (dot > 0) ? fileName.substring(0, dot) : fileName;
        return new EitaaFilePayload(base + "." + extension, contentType, bytes);
    }

    public int size() {
        return bytes.length;
    }

    /**
     * ⚠️ محتوا عمداً نمایش داده نمی‌شود — نه کامل و نه هش‌شده.
     * اندازه کافی است تا در Log بفهمیم کدام فایل ارسال شد.
     *
     * <p>نکته: رکورد بودن یعنی {@code equals} روی آرایه با مرجع مقایسه
     * می‌کند، نه با محتوا. این کلاس برای مقایسه محتوایی ساخته نشده؛
     * برای آن {@code Arrays.equals} روی {@link #bytes()} لازم است.
     */
    @Override
    public String toString() {
        return "EitaaFilePayload{fileName=" + fileName + ", contentType=" + contentType
                + ", size=" + bytes.length + "B}";
    }

    /** مقایسه محتوایی — چون {@code equals} پیش‌فرض رکورد برای آرایه کار نمی‌کند. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EitaaFilePayload that)) {
            return false;
        }
        return fileName.equals(that.fileName)
                && contentType.equals(that.contentType)
                && Arrays.equals(bytes, that.bytes);
    }

    /** هش محتوایی — سازگار با {@link #equals}. */
    @Override
    public int hashCode() {
        int result = fileName.hashCode();
        result = 31 * result + contentType.hashCode();
        result = 31 * result + Arrays.hashCode(bytes);
        return result;
    }
}
