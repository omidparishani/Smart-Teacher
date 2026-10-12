/// <reference types="vite/client" />

/**
 * متغیرهای محیطی این پروژه.
 *
 * <p>🔴 <b>چرا این فایل لازم است؟</b> چون {@code vite/client} یک امضای
 * نمایه‌ای {@code [key: string]: any} برای {@code ImportMetaEnv} تعریف
 * می‌کند. بدون اعلان صریح، هر {@code import.meta.env.VITE_...} از نوع
 * {@code any} است — و یک {@code any} در مسیری که تصمیم ورود می‌گیرد
 * (همان {@code VITE_DEV_INIT_DATA}) دقیقاً همان جایی است که یک اشتباه
 * تایپی بی‌صدا رد می‌شود و به‌جای خطا، {@code undefined} می‌گیریم.
 *
 * <p>با اعلان زیر، هر دو متغیر {@code string | undefined} می‌شوند و
 * {@code no-unsafe-assignment} هم دیگر شاکی نیست.
 */
interface ImportMetaEnv {
  /** نشانی پایه API — بدون اسلش انتهایی. */
  readonly VITE_API_URL?: string;
  /**
   * `initData` جعلی — <b>فقط توسعه</b>.
   *
   * <p>⚠️ در بیلد Production هرگز خوانده نمی‌شود
   * ({@code src/eitaa/init-data.ts}).
   */
  readonly VITE_DEV_INIT_DATA?: string;
  /**
   * اجازه دادن به باز شدن برنامک در <b>مرورگر</b> — فقط توسعه.
   *
   * <p>🔴 پیش‌فرض خاموش است. بدون آن، گارد پلتفرم بیرون از ایتا صفحه
   * «این برنامه باید از داخل ایتا باز شود» را نشان می‌دهد و ورود
   * ممکن نیست — این رفتار محصول است، نه خطا.
   *
   * <p>⚠️ رایج‌ترین دلیل روشن کردنش: روی شبکه‌ای که
   * {@code developer.eitaa.com/eitaa-web-app.js} در دسترس نیست،
   * {@code window.Eitaa} ساخته نمی‌شود و کار روی Mini App در مرورگر
   * ممکن نیست. جزئیات: {@code docs/mini-app.md} بخش ۴.۵.
   */
  readonly VITE_DEV_ALLOW_BROWSER?: string;
}
