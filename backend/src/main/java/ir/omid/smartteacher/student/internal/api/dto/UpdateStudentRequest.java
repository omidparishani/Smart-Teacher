package ir.omid.smartteacher.student.internal.api.dto;

import ir.omid.smartteacher.student.internal.domain.Gender;
import ir.omid.smartteacher.student.internal.domain.StudentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * ورودی ویرایش دانش‌آموز — جایگزینی کامل ({@code PUT}).
 *
 * <p>⚠️ {@code status} اینجا {@code @NotNull} است، همان استدلال
 * {@code UpdateClassRoomRequest}: در یک جایگزینی کامل، نبودن فیلدی که
 * مقدارش حتماً لازم است یعنی «نمی‌دانم»، و باید صریح گفته شود — نه
 * اینکه بی‌صدا به مقدار قبلی برگردد.
 *
 * @param fullName       نام کامل — اجباری
 * @param nationalCode   کد ملی — اختیاری ({@code null} یعنی پاک‌کردن)
 * @param guardianName   نام ولی — اختیاری
 * @param guardianMobile موبایل ولی — اختیاری
 * @param birthDate      تاریخ تولد — اختیاری
 * @param gender         جنسیت — اختیاری
 * @param status         وضعیت — اجباری
 */
public record UpdateStudentRequest(
        @NotBlank(message = "نام دانش‌آموز اجباری است.")
        @Size(max = 150, message = "نام دانش‌آموز نمی‌تواند بیش از ۱۵۰ نویسه باشد.")
        String fullName,

        @Size(max = 20, message = "کد ملی نمی‌تواند بیش از ۲۰ نویسه باشد.")
        String nationalCode,

        @Size(max = 150, message = "نام ولی نمی‌تواند بیش از ۱۵۰ نویسه باشد.")
        String guardianName,

        @Pattern(regexp = "^09\\d{9}$",
                message = "موبایل ولی باید ۱۱ رقم و با ۰۹ شروع شود.")
        String guardianMobile,

        @PastOrPresent(message = "تاریخ تولد نمی‌تواند در آینده باشد.")
        LocalDate birthDate,

        Gender gender,

        @NotNull(message = "وضعیت دانش‌آموز اجباری است.")
        StudentStatus status
) {
}
