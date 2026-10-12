package ir.omid.smartteacher.eitaa.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * تقسیم پیام بلند به بخش‌های قابل‌ارسال.
 *
 * <p><b>چرا لازم است؟</b> ایتا برای طول متن سقف دارد و پیام بلندتر را
 * رد می‌کند. ما سقف دقیق را نمی‌دانیم (سند رسمی نگفته)، پس سقف
 * محافظه‌کارانه‌ای می‌گیریم و پیام بلند را <i>تقسیم</i> می‌کنیم. رد شدن
 * یک اعلان تکلیف، بدتر از رسیدنش در دو پیام است.
 *
 * <p>الگوریتم تقسیم، سه چیز را رعایت می‌کند:
 * <ol>
 *   <li><b>مرز معنی‌دار.</b> اول در آخرین خط جدید، بعد در آخرین فاصله،
 *       و فقط اگر هیچ‌کدام نبود در میانه کلمه بریده می‌شود. بریدن وسط
 *       کلمه در متن فارسی، خواندن را سخت می‌کند.</li>
 *   <li><b>جفت جانشین (surrogate pair).</b> اگر نقطه برش وسط یک کاراکتر
 *       دوبایتی (ایموجی) بیفتد، یک واحد جابه‌جا می‌شود. بریدن وسطش
 *       یک کاراکتر نامعتبر می‌سازد که در نمایش، مربع خالی می‌شود.</li>
 *   <li><b>پیشرفت تضمین‌شده.</b> هر بخش حداقل یک کاراکتر دارد، پس
 *       حلقه هرگز بی‌نهایت نمی‌شود — حتی با {@code maxLength} کوچک.</li>
 * </ol>
 *
 * <p>این کلاس هیچ حالتی نگه نمی‌دارد و همه متدهایش ایستا هستند: تقسیم
 * یک تابع خالص از ورودی است و وابستگی به وضعیت فقط تست‌پذیری را بدتر
 * می‌کرد.
 */
public final class EitaaTextSplitter {

    /** جایگزین‌نشدنی — فقط برای جلوگیری از ساخت نمونه. */
    private EitaaTextSplitter() {
        throw new AssertionError("کلاس ابزاری نباید نمونه‌سازی شود");
    }

    /**
     * تقسیم متن به بخش‌هایی که هرکدام حداکثر {@code maxLength} کاراکترند.
     *
     * @param text      متن ورودی؛ {@code null} یا خالی ⇒ فهرست خالی
     * @param maxLength بیشترین طول هر بخش؛ اگر نامعتبر باشد یک بخش
     *                  برمی‌گردد
     * @return بخش‌ها به ترتیب — هرگز فهرست خالی برای ورودی غیرخالی
     */
    public static List<String> split(String text, int maxLength) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        if (maxLength <= 0 || text.length() <= maxLength) {
            return List.of(text);
        }

        List<String> parts = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());

            if (end < text.length()) {
                end = findBreakPoint(text, start, end);
            }

            String part = text.substring(start, end);
            if (!part.isEmpty()) {
                parts.add(part);
            }
            start = end;
        }

        return parts;
    }

    /**
     * یافتن نقطه برش در بازه {@code [start, hardEnd)}.
     *
     * <p>ترتیب اولویت: خط جدید، سپس فاصله، سپس برش اجباری. در همه
     * حالت‌ها، اگر نقطه برش وسط یک جفت جانشین بیفتد، یک واحد عقب
     * می‌رود.
     */
    private static int findBreakPoint(String text, int start, int hardEnd) {
        int minimum = start + 1;   // پیشرفت تضمین‌شده

        int newline = text.lastIndexOf('\n', hardEnd - 1);
        if (newline >= minimum) {
            // خط جدید در بخش بعد می‌ماند، نه در بخش فعلی — وگرنه
            // بخش بعدی با یک خط خالی شروع می‌شود.
            return safeAdjust(text, newline, minimum);
        }

        int space = text.lastIndexOf(' ', hardEnd - 1);
        if (space >= minimum) {
            return safeAdjust(text, space, minimum);
        }

        return safeAdjust(text, hardEnd, minimum);
    }

    /**
     * جابه‌جایی نقطه برش اگر وسط یک جفت جانشین بیفتد.
     *
     * <p>یک کاراکتر دوبایتی دو واحد {@code char} می‌گیرد. اگر بعد از
     * نقطه برش، کاراکتر «ادامه جفت» بیاید، یعنی از وسط بریده‌ایم.
     * یک واحد عقب می‌رویم — مگر که به حداقل برسیم، که در آن صورت یک
     * واحد جلو می‌رویم تا جفت کامل شود.
     */
    private static int safeAdjust(String text, int cut, int minimum) {
        if (cut <= 0 || cut >= text.length()) {
            return cut;
        }
        if (!Character.isLowSurrogate(text.charAt(cut))) {
            return cut;
        }

        int adjusted = cut - 1;
        if (adjusted >= minimum) {
            return adjusted;
        }

        // نمی‌توان عقب رفت — پس جلو می‌رویم تا جفت کامل شود.
        int forward = cut + 1;
        return Math.min(forward, text.length());
    }
}
