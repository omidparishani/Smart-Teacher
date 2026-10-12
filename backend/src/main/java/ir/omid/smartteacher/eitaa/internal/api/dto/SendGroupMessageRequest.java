package ir.omid.smartteacher.eitaa.internal.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * ارسال فوری پیام به گروه.
 *
 * @param pin                 سنجاق پس از ارسال (پارامتر بومی ایتا)
 * @param viewCountForDelete  حذف خودکار پس از N بازدید
 */
public record SendGroupMessageRequest(
        @NotBlank String text,
        Boolean pin,
        @Min(1) Integer viewCountForDelete
) {
    public SendGroupMessageRequest(String text) {
        this(text, null, null);
    }
}
