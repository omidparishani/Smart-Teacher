package ir.omid.smartteacher.assignment.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * ورودی ثبت پاسخ متنی — مسیر Mini App (نه چت ایتا).
 *
 * @param studentId  شناسه دانش‌آموز
 * @param answerText متن پاسخ
 */
public record CreateSubmissionRequest(
        @NotNull(message = "شناسه دانش‌آموز اجباری است.")
        UUID studentId,

        @NotBlank(message = "متن پاسخ اجباری است.")
        String answerText
) {
}
