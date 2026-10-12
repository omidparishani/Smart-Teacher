package ir.omid.smartteacher.assignment.internal.api.dto;

import jakarta.validation.constraints.Size;

/**
 * ورودی انتشار تکلیف.
 *
 * <p>{@code eitaaChatId} اختیاری است. اگر نباشد، وضعیت
 * {@code SKIPPED} ثبت می‌شود و ارسال انجام نمی‌شود. اگر باشد،
 * {@code EitaaAdapter} صدا زده می‌شود — شکست ارسال انتشار را باطل
 * نمی‌کند ({@code docs/eitaa.md} بخش ۹).
 *
 * @param eitaaChatId شناسه یا username گروه ایتا
 */
public record PublishAssignmentRequest(
        @Size(max = 100, message = "شناسه چت ایتا نمی‌تواند بیش از ۱۰۰ نویسه باشد.")
        String eitaaChatId
) {
}
