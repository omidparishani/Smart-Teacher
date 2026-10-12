package ir.omid.smartteacher.security.crypto;

import ir.omid.smartteacher.common.error.EncryptionException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * رمزنگاری متقارن کلیدهای API با AES-256-GCM.
 *
 * <p>الزام صریح {@code docs/security.md} و بخش ۵۳ سند اصلی: کلیدهای API
 * ارائه‌دهندگان <b>هرگز</b> به‌صورت متن خام ذخیره نمی‌شوند. اگر دیتابیس
 * لو برود یا یک پشتیبان دزدیده شود، مهاجم بدون کلید اصلی هیچ کاری با
 * مقادیر رمزشده نمی‌تواند بکند.
 *
 * <p><b>قالب ذخیره‌شده</b> (سپس Base64 و در ستون متنی):
 * <pre>
 *   بایت ۰      → نسخه قالب (فعلاً ۱)
 *   بایت ۱..۱۲  → IV تصادفی ۱۲ بایتی
 *   بایت ۱۳..   → متن رمز
 *   بایت آخر ۱۶ → برچسب احراز اصالت GCM
 * </pre>
 *
 * <p><b>چرا نسخه در بایت اول؟</b> برای چرخش کلید در آینده. اگر روزی
 * الگوریتم یا طول کلید عوض شود، نسخه ۲ نوشته می‌شود و رمزگشایی می‌داند
 * داده قدیمی را با کدام مسیر بخواند. افزودن بعدیِ نسخه، مهاجرت داده‌ی
 * رمزشده را لازم می‌کند؛ افزودنش از ابتدا رایگان است.
 *
 * <p><b>چرا GCM و نه CBC؟</b> GCM هم رمزنگاری می‌کند و هم احراز اصالت
 * (AEAD): هر تغییری در بایت‌های ذخیره‌شده در زمان رمزگشایی شناسایی
 * می‌شود. با CBC، مهاجمی که به دیتابیس دسترسی نوشتنی دارد می‌تواند
 * بایت‌ها را دست‌کاری کند و ما متوجه نشویم.
 *
 * <p><b>⚠️ خطر بحرانی IV:</b> استفاده دوباره از یک IV با یک کلید، در
 * GCM کلید را می‌شکند. پس برای <i>هر</i> عملیات رمزنگاری یک IV تازه از
 * {@link SecureRandom} گرفته می‌شود و IV به‌عنوان بخشی از خروجی ذخیره
 * می‌گردد. IV نیازی به مخفی‌بودن ندارد — فقط نباید تکرار شود.
 */
