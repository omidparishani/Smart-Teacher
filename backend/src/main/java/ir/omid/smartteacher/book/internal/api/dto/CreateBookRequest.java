package ir.omid.smartteacher.book.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * ورودی ساخت کتاب.
 *
 * @param title      عنوان — اجباری
 * @param publisher  ناشر — اختیاری
 * @param gradeLevel پایه — اختیاری
 * @param subjectId  درس — اختیاری؛ باید مال همین مستأجر باشد
 */
public record CreateBookRequest(
        @NotBlank(message = "عنوان کتاب اجباری است.")
        @Size(max = 250, message = "عنوان کتاب نمی‌تواند بیش از ۲۵۰ نویسه باشد.")
        String title,

        @Size(max = 150, message = "نام ناشر نمی‌تواند بیش از ۱۵۰ نویسه باشد.")
        String publisher,

        @Size(max = 50, message = "پایه تحصیلی نمی‌تواند بیش از ۵۰ نویسه باشد.")
        String gradeLevel,

        UUID subjectId
) {
}
