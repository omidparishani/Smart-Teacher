package ir.omid.smartteacher.assignment.internal.api.dto;

import ir.omid.smartteacher.assignment.internal.domain.AssignmentStatus;
import ir.omid.smartteacher.assignment.internal.domain.AssignmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * ورودی ویرایش تکلیف.
 */
public record UpdateAssignmentRequest(
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

        Boolean aiEvaluationEnabled,

        AssignmentStatus status
) {
}
