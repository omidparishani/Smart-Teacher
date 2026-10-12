package ir.omid.smartteacher.ai.internal.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * پاسخ گفت‌وگوی دستیار.
 *
 * @param content           متن پاسخ
 * @param model             مدل استفاده‌شده
 * @param provider          کد Provider
 * @param promptTokens      توکن ورودی
 * @param completionTokens  توکن خروجی
 * @param cost              هزینه محاسبه‌شده
 * @param usageRecordId     شناسه رکورد مصرف
 */
public record AiChatResponse(
        String content,
        String model,
        String provider,
        int promptTokens,
        int completionTokens,
        BigDecimal cost,
        UUID usageRecordId
) {
}
