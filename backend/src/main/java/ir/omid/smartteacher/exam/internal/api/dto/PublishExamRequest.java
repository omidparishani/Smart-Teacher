package ir.omid.smartteacher.exam.internal.api.dto;

import jakarta.validation.constraints.Size;

/**
 * ورودی انتشار آزمون — ارسال اختیاری به گروه ایتا.
 *
 * @param eitaaChatId شناسه یا username گروه ایتا
 */
public record PublishExamRequest(
        @Size(max = 100, message = "شناسه چت ایتا نمی‌تواند بیش از ۱۰۰ نویسه باشد.")
        String eitaaChatId
) {
}
