import { eitaa } from '../eitaa/sdk';

/** نتیجه بررسی پلتفرم. */
export type PlatformVerdict =
  | { ok: true; platform: string }
  | { ok: false; platform: string; reason: 'not-in-eitaa' | 'ios'; message: string };

/**
 * پیام‌های فارسی — از {@code docs/mini-app.md} بخش ۴.۵.
 *
 * <p>⚠️ یک‌جا نگه داشته می‌شوند تا صفحه فرود، بنر و صفحه خطا یک متن را
 * نشان دهند.
 */
export const PLATFORM_MESSAGES = {
  notInEitaa: 'این برنامه باید از داخل ایتا باز شود.',
  ios:
    'این برنامه در نسخه iOS ایتا پشتیبانی نمی‌شود. ' +
    'لطفاً از نسخه اندروید یا ویندوز استفاده کنید.',
} as const;

/**
 * آیا این محیط برای اجرای برنامک پشتیبانی می‌شود؟
 *
 * <p><b>🔴 این یک واقعیت خارجی است، نه انتخاب ما.</b> مستندات رسمی ایتا
 * ({@code developer.eitaa.com/docs/QuickStart/}) صریح می‌گوید برنامک‌ها فقط
 * روی <b>اندروید</b> و <b>دسکتاپ ویندوز</b> اجرا می‌شوند؛ iOS و مرورگر
 * پشتیبانی نمی‌شوند. پس این محدودیت در UI <i>شفاف اعلام</i> می‌شود — نه
 * اینکه کاربر یک صفحه سفید ببیند و ما سکوت کنیم.
 *
 * <p><b>⚠️ تصمیم مهم: پلتفرم ناشناخته مانع نمی‌شود.</b>
 * اگر ایتا روزی پلتفرم جدیدی اضافه کند (یا مقدار خالی بدهد)، ما آن را
 * رد نمی‌کنیم. دلیلش نامتقارن بودن هزینه است: رد کردن یک کاربر واقعی
 * هزینه‌اش بیشتر از اجازه دادن به یک کاربر iOS است که در هر صورت در
 * لایه‌های بعدی به مشکل می‌خورد.
 *
 * <p>مقایسه بر پایه <i>زیررشته</i> است و نه برابری کامل، چون مقدار دقیق
 * {@code platform} در مستندات به‌صورت فهرست بسته نیامده.
 *
 * <p><b>⚠️ یک استثنا: دروازه توسعه.</b> اگر
 * {@code VITE_DEV_ALLOW_BROWSER=true} تنظیم شده باشد <i>و</i> بیلد
 * Production نباشد، نبود SDK مانع نمی‌شود. این عمداً یک Opt-in صریح است و
 * نه رفتار پیش‌فرض — چون آزمون E2E «بیرون از ایتا» همان حالت را می‌سنجد.
 */
export function checkPlatform(): PlatformVerdict {
  const app = eitaa.app;

  if (!app) {
    // 🔴 دروازه توسعه — پیش‌فرض خاموش.
    //
    // ⚠️ چرا لازم است: روی شبکه‌ای که `developer.eitaa.com` در دسترس
    // نیست، اسکریپت SDK بارگذاری نمی‌شود و `window.Eitaa` ساخته نمی‌شود.
    // نتیجه، بدون این دروازه: کار روی Mini App در مرورگر غیرممکن است —
    // `VITE_DEV_INIT_DATA` هم نجات نمی‌دهد، چون گارد *پیش از* خواندن
    // `initData` اجرا می‌شود.
    //
    // ⚠️ ترتیب شرط عمدی است: اول `PROD`. `import.meta.env.PROD` در زمان
    // بیلد `true` می‌شود، پس bundler کل شاخه توسعه را حذف می‌کند. همان
    // الگویی که `getInitData` در `src/eitaa/init-data.ts` دارد.
    if (isDevBrowserAllowed()) {
      return { ok: true, platform: 'dev-browser' };
    }

    return {
      ok: false,
      platform: 'unknown',
      reason: 'not-in-eitaa',
      message: PLATFORM_MESSAGES.notInEitaa,
    };
  }

  const platform = eitaa.platform();

  if (platform.toLowerCase().includes('ios')) {
    return {
      ok: false,
      platform,
      reason: 'ios',
      message: PLATFORM_MESSAGES.ios,
    };
  }

  return { ok: true, platform };
}

/**
 * آیا عبور از گارد پلتفرم در مرورگر مجاز شده است؟
 *
 * <p>🔴 <b>در بیلد Production هرگز {@code true} نمی‌شود.</b> شرط اول در
 * زمان بیلد ثابت می‌شود، پس شاخه دوم در باندل نهایی وجود ندارد.
 */
function isDevBrowserAllowed(): boolean {
  if (import.meta.env.PROD) {
    return false;
  }
  return import.meta.env.VITE_DEV_ALLOW_BROWSER === 'true';
}
