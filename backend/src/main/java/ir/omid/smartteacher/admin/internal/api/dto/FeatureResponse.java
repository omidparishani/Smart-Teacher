package ir.omid.smartteacher.admin.internal.api.dto;

import java.util.UUID;

/**
 * ردیف کاتالوگ امکانات.
 *
 * @param id             شناسه
 * @param code           کد یکتا
 * @param name           نام نمایشی
 * @param category       دسته
 * @param description    توضیح
 * @param defaultEnabled پیش‌فرض فعال بودن
 * @param sortOrder      ترتیب نمایش
 */
public record FeatureResponse(
        UUID id,
        String code,
        String name,
        String category,
        String description,
        boolean defaultEnabled,
        int sortOrder
) {
}
