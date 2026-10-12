package ir.omid.smartteacher.classroom.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ورودی ساخت درس.
 *
 * <p><b>⚠️ چرا {@code tenantId} در ورودی نیست؟</b> چون مستأجر <i>هرگز</i>
 * از بدنه درخواست خوانده نمی‌شود. اگر می‌شد، مهاجم با عوض‌کردن یک فیلد
 * JSON می‌توانست در مستأجر دیگری بنویسد. مستأجر فقط از توکن می‌آید
 * ({@code docs/security.md}، تهدید T11).
 *
 * @param name       نام درس — اجباری
 * @param code       کد درس — اختیاری، در محدوده مستأجر یکتا
 * @param gradeLevel پایه تحصیلی — اختیاری
 */
public record CreateSubjectRequest(
        @NotBlank(message = "نام درس اجباری است.")
        @Size(max = 150, message = "نام درس نمی‌تواند بیش از ۱۵۰ نویسه باشد.")
        String name,

        @Size(max = 50, message = "کد درس نمی‌تواند بیش از ۵۰ نویسه باشد.")
        String code,

        @Size(max = 50, message = "پایه تحصیلی نمی‌تواند بیش از ۵۰ نویسه باشد.")
        String gradeLevel
) {
}
