package ir.omid.smartteacher.classroom.internal.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * نمای درس در پاسخ‌های API.
 *
 * <p>⚠️ این record فقط خوانده می‌شود و هیچ ستون دیتابیس اضافه‌ای بیرون
 * نمی‌دهد. {@code deletedAt} عمداً اینجا نیست: کلاینتی که فقط رکوردهای
 * حذف‌نشده را می‌بیند، به این فیلد نیازی ندارد و بودنش فقط این پرسش را
 * می‌سازد که «پس چرا دارمش؟».
 *
 * @param id          شناسه درس
 * @param name        نام درس، مثل «ریاضی»
 * @param code        کد درس؛ ممکن است {@code null} باشد
 * @param gradeLevel  پایه تحصیلی؛ ممکن است {@code null} باشد
 * @param createdAt   زمان ساخت
 * @param updatedAt   زمان آخرین تغییر
 */
public record SubjectResponse(
        UUID id,
        String name,
        String code,
        String gradeLevel,
        Instant createdAt,
        Instant updatedAt
) {
}
