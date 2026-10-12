package ir.omid.smartteacher.common.api;

import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * ساخت {@link Pageable} از پارامترهای پرس‌وجو — با سقف و فهرست مجاز.
 *
 * <p><b>🔴 چرا این کلاس لازم است؟</b> چون گرفتن {@code Pageable} مستقیم
 * از Spring یک سوراخ امنیتی است: هر فیلدی که کلاینت بگوید روی همان
 * مرتب‌سازی می‌شود. مهاجم با فرستادن
 * {@code sort=metadata,asc} روی ستون بدون ایندکس، برای هر درخواست یک
 * Sort در حافظه و یک Seq Scan می‌سازد. {@code docs/api.md} بخش ۷ این را
 * الزام کرده: «فیلدهای مجاز مرتب‌سازی Allow-List هستند».
 *
 * <p>سقف {@code size} هم همان‌جا الزام شده («حداکثر ۱۰۰ — بالاتر →
 * کلمپ، ضد DoS»). کلمپ و نه خطا: کلاینتی که ۵۰۰۰ می‌خواهد، درخواستش
 * بی‌معنا نیست، فقط بلندپروازانه است.
 *
 * <p><b>⚠️ چرا فیلد نامجاز خطا می‌دهد ولی اندازه نامجاز کلمپ
 * می‌شود؟</b> چون این دو یک جنس نیستند. اندازه بزرگ یک <i>خواسته</i> است
 * که با سقف ما جور می‌شود؛ فیلد نامجاز یک <i>اشتباه</i> است — یا کلاینت
 * باگ دارد یا کسی دارد حدس می‌زند. اگر بی‌صدا نادیده بگیریم، کلاینت
 * فکر می‌کند مرتب‌سازی اعمال شده و ترتیب اشتباه را به کاربر نشان می‌دهد.
 */
public final class PageableFactory {

    /** اندازه پیش‌فرض صفحه — مطابق {@code docs/api.md} بخش ۷. */
    public static final int DEFAULT_SIZE = 20;

    /** سقف اندازه صفحه — مطابق {@code docs/api.md} بخش ۷. */
    public static final int MAX_SIZE = 100;

    private PageableFactory() {
        throw new AssertionError("کلاس ابزار نباید نمونه‌سازی شود");
    }

    /**
     * ساخت صفحه‌بندی امن.
     *
     * @param page         شماره صفحه (از صفر)؛ منفی به صفر کلمپ می‌شود
     * @param size         اندازه صفحه؛ صفر یا منفی به پیش‌فرض و بیش از
     *                     سقف به سقف کلمپ می‌شود
     * @param sort         رشته مرتب‌سازی به شکل {@code field,direction}؛
     *                     {@code null} یا خالی یعنی {@code defaultSort}
     * @param allowedFields فیلدهای مجاز — کلیدواژه‌های دقیق و حساس به بزرگی حروف
     * @param defaultSort  مرتب‌سازی پیش‌فرض، مثل {@code "createdAt,desc"}
     * @return صفحه‌بندی آماده برای پرس‌وجو
     * @throws AppException با کد {@link ErrorCode#VALIDATION_FAILED} اگر
     *                      فیلد یا جهت مرتب‌سازی مجاز نباشد
     */
    public static Pageable of(int page,
                              int size,
                              String sort,
                              Set<String> allowedFields,
                              String defaultSort) {

        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String requested = (sort == null || sort.isBlank()) ? defaultSort : sort;

        return PageRequest.of(safePage, safeSize, parseSort(requested, allowedFields));
    }

    private static Sort parseSort(String sort, Set<String> allowedFields) {
        if (sort == null || sort.isBlank()) {
            return Sort.unsorted();
        }

        String[] parts = sort.split(",", -1);
        String field = parts[0].trim();

        if (!allowedFields.contains(field)) {
            // ⚠️ نام فیلد در پیام می‌آید ولی فهرست مجاز نه. کاربر مجاز
            // فهرست را از مستندات می‌داند؛ مهاجم نباید با یک درخواست،
            // نقشه ستون‌های دیتابیس را بگیرد.
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "مرتب‌سازی روی «" + field + "» مجاز نیست.");
        }

        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1 && !parts[1].isBlank()) {
            String raw = parts[1].trim();
            try {
                direction = Sort.Direction.fromString(raw);
            } catch (IllegalArgumentException e) {
                // ⚠️ fromString برای «ascending» هم استثنا می‌دهد؛ عمداً
                // همان پیام را می‌دهیم تا کلاینت بداند قرارداد چیست.
                throw new AppException(ErrorCode.VALIDATION_FAILED,
                        "جهت مرتب‌سازی «" + raw + "» نامعتبر است؛ "
                                + "فقط asc و desc مجاز است.");
            }
        }

        return Sort.by(direction, field);
    }
}
