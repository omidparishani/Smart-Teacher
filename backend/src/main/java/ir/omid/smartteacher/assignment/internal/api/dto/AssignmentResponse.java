package ir.omid.smartteacher.assignment.internal.api.dto;

import ir.omid.smartteacher.assignment.internal.domain.AssignmentStatus;
import ir.omid.smartteacher.assignment.internal.domain.AssignmentType;
import ir.omid.smartteacher.assignment.internal.domain.EitaaSendStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * پاسخ تکلیف.
 *
 * @param id                   شناسه
 * @param classroomId          کلاس
 * @param classroomName        نام کلاس (اگر در دسترس باشد)
 * @param bookId               کتاب اختیاری
 * @param pageFrom             از صفحه
 * @param pageTo               تا صفحه
 * @param title                عنوان
 * @param description          توضیح
 * @param type                 نوع
 * @param maxScore             سقف نمره
 * @param dueAt                موعد
 * @param status               وضعیت
 * @param aiEvaluationEnabled  تصحیح AI
 * @param submissionCount      تعداد ارسال‌ها
 * @param eitaaChatId          شناسه چت ایتا (اگر ارسال شده)
 * @param eitaaMessageStatus   نتیجه ارسال به ایتا
 * @param createdAt            زمان ساخت
 * @param updatedAt            زمان به‌روزرسانی
 */
public record AssignmentResponse(
        UUID id,
        UUID classroomId,
        String classroomName,
        UUID bookId,
        Integer pageFrom,
        Integer pageTo,
        String title,
        String description,
        AssignmentType type,
        BigDecimal maxScore,
        Instant dueAt,
        AssignmentStatus status,
        boolean aiEvaluationEnabled,
        long submissionCount,
        String eitaaChatId,
        EitaaSendStatus eitaaMessageStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
