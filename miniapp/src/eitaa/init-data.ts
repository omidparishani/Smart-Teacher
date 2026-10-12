import { eitaa } from './sdk';

/** دلیل نبود `initData` — تا UI پیام درست را نشان دهد. */
export type InitDataUnavailableReason =
  /** از داخل ایتا باز نشده (مرورگر یا WebView بدون SDK). */
  | 'not-in-eitaa'
  /** در حال توسعه‌ایم ولی `VITE_DEV_INIT_DATA` تنظیم نشده. */
  | 'dev-data-missing';

/** خطای قابل‌تشخیص نبود `initData`. */
export class InitDataUnavailableError extends Error {
  readonly reason: InitDataUnavailableReason;

  constructor(reason: InitDataUnavailableReason, message: string) {
    super(message);
    this.name = 'InitDataUnavailableError';
    this.reason = reason;
  }
}

/**
 * پیام فارسی برای هر دلیل — یک‌جا و نه پراکنده در کامپوننت‌ها.
 *
 * <p>⚠️ اینجا عمدی است: اگر هر صفحه پیام خودش را بسازد، دیر یا زود یکی
 * پیام فنی خام نشان می‌دهد یا یکی متن را کوتاه‌تر می‌کند و تفاوت iOS با
 * مرورگر گم می‌شود.
 */
export const INIT_DATA_MESSAGES: Record<InitDataUnavailableReason, string> = {
  'not-in-eitaa': 'این برنامه باید از داخل ایتا باز شود.',
  'dev-data-missing':
    'حالت توسعه فعال است ولی VITE_DEV_INIT_DATA تنظیم نشده. ' +
    'فایل .env.local را از .env.example بسازید.',
};

let devDataInUse = false;

/**
 * آیا این نشست با `initData` جعلی توسعه اجرا می‌شود؟
 *
 * <p>⚠️ برای نشان دادن بنر هشدار در UI. بدون آن، ممکن است کسی یک نشست
 * توسعه را با یک نشست واقعی اشتباه بگیرد — و بعد دنبال باگی بگردد که
 * وجود ندارد.
 */
export function isDevInitDataInUse(): boolean {
  return devDataInUse;
}

/**
 * استخراج `initData` برای فرستادن به بک‌اند.
 *
 * <p><b>🔴 قاعده امنیتی این تابع:</b> در بیلد Production، <b>هرگز</b> از
 * `VITE_DEV_INIT_DATA` استفاده نمی‌شود — حتی اگر آن متغیر در محیط تنظیم
 * شده باشد. اگر این دروازه روزی باز شود، هر کسی می‌تواند با یک
 * `initData` جعلی وارد شود.
 *
 * <p>پس ترتیب بررسی عمدی است: اول Production رد می‌شود، بعد توسعه بررسی
 * می‌شود. `import.meta.env.PROD` در زمان بیلد ثابت می‌شود، پس این شرط در
 * باندل نهایی به `true` تبدیل می‌شود و bundler کل شاخه‌ی توسعه را حذف
 * می‌کند.
 *
 * <p>🔴 <b>این ادعا اندازه‌گیری شده است، نه استنباط.</b> با برداشتن
 * پلاگین پیکربندی و حتی با `--minify false`، نه مقدار و نه خودِ شاخه در
 * باندل نبود. اگر کسی ترتیب این دو شرط را عوض کند، دروازه باز می‌شود و
 * `scripts/verify-bundle.mjs` در بیلد شکست می‌خورد.
 *
 * @throws InitDataUnavailableError اگر داده‌ای برای ورود نباشد
 */
export function getInitData(): string {
  devDataInUse = false;

  const fromEitaa = eitaa.initData();

  if (fromEitaa) {
    return fromEitaa;
  }

  // 🔴 اول Production — تا کد توسعه هرگز در باندل نهایی اجرا نشود.
  if (import.meta.env.PROD) {
    throw new InitDataUnavailableError(
      'not-in-eitaa',
      INIT_DATA_MESSAGES['not-in-eitaa'],
    );
  }

  const devData = import.meta.env.VITE_DEV_INIT_DATA;

  if (devData) {
    devDataInUse = true;
    return devData;
  }

  throw new InitDataUnavailableError(
    'dev-data-missing',
    INIT_DATA_MESSAGES['dev-data-missing'],
  );
}
