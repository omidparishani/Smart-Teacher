package ir.omid.smartteacher.eitaa;

import java.time.Instant;

/**
 * نتیجه بررسی سلامت اتصال به ایتا — خروجی
 * {@link EitaaAdapter#checkHealth}.
 *
 * <p>بررسی سلامت با {@code getMe} انجام می‌شود: تنها متدی که هیچ
 * پارامتری جز توکن نمی‌خواهد و هیچ پیامی نمی‌فرستد. پس ارزان است و
 * عوارض جانبی ندارد.
 *
 * @param healthy   آیا ایتا پاسخ {@code ok: true} داد؟
 * @param detail     توضیح کوتاه وضعیت — برای داشبورد و Log
 * @param checkedAt  زمان بررسی
 */
public record EitaaHealth(boolean healthy, String detail, Instant checkedAt) {

    public static EitaaHealth up(String botDisplayName, Instant checkedAt) {
        return new EitaaHealth(true, "ربات «" + botDisplayName + "» پاسخ داد", checkedAt);
    }

    public static EitaaHealth down(String detail, Instant checkedAt) {
        return new EitaaHealth(false, detail, checkedAt);
    }

    /**
     * ⚠️ عمداً بازنویسی شده: {@code detail} ممکن است متن خطای ایتا را
     * حمل کند و آن متن نباید بدون بازبینی در Log پخش شود.
     */
    @Override
    public String toString() {
        return "EitaaHealth{healthy=" + healthy + ", checkedAt=" + checkedAt + '}';
    }
}
