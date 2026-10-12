package ir.omid.smartteacher.book.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * درخواست ساخت فصل.
 *
 * @param title     عنوان فصل
 * @param orderIndex ترتیب؛ اگر تهی باشد انتهای فهرست
 * @param startPage  صفحه شروع اختیاری
 * @param endPage    صفحه پایان اختیاری
 */
public record CreateChapterRequest(
        @NotBlank @Size(max = 250) String title,
        Integer orderIndex,
        Integer startPage,
        Integer endPage
) {
}
