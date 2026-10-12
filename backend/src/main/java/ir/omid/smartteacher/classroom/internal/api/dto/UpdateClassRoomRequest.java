package ir.omid.smartteacher.classroom.internal.api.dto;

import ir.omid.smartteacher.classroom.internal.domain.ClassRoomStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * ورودی ویرایش کلاس — جایگزینی کامل ({@code PUT}).
 *
 * <p><b>⚠️ چرا {@code homeroomTeacherId} اینجا هم نیست؟</b> همان دلیل
 * {@link CreateClassRoomRequest}: بررسی عضویت معلم تازه، نیازمند فهرست
 * اعضای مستأجر است که فاز ۴ می‌آورد.
 *
 * <p>⚠️ {@code status} اینجا هست. کلاس را می‌توان بایگانی کرد
 * ({@code ARCHIVED}) بدون حذفش.
 *
 * <p><b>⚠️ چرا {@code status} اینجا {@code @NotNull} است ولی در ساخت
 * نبود؟</b> چون {@code PUT} یعنی «این رکورد را با این مقادیر جایگزین
 * کن». نبودن {@code status} در یک جایگزینی کامل یعنی «نمی‌دانم»، و
 * «نمی‌دانم» در فیلدی که مقدارش حتماً لازم است، باید صریح گفته شود —
 * نه اینکه بی‌صدا به مقدار قبلی برگردد. اگر می‌خواستیم به‌روزرسانی
 * جزئی، متد {@code PATCH} بود.
 *
 * @param name         نام کلاس — اجباری
 * @param gradeLevel   پایه تحصیلی — اختیاری (ارسال {@code null} یعنی پاک‌کردن)
 * @param academicYear سال تحصیلی — اختیاری
 * @param subjectId    شناسه درس — اختیاری؛ {@code null} یعنی کلاس بی‌درس شود
 * @param capacity     ظرفیت — اختیاری
 * @param status       وضعیت کلاس — اجباری
 */
public record UpdateClassRoomRequest(
        @NotBlank(message = "نام کلاس اجباری است.")
        @Size(max = 150, message = "نام کلاس نمی‌تواند بیش از ۱۵۰ نویسه باشد.")
        String name,

        @Size(max = 50, message = "پایه تحصیلی نمی‌تواند بیش از ۵۰ نویسه باشد.")
        String gradeLevel,

        @Size(max = 20, message = "سال تحصیلی نمی‌تواند بیش از ۲۰ نویسه باشد.")
        String academicYear,

        UUID subjectId,

        @Min(value = 1, message = "ظرفیت کلاس باید حداقل ۱ باشد.")
        @Max(value = 1000, message = "ظرفیت کلاس نمی‌تواند بیش از ۱۰۰۰ باشد.")
        Integer capacity,

        @NotNull(message = "وضعیت کلاس اجباری است.")
        ClassRoomStatus status
) {
}
