package ir.omid.smartteacher.eitaa;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * توکن برنامک — مسیر ب (پیام به کاربر خصوصی).
 *
 * <p>محافظ‌ها دقیقاً مانند {@link EitaaBotToken} است: {@code @JsonIgnore}
 * روی مقدار و {@code toString()} ماسک‌شده.
 *
 * <p><b>⚠️ تفاوت مهم با توکن ربات:</b> این توکن <i>اعتبار مصرفی</i>
 * دارد. هر پیام به هر کاربر یک اعتبار از ۵۰٬۰۰۰ اعتبار رایگان کم
 * می‌کند ({@code docs/eitaa.md} بخش ۵). پس مصرفش باید شمارش و محدود
 * شود — نه فقط از نظر امنیتی، بلکه از نظر هزینه.
 *
 * @param value توکن خام — هرگز در Log، Swagger یا حسابرسی نمی‌آید
 */
public record EitaaAppToken(@JsonIgnore String value) {

    public EitaaAppToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("توکن برنامک ایتا نباید خالی باشد");
        }
    }

    @Override
    public String toString() {
        return "EitaaAppToken[REDACTED]";
    }
}