@Component
public class SecretEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";

    /** AES-256 → کلید ۳۲ بایتی. */
    private static final int KEY_LENGTH_BYTES = 32;

    /** ۹۶ بیت — طول IV توصیه‌شده NIST برای GCM. */
    private static final int IV_LENGTH_BYTES = 12;

    /** برچسب احراز اصالت ۱۲۸ بیتی — حداکثر پشتیبانی‌شده GCM. */
    private static final int TAG_LENGTH_BITS = 128;

    private static final int TAG_LENGTH_BYTES = TAG_LENGTH_BITS / 8;

    /** نسخه فعلی قالب. */
    private static final byte FORMAT_VERSION = 1;

    private static final int VERSION_OFFSET = 0;
    private static final int IV_OFFSET = VERSION_OFFSET + 1;
    private static final int CIPHERTEXT_OFFSET = IV_OFFSET + IV_LENGTH_BYTES;

    private static final int MIN_PAYLOAD_BYTES =
            CIPHERTEXT_OFFSET + TAG_LENGTH_BYTES;

    /** تعداد کاراکترهای آشکار در مقدار پوشانده‌شده. */
    private static final int MASK_VISIBLE_CHARS = 4;

    private static final int MASK_DOTS = 8;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public SecretEncryptor(EncryptionProperties properties) {
        this.key = buildKey(properties.masterKey());
    }

    private static SecretKey buildKey(String base64MasterKey) {
        if (base64MasterKey == null || base64MasterKey.isBlank()) {
            throw EncryptionException.invalidKey(
                    "کلید اصلی رمزنگاری تنظیم نشده است؛ متغیر محیطی ENCRYPTION_MASTER_KEY را مقدار بدهید");
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64MasterKey.trim());
        } catch (IllegalArgumentException e) {
            // ⚠️ مقدار کلید در پیام نمی‌آید — فقط اینکه Base64 نبود.
            throw EncryptionException.invalidKey("کلید اصلی رمزنگاری Base64 معتبر نیست");
        }

        if (decoded.length != KEY_LENGTH_BYTES) {
            // طول کلید راز نیست و دانستنش عیب‌یابی را ممکن می‌کند.
            throw EncryptionException.invalidKey(
                    "کلید اصلی رمزنگاری باید " + KEY_LENGTH_BYTES + " بایت باشد اما "
                            + decoded.length + " بایت است");
        }

        SecretKey built = new SecretKeySpec(decoded, KEY_ALGORITHM);
        Arrays.fill(decoded, (byte) 0);
        return built;
    }

    // ─────────────────────────── رمزنگاری ───────────────────────────

    /**
     * رمزنگاری یک رشته.
     *
     * @param plaintext متن خام؛ {@code null} مجاز است و {@code null} برمی‌گردد
     * @return مقدار Base64 آماده ذخیره، یا {@code null}
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        byte[] payload = encryptBytes(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(payload);
    }

    /**
     * رمزگشایی یک رشته.
     *
     * @param encoded مقدار Base64؛ {@code null} مجاز است و {@code null} برمی‌گردد
     * @return متن خام
     * @throws EncryptionException اگر داده دست‌کاری شده یا کلید عوض شده باشد
     */
    public String decrypt(String encoded) {
        if (encoded == null) {
            return null;
        }
        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(encoded.trim());
        } catch (IllegalArgumentException e) {
            throw EncryptionException.decryptionFailed(e);
        }
        return new String(decryptBytes(payload), StandardCharsets.UTF_8);
    }

    /**
     * رمزنگاری آرایه بایت.
     *
     * @param plaintext داده خام
     * @return بایت‌های قالب‌بندی‌شده شامل نسخه، IV، متن رمز و برچسب
     */
    public byte[] encryptBytes(byte[] plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            // IV تازه برای هر عملیات — تکرار IV کلید GCM را می‌شکند.
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertextWithTag = cipher.doFinal(plaintext);

            return ByteBuffer.allocate(VERSION_OFFSET + 1 + IV_LENGTH_BYTES + ciphertextWithTag.length)
                    .put(FORMAT_VERSION)
                    .put(iv)
                    .put(ciphertextWithTag)
                    .array();
        } catch (Exception e) {
            throw EncryptionException.failed("رمزنگاری شکست خورد", e);
        }
    }

    /**
     * رمزگشایی آرایه بایت.
     *
     * @param payload بایت‌های قالب‌بندی‌شده
     * @return داده خام
     * @throws EncryptionException اگر کوتاه‌تر از حداقل باشد، نسخه ناشناخته
     *                             باشد، یا برچسب احراز اصالت نخواند
     */
    public byte[] decryptBytes(byte[] payload) {
        if (payload == null) {
            return null;
        }
        if (payload.length < MIN_PAYLOAD_BYTES) {
            throw EncryptionException.decryptionFailed(null);
        }

        byte version = payload[VERSION_OFFSET];
        if (version != FORMAT_VERSION) {
            throw EncryptionException.decryptionFailed(null);
        }

        try {
            byte[] iv = Arrays.copyOfRange(payload, IV_OFFSET, CIPHERTEXT_OFFSET);
            byte[] ciphertextWithTag = Arrays.copyOfRange(payload, CIPHERTEXT_OFFSET, payload.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(ciphertextWithTag);
        } catch (Exception e) {
            // AEADBadTagException یعنی داده دست‌کاری شده یا کلید عوض شده.
            // پیام عمومی می‌دهیم تا به اوراکل تبدیل نشود.
            throw EncryptionException.decryptionFailed(e);
        }
    }

    // ─────────────────────────── نمایش امن ───────────────────────────

    /**
     * پوشاندن یک راز برای نمایش در رابط کاربری.
     *
     * <p>کاربر باید بتواند چند کاراکتر آخر را ببیند تا مطمئن شود کدام
     * کلید ثبت شده — بدون اینکه مقدار کامل در پاسخ API یا حافظه مرورگر
     * قرار بگیرد.
     *
     * @param secret مقدار خام؛ {@code null} یا خالی مجاز است
     * @return رشته‌ای مثل {@code ••••••••AB12}
     */
    public static String mask(String secret) {
        if (secret == null || secret.isEmpty()) {
            return "";
        }
        if (secret.length() <= MASK_VISIBLE_CHARS) {
            return "•".repeat(MASK_DOTS);
        }
        return "•".repeat(MASK_DOTS) + secret.substring(secret.length() - MASK_VISIBLE_CHARS);
    }
}
