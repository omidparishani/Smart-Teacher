package ir.omid.smartteacher.student.internal.domain;

/**
 * اعتبارسنجی کد ملی ایران.
 *
 * <p><b>⚠️ چرا این کلاس وجود دارد؟</b> چون کد ملی ایران یک رقم کنترلی
 * دارد و بدون بررسی‌اش، یک رقم اشتباه در ثبت‌نام تا سال‌ها نامرئی
 * می‌ماند — تا روزی که کارنامه یا مدرک لازم شود و کد در سامانه‌های
 * بالادستی پذیرفته نشود. بررسی در لحظه ورود، جایی است که هنوز می‌شود
 * درستش کرد.
 *
 * <p><b>الگوریتم:</b> ده رقم. مجموع {@code d[i] × (10-i)} برای نُه رقم
 * نخست، باقی‌مانده به ۱۱. اگر باقی‌مانده کمتر از ۲ باشد، رقم کنترلی
 * خودِ باقی‌مانده است؛ وگرنه {@code 11 - باقی‌مانده}.
 *
 * <p><b>⚠️ دو استثنا که عمداً رد می‌شوند:</b>
 * <ul>
 *   <li>کدهای همه‌رقم‌یکسان مثل {@code 1111111111} — این‌ها رقم کنترلی
 *       معتبری می‌سازند ولی در عمل کد ملی نیستند و در سامانه‌های واقعی
 *       رد می‌شوند.</li>
 *   <li>هر ورودی که ده رقم نباشد.</li>
 * </ul>
 *
 * <p><b>⚠️ این بررسی فقط وقتی اجرا می‌شود که کد داده شده باشد.</b>
 * کد ملی اختیاری است؛ دانش‌آموزی که کد ندارد باید بدون کد ثبت شود، نه
 * اینکه ثبتش رد شود.
 */
public final class NationalCode {

    /** طول ثابت کد ملی ایران. */
    private static final int LENGTH = 10;

    private NationalCode() {
        throw new AssertionError("کلاس ابزار نباید نمونه‌سازی شود");
    }

    /**
     * آیا این کد ملی معتبر است؟
     *
     * @param code کد ملی؛ {@code null} و رشته خالی نامعتبرند
     * @return {@code true} اگر ده رقم و با رقم کنترلی درست باشد
     */
    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }

        // ⚠️ trim نمی‌کنیم. «کد ملی» یک شناسه است، نه یک متن؛ فاصله
        // دادنش یعنی کاربر چیزی فرستاده که ما نفهمیدیم. سرویس پیش از
        // اینجا ورودی را نرمال می‌کند و اگر باز هم فاصله بماند، رد
        // شدنش رفتار درست است.
        if (code.length() != LENGTH) {
            return false;
        }

        int[] digits = new int[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            char c = code.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            digits[i] = c - '0';
        }

        if (allDigitsIdentical(digits)) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < LENGTH - 1; i++) {
            sum += digits[i] * (LENGTH - i);
        }

        int remainder = sum % 11;
        int expectedCheckDigit = remainder < 2 ? remainder : 11 - remainder;

        return expectedCheckDigit == digits[LENGTH - 1];
    }

    private static boolean allDigitsIdentical(int[] digits) {
        for (int digit : digits) {
            if (digit != digits[0]) {
                return false;
            }
        }
        return true;
    }
}
