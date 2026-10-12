package ir.omid.smartteacher.ai.internal.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ورودی گفت‌وگوی دستیار معلم — {@code POST /api/v1/ai/chat}.
 *
 * @param message متن پیام کاربر
 * @param model   شناسه مدل اختیاری؛ اگر خالی باشد Mock پیش‌فرض می‌گیرد
 */
public record AiChatRequest(
        @NotBlank(message = "پیام اجباری است.")
        @Size(max = 8_000, message = "پیام نمی‌تواند بیش از ۸۰۰۰ نویسه باشد.")
        String message,

        @Size(max = 100, message = "شناسه مدل نمی‌تواند بیش از ۱۰۰ نویسه باشد.")
        String model
) {
}
