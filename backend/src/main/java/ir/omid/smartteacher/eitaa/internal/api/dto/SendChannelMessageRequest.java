package ir.omid.smartteacher.eitaa.internal.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** ارسال به کانال با گزینه‌های سنجاق/حذف. */
public record SendChannelMessageRequest(
        @NotBlank String text,
        Boolean pin,
        @Min(1) Integer viewCountForDelete
) {
}
