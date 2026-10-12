package ir.omid.smartteacher.security.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * پیکربندی رمزنگاری.
 *
 * <p><b>⚠️ هیچ مقدار پیش‌فرضی اینجا نیست — عمدی است.</b> اگر
 * {@code masterKey} مقدار پیش‌فرض داشت، یک استقرار اشتباه در محیط
 * production بی‌صدا با کلید شناخته‌شده بالا می‌آمد و همه کلیدهای API
 * مشتریان با کلیدی رمز می‌شدند که در مخزن گیت است. نبودِ پیش‌فرض باعث
 * می‌شود اپلیکیشن <b>بالا نیاید</b> — که رفتار درست است.
 *
 * <p>مقدار از متغیر محیطی {@code ENCRYPTION_MASTER_KEY} می‌آید
 * ({@code application.yml}) و باید Base64 یک کلید ۳۲ بایتی باشد.
 * ساختش: {@code openssl rand -base64 32} یا
 * {@code scripts/generate-keys.sh}.
 *
 * @param masterKey کلید اصلی Base64 — فقط از محیط اجرا، هرگز از فایل مخزن
 */
@ConfigurationProperties(prefix = "security.encryption")
public record EncryptionProperties(String masterKey) {

    /**
     * ⚠️ این متد هرگز نباید در Log بیفتد. برای نمایش ایمن از
     * {@code SecretEncryptor.mask} استفاده کنید.
     */
    @Override
    public String toString() {
        return "EncryptionProperties{masterKey=***}";
    }
}
