import '@testing-library/jest-dom/vitest';
import { afterEach, beforeEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';

/**
 * راه‌اندازی مشترک آزمون‌ها.
 *
 * <p>⚠️ <b>چرا پاک‌سازی صریح؟</b> چون همه آزمون‌های این پروژه روی وضعیت
 * جهانی کار می‌کنند: {@code window.Eitaa} جعل می‌شود، حافظه‌های مرورگر
 * نوشته می‌شوند و پل احراز هویت وصل می‌شود. بدون پاک‌سازی، یک آزمون
 * می‌تواند نتیجه آزمون بعدی را عوض کند و شکست، جای اشتباهی گزارش شود.
 */
beforeEach(() => {
  window.sessionStorage.clear();
  // ⚠️ توکن تازه‌سازی در `localStorage` می‌نشیند (`lib/token-storage.ts`).
  // بدون این خط، یک آزمون که کاربرِ واردشده می‌سازد، آزمون بعدی را هم
  // وارد نشان می‌دهد — و بدترین نوع شکست، شکستی است که جای دیگری گزارش شود.
  window.localStorage.clear();

  /*
   * 🔴 خنثی‌سازی `VITE_DEV_INIT_DATA`.
   *
   * ⚠️ این یک باگ واقعی بود که در همین اجرا کشف شد: یک `.env.local` روی
   * دستگاه توسعه، مقدار جعلی را به آزمون‌ها هم می‌رساند (Vite فایل‌های
   * محیطی را در حالت `test` هم می‌خواند). نتیجه‌اش این شد که آزمون
   * «بیرون از ایتا باید مسدود شود» سبز نمی‌ماند، چون برنامه در واقع با
   * داده جعلی وارد می‌شد.
   *
   * ⚠️ درس مهم‌تر: آزمونی که نتیجه‌اش به پیکربندی شخصی دستگاه وابسته
   * باشد، <b>آزمون نیست</b>. با خنثی‌سازی پیش‌فرض اینجا، هر آزمونی که
   * داده جعلی می‌خواهد باید صریحاً {@code vi.stubEnv} بزند.
   */
  vi.stubEnv('VITE_DEV_INIT_DATA', '');

  /*
   * ⚠️ و همان درس برای دروازه توسعه (`VITE_DEV_ALLOW_BROWSER`): اگر روی
   * دستگاه توسعه روشن باشد، آزمون «بیرون از ایتا باید مسدود شود» سبز
   * نمی‌ماند. پیش‌فرض خاموش، و هر آزمونی که آن را می‌خواهد صریحاً
   * `vi.stubEnv` می‌زند.
   */
  vi.stubEnv('VITE_DEV_ALLOW_BROWSER', '');
});

afterEach(() => {
  cleanup();
  // ⚠️ SDK جعلی آزمون قبلی نباید به آزمون بعدی برسد — وگرنه آزمونی که
  // انتظار دارد «بیرون از ایتا» باشد، خودش را داخل ایتا می‌بیند.
  delete window.Eitaa;
});

/**
 * {@code matchMedia} در jsdom وجود ندارد.
 *
 * <p>⚠️ بدون این جعل، {@code detectTheme} به مسیر {@code catch} می‌رود و
 * آزمون‌ها به‌جای بررسی رفتار واقعی، همیشه «روشن» می‌بینند. این جعل
 * محیط واقعی مرورگر را شبیه‌تر می‌کند.
 */
if (typeof window.matchMedia !== 'function') {
  // ⚠️ شیء برگشتی عمداً همه اعضای `MediaQueryList` را دارد تا نیازی به
  // `as unknown as` نباشد — آن ریخته‌گری نوع‌گذاری را دور می‌زد و اگر
  // امضای `matchMedia` عوض می‌شد، کسی متوجه نمی‌شد.
  window.matchMedia = (query: string): MediaQueryList => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => undefined,
    removeListener: () => undefined,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    dispatchEvent: () => false,
  });
}
