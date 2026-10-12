package ir.omid.smartteacher.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * نگاشت خودکار ستون‌های حساس به مقدار رمزشده.
 *
 * <p>روی فیلد موجودیت می‌نویسید:
 * <pre>{@code
 * @Convert(converter = EncryptedStringConverter.class)
 * @Column(name = "api_key_encrypted")
 * private String apiKey;
 * }</pre>
 * و از آن لحظه، هر نوشتن و خواندن از دیتابیس خودکار رمز/رمزگشایی
 * می‌شود. منطق کسب‌وکار فقط متن خام را می‌بیند.
 *
 * <p><b>چرا Converter و نه فراخوانی دستی در سرویس؟</b> چون یک
 * فراخوانی فراموش‌شده یعنی کلید API متن خام در دیتابیس — و این خطا در
 * بازبینی کد به‌سختی دیده می‌شود. Converter در لایه نگاشت می‌نشیند و
 * امکان فراموش‌کردن را از بین می‌برد: هر مسیری که به آن ستون بنویسد،
 * خودکار رمز می‌شود.
 *
 * <p><b>⚠️ چرا فیلد {@code static}؟</b> چون JPA خودش نمونه Converter را
 * می‌سازد، نه Spring؛ پس تزریق معمولی در سازنده کار نمی‌کند.
 * {@link EncryptorHolder} که یک Bean اسپرینگی است، مقدار را در همان
 * لحظه راه‌اندازی می‌گذارد. این کار به‌عمد از تکیه بر
 * {@code SpringBeanContainer} خودِ Hibernate پرهیز می‌کند که درستی‌اش
 * وابسته به جزئیات پیکربندی است و خطایش فقط در زمان اجرا دیده می‌شود.
 *
 * <p><b>⚠️ نکته مهم:</b> این Converter برای ستون‌هایی است که همیشه باید
 * رمز باشند. اگر ستونی گاهی خام ذخیره شده باشد، رمزگشایی‌اش استثنا
 * می‌دهد. مهاجرت داده‌های موجود باید قبل از فعال‌کردن Converter انجام
 * شود.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static volatile SecretEncryptor encryptor;

    /**
     * پل بین Spring و JPA.
     *
     * <p>کلاس تودرتو و {@code static} است تا اسکن کامپوننت آن را پیدا
     * کند و کنار همان چیزی بماند که سیم‌کشی می‌کند.
     */
    @Component
    static class EncryptorHolder {

        EncryptorHolder(SecretEncryptor secretEncryptor) {
            EncryptedStringConverter.encryptor = secretEncryptor;
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return requireEncryptor().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return requireEncryptor().decrypt(dbData);
    }

    private static SecretEncryptor requireEncryptor() {
        SecretEncryptor current = encryptor;
        if (current == null) {
            // رسیدن به اینجا یعنی Converter بیرون از زمینه Spring استفاده
            // شده — مثل تستی که موجودیت را دستی ساخته. در محیط اجرا
            // هرگز رخ نمی‌دهد چون EncryptorHolder یک Bean است.
            throw new IllegalStateException(
                    "EncryptedStringConverter پیش از آماده‌شدن زمینه Spring استفاده شد");
        }
        return current;
    }
}
