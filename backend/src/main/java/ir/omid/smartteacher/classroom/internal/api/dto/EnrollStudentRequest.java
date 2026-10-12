package ir.omid.smartteacher.classroom.internal.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * ورودی ثبت‌نام دانش‌آموز در کلاس.
 *
 * <p><b>⚠️ چرا فقط {@code studentId} و نه نام؟</b> چون دانش‌آموز باید
 * از قبل در مدرسه ساخته شده باشد ({@code POST /students}). ساخت و
 * ثبت‌نام دو عمل جدا هستند: یکی پرونده مدرسه می‌سازد، دیگری عضویت در
 * یک کلاس. قاطی‌کردنشان یعنی معلم نتواند همان دانش‌آموز را در کلاس
 * دوم (مثلاً فوق‌برنامه) ثبت کند بدون ساختن پرونده تکراری.
 *
 * @param studentId  شناسه دانش‌آموز — اجباری
 * @param rollNumber شماره حضور — اختیاری
 */
public record EnrollStudentRequest(
        @NotNull(message = "شناسه دانش‌آموز اجباری است.")
        UUID studentId,

        @Size(max = 20, message = "شماره حضور نمی‌تواند بیش از ۲۰ نویسه باشد.")
        String rollNumber
) {
}
