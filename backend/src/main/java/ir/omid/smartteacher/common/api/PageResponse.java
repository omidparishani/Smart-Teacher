package ir.omid.smartteacher.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * پوشش استاندارد پاسخ‌های صفحه‌بندی‌شده ({@code docs/api.md}).
 *
 * <p><b>چرا {@code Page} خام برگردانده نمی‌شود؟</b> چون شکل JSON آن
 * جزئیات پیاده‌سازی Spring Data را بیرون می‌ریزد ({@code pageable}،
 * {@code sort}، {@code numberOfElements}) و قرارداد API را به کتابخانه
 * گره می‌زند. تغییر نسخه Spring Data نباید API را بشکند.
 *
 * @param content       آیتم‌های صفحه جاری
 * @param page          شماره صفحه (از صفر)
 * @param size          اندازه صفحه
 * @param totalElements کل تعداد آیتم‌ها در همه صفحات
 * @param totalPages    تعداد کل صفحات
 * @param first         آیا صفحه اول است؟
 * @param last          آیا صفحه آخر است؟
 * @param <T>           نوع آیتم
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageResponse<T> from(Page<T> source) {
        return new PageResponse<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isFirst(),
                source.isLast());
    }

    /**
     * تبدیل صفحه‌بندی همراه با نگاشت موجودیت به DTO.
     *
     * @param source    صفحه مبدأ
     * @param converter تابع نگاشت
     * @param <E>       نوع مبدأ
     * @param <T>       نوع مقصد
     * @return صفحه نگاشته‌شده
     */
    public static <E, T> PageResponse<T> from(Page<E> source, Function<E, T> converter) {
        return from(source.map(converter));
    }

    /** پاسخ خالی — برای وقتی که فیلتر نتیجه‌ای نداشته. */
    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(List.of(), page, size, 0L, 0, true, true);
    }
}
