package ir.omid.smartteacher.eitaa;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * توکن ربات ایتایار — مسیر الف (گروه و کانال).
 *
 * <p><b>⚠️ این نوع یک راز حمل می‌کند.</b> سه محافظ دارد:
 * <ol>
 *   <li>{@code @JsonIgnore} روی مقدار — اگر روزی این رکورد به بدنه
 *       پاسخ یا سند OpenAPI راه پیدا کرد، مقدارش سریال نمی‌شود.</li>
 *   <li>{@link #toString()} ماسک‌شده — اگر در Log بیفتد، توکن نمی‌افتد.</li>
 *   <li>رکورد است، پس {@code equals}/{@code hashCode} هم خودکار ماسک
 *       نمی‌شوند؛ به همین دلیل هیچ‌جا این نوع را به‌عنوان کلید Map
 *       استفاده نکنید.</li>
 * </ol>
 *
 * <p>خودِ رکورد باید از {@code SecretEncryptor} رد شود و رمزنگاری‌شده در
 * دیتابیس بنشیند ({@code docs/eitaa.md} بخش ۱۲). این نوع فقط حامل
 * مقدار رمزگشایی‌شده در حافظه است.
 *
 * @param value توکن خام — هرگز در Log، Swagger یا حسابرسی نمی‌آید
 */
public record EitaaBotToken(@JsonIgnore String value) {

    public EitaaBotToken {
        if (value == null || value.isBlank()) {
            // ⚠️ مقدار در پیام نمی‌آید — فقط اینکه خالی است.
            throw new IllegalArgumentException("توکن ربات ایتا نباید خالی باشد");
        }
    }

    @Override
    public String toString() {
        return "EitaaBotToken[REDACTED]";
    }
}
