package ir.omid.smartteacher.eitaa.internal.api.dto;

import java.util.UUID;

/**
 * نمای مستأجر فعال در پاسخ‌های احراز هویت.
 *
 * <p>شکل آن مطابق نمونه {@code docs/api.md} بخش ۵ است:
 * {@code { "id", "name", "type" }}.
 *
 * @param id   شناسه مستأجر
 * @param name نام مستأجر
 * @param type نوع مستأجر — {@code TEACHER}، {@code SCHOOL}،
 *             {@code ORGANIZATION}
 */
public record AuthTenantView(UUID id, String name, String type) {
}
