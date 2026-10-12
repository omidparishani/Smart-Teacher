package ir.omid.smartteacher.assignment.internal.api.dto;

import ir.omid.smartteacher.assignment.internal.domain.AssignmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ورودی ساخت تکلیف.
 *
 * @param classroomId         کلاس — اجباری؛ باید برای معلم قابل‌دسترس باشد
 * @param title               عنوان
 * @param description         توضیح اختیاری
 * @param type                نوع؛ پیش‌فرض HOMEWORK اگر null
 * @param maxScore            سقف نمره
 * @param dueAt               موعد
 * @param bookId              کتاب اختیاری
 * @param pageFrom            از صفحه
 * @param pageTo              تا صفحه
 * @param aiEvaluationEnabled فعال‌سازی تصحیح AI
 */
public record CreateAssignmentRequest(
        @NotNull(message = "شناسه کلاس اجباری است.")
        UUID classroomId,

        @NotBlank(message = "عنوان تکلیف اجباری است.")
        @Size(max = 250, message = "عنوان تکلیف نمی‌تواند بیش از ۲۵۰ نویسه باشد.")
        String title,

        String description,

        AssignmentType type,

        @DecimalMin(value = "0.01", message = "سقف نمره باید مثبت باشد.")
        BigDecimal maxScore,

        Instant dueAt,

        UUID bookId,

        Integer pageFrom,

        Integer pageTo,

        Boolean aiEvaluationEnabled
) {
}
