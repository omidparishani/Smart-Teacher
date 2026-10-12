package ir.omid.smartteacher.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * توابع درهم‌سازی و مقایسه امن.
 *
 * <p>ابزارهای این کلاس دو کاربرد امنیتی دارند:
 * <ol>
 *   <li><b>ذخیره توکن:</b> توکن Refresh هرگز خام ذخیره نمی‌شود؛ فقط
 *       {@link #sha256Hex(String)} آن در دیتابیس می‌ماند. اگر دیتابیس لو
 *       برود، مهاجم توکن قابل استفاده‌ای به دست نمی‌آورد.</li>
 *   <li><b>مقایسه امض‌ها:</b> مقایسه با {@code equals} معمولی در اولین
 *       بایت متفاوت برمی‌گردد و زمان اجرا وابسته به ورودی می‌شود
 *       (Timing Attack). {@link #constantTimeEquals} همیشه تمام طول را
 *       مقایسه می‌کند.</li>
 * </ol>
 *
 * <p><b>⚠️ توجه:</b> این کلاس برای هش <i>توکن</i> است، نه رمز عبور.
 * رمز عبور باید با الگوریتم کند (bcrypt/Argon2) هش شود، چون SHA-256
 * برای Brute-force روی رمزهای کوتاه بیش از حد سریع است.
 */
public final class Hashing {

    private static final String ALGORITHM = "SHA-256";

    /** طول هش SHA-256 در قالب hex. */
    public static final int SHA256_HEX_LENGTH = 64;

    private static final HexFormat HEX = HexFormat.of();

    private Hashing() {
        throw new AssertionError("کلاس ابزاری نباید نمونه‌سازی شود");
    }

    /**
     * هش SHA-256 یک رشته UTF-8 در قالب hex با حروف کوچک.
     *
     * @param value ورودی (نباید {@code null} باشد)
     * @return ۶۴ کاراکتر hex
     */
    public static String sha256Hex(String value) {
        if (value == null) {
            throw new IllegalArgumentException("مقدار ورودی برای هش نمی‌تواند null باشد");
        }
        return sha256Hex(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * هش SHA-256 یک آرایه بایت در قالب hex با حروف کوچک.
     *
     * @param value ورودی (نباید {@code null} باشد)
     * @return ۶۴ کاراکتر hex
     */
    public static String sha256Hex(byte[] value) {
        if (value == null) {
            throw new IllegalArgumentException("مقدار ورودی برای هش نمی‌تواند null باشد");
        }
        return HEX.formatHex(sha256(value));
    }

    /**
     * هش SHA-256 خام.
     *
     * @param value ورودی (نباید {@code null} باشد)
     * @return ۳۲ بایت هش
     */
    public static byte[] sha256(byte[] value) {
        if (value == null) {
            throw new IllegalArgumentException("مقدار ورودی برای هش نمی‌تواند null باشد");
        }
        try {
            return MessageDigest.getInstance(ALGORITHM).digest(value);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 در هر JVM سازگار با Java SE الزامی است؛ رسیدن به این
            // خط یعنی محیط اجرا خراب است، نه اینکه ورودی بد بوده.
            throw new IllegalStateException("الگوریتم " + ALGORITHM + " در این JVM موجود نیست", e);
        }
    }

    /**
     * مقایسه دو رشته در زمان ثابت.
     *
     * <p>اگر یکی از دو مقدار {@code null} باشد، نتیجه فقط وقتی {@code true}
     * است که هر دو {@code null} باشند.
     *
     * @param left  مقدار اول
     * @param right مقدار دوم
     * @return {@code true} اگر برابر باشند
     */
    public static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return left == right;
        }
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * مقایسه دو آرایه بایت در زمان ثابت.
     *
     * @param left  مقدار اول
     * @param right مقدار دوم
     * @return {@code true} اگر برابر باشند
     */
    public static boolean constantTimeEquals(byte[] left, byte[] right) {
        if (left == null || right == null) {
            return left == right;
        }
        return MessageDigest.isEqual(left, right);
    }
}
