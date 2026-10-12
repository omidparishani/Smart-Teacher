/**
 * رمزنگاری کلیدهای حساس — <b>رابط نام‌دار</b> ماژول {@code security}.
 *
 * <p>ماژول‌هایی مثل {@code ai} برای ذخیره کلید API به
 * {@link ir.omid.smartteacher.security.crypto.EncryptedStringConverter}
 * و {@link ir.omid.smartteacher.security.crypto.SecretEncryptor} نیاز
 * دارند. باز کردن فقط همین زیرپکیج، مرز CLOSED ماژول امنیت را حفظ
 * می‌کند (همان الگوی {@code security::jwt}).
 */
@org.springframework.modulith.NamedInterface("crypto")
package ir.omid.smartteacher.security.crypto;
