package ir.omid.smartteacher.eitaa.internal.api.dto;

/**
 * ویرایش کانال ایتا — به‌روزرسانی <b>جزئی</b>.
 *
 * <p>همان قرارداد {@link UpdateEitaaGroupRequest}:
 * {@code null} یعنی «تغییر نده»، رشتهٔ خالی برای {@code title} یا
 * {@code username} یعنی «پاک کن»، و {@code eitaaChatId} اگر خالی/‌{@code null}
 * باشد دست‌نخورده می‌ماند.
 */
public record UpdateEitaaChannelRequest(
        String eitaaChatId,
        String title,
        String username
) {}
