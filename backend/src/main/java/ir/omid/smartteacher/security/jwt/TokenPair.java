package ir.omid.smartteacher.security.jwt;

/**
 * جفت توکن صادرشده در پاسخ ورود یا تازه‌سازی.
 *
 * @param accessToken          توکن دسترسی — کوتاه‌عمر، در هر درخواست فرستاده می‌شود
 * @param refreshToken         توکن تازه‌سازی — بلندعمر، فقط برای چرخش
 * @param tokenType            معمولاً {@code Bearer}
 * @param expiresIn            عمر Access Token به ثانیه — کلاینت با آن
 *                             زمان تازه‌سازی بعدی را می‌داند
 * @param refreshExpiresIn     عمر Refresh Token به ثانیه
 */
public record TokenPair(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn
) {

    public static final String BEARER = "Bearer";

    public static TokenPair of(String accessToken,
                               String refreshToken,
                               long expiresIn,
                               long refreshExpiresIn) {
        return new TokenPair(accessToken, refreshToken, BEARER, expiresIn, refreshExpiresIn);
    }

    /**
     * ⚠️ عمداً بازنویسی شده: توکن‌ها هرگز نباید در Log بیفتند. یک
     * {@code log.info("{}", tokenPair)} ساده، در صورت نبود این متد،
     * هر دو توکن را برای همیشه در فایل Log ثبت می‌کرد.
     */
    @Override
    public String toString() {
        return "TokenPair{tokenType=" + tokenType + ", expiresIn=" + expiresIn
                + ", refreshExpiresIn=" + refreshExpiresIn + ", tokens=***}";
    }
}
