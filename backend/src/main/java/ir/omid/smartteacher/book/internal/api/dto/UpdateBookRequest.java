package ir.omid.smartteacher.book.internal.api.dto;

import ir.omid.smartteacher.book.internal.domain.BookStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * ورودی ویرایش کامل کتاب.
 *
 * @param title      عنوان
 * @param publisher  ناشر
 * @param gradeLevel پایه
 * @param subjectId  درس
 * @param status     وضعیت
 * @param coverFileId شناسه فایل جلد؛ {@code null} یعنی بدون جلد
 */
public record UpdateBookRequest(
        @NotBlank(message = "عنوان کتاب اجباری است.")
        @Size(max = 250, message = "عنوان کتاب نمی‌تواند بیش از ۲۵۰ نویسه باشد.")
        String title,

        @Size(max = 150, message = "نام ناشر نمی‌تواند بیش از ۱۵۰ نویسه باشد.")
        String publisher,

        @Size(max = 50, message = "پایه تحصیلی نمی‌تواند بیش از ۵۰ نویسه باشد.")
        String gradeLevel,

        UUID subjectId,

        @NotNull(message = "وضعیت کتاب اجباری است.")
        BookStatus status,

        UUID coverFileId
) {
}
