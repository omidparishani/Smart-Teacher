package ir.omid.smartteacher.exam.internal.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * تولید آزمون از کتاب/بازه صفحه با Mock AI.
 *
 * @param mcqCount تعداد چهارگزینه‌ای (پیش‌فرض ۳)
 * @param trueFalseCount تعداد درست/نادرست (پیش‌فرض ۰) — همیشه یک تشریحی هم اضافه می‌شود
 */
public record GenerateExamRequest(
        @NotNull UUID classroomId,
        @NotNull UUID bookId,
        @NotNull @Min(1) Integer pageFrom,
        @NotNull @Min(1) Integer pageTo,
        @NotBlank String title,
        String description,
        Integer durationMinutes,
        BigDecimal totalScore,
        @Min(1) Integer mcqCount,
        @Min(0) Integer trueFalseCount,
        String difficultyDistribution,
        String questionTypeDistribution
) {
}
