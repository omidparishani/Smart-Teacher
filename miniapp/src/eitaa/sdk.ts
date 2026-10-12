import type { EitaaWebAppApi } from './types';

export type { BottomButton, EitaaUnsafeUser, EitaaUnsafeData, EitaaWebAppApi } from './types';

/**
 * پوشش SDK ایتا — <b>تنها نقطه تماس با SDK</b>.
 *
 * <p>هیچ کامپوننتی {@code window.Eitaa} را مستقیم نمی‌خواند. اگر ایتا نامی را
 * عوض کند، فقط همین فایل اصلاح می‌شود.
 *
 * <p><b>🔴 قاعده‌ای که همه متدهای این شیء رعایت می‌کنند: هیچ‌کدام استثنا
 * پرتاب نمی‌کند.</b> دلیلش این است که این کد در سه محیط اجرا می‌شود:
 * <ol>
 *   <li>داخل ایتا اندروید — همه‌چیز هست.</li>
 *   <li>داخل ایتا iOS — SDK هست ولی برنامک پشتیبانی نمی‌شود.</li>
 *   <li>مرورگر — {@code window.Eitaa} اصلاً وجود ندارد.</li>
 * </ol>
 * اگر یک getter روی حالت سوم استثنا بدهد، به‌جای یک پیام فارسی روشن، کاربر
 * یک صفحه سفید می‌بیند. پس همه‌جا مقدار پیش‌فرض برمی‌گردانیم و «نبودن» را
 * با {@link eitaa.isAvailable} اعلام می‌کنیم — کسی که باید تصمیم بگیرد،
 * همان کسی است که این وضعیت را می‌بیند.
 *
 * <p>⚠️ حتی خودِ خواندن {@code window.Eitaa} هم داخل {@code try} است: اگر
 * صفحه‌ای با {@code Object.defineProperty} خاصیت را قفل کرده باشد، خواندنش
 * استثنا می‌دهد. بعید است، ولی هزینه‌اش صفر است.
 */
function readApp(): EitaaWebAppApi | undefined {
  try {
    return window.Eitaa?.WebApp;
  } catch {
    return undefined;
  }
}

export const eitaa = {
  /** شیء Global — `undefined` یعنی SDK بارگذاری نشده (مرورگر). */
  get app(): EitaaWebAppApi | undefined {
    return readApp();
  },

  /**
   * آیا اپ داخل ایتا و با داده معتبر باز شده است؟
   *
   * <p>⚠️ معیار، وجود {@code initData} است و نه وجود SDK. SDK ممکن است
   * بارگذاری شده باشد ولی {@code initData} خالی باشد — آن حالت یعنی فرم
   * واقعاً از داخل ایتا باز نشده و ورود ممکن نیست.
   */
  isAvailable(): boolean {
    const app = readApp();
    return Boolean(app && typeof app.initData === 'string' && app.initData.length > 0);
  },

  /** رشته خام `initData` — برای فرستادن به بک‌اند. */
  initData(): string {
    const value = readApp()?.initData;
    return typeof value === 'string' ? value : '';
  },

  /** پارامتر Deep Link — مثلاً `asg_123` برای باز شدن مستقیم یک تکلیف. */
  startParam(): string | undefined {
    const value = readApp()?.initDataUnsafe?.start_param;
    return typeof value === 'string' && value.length > 0 ? value : undefined;
  },

  /** ⚠️ داده تأییدنشده — فقط برای راحتی UI، هرگز برای تصمیم دسترسی. */
  unsafeUser() {
    return readApp()?.initDataUnsafe?.user;
  },

  colorScheme(): 'light' | 'dark' {
    return readApp()?.colorScheme === 'dark' ? 'dark' : 'light';
  },

  platform(): string {
    const value = readApp()?.platform;
    return typeof value === 'string' ? value : 'unknown';
  },

  version(): string {
    const value = readApp()?.version;
    return typeof value === 'string' ? value : '';
  },

  ready(): void {
    call((app) => app.ready?.());
  },

  expand(): void {
    call((app) => app.expand?.());
  },

  /**
   * بستن برنامک — بازگشت به گفت‌وگوی ایتا.
   *
   * <p>⚠️ تنها راه ساختن {@code initData} تازه برای همین کاربر است. بدون
   * بستن و باز کردن دوباره، SDK همان رشته کهنه را برمی‌گرداند و سرور
   * آن را رد می‌کند — پس هر «تلاش مجدد» داخل برنامه محکوم به شکست است.
   *
   * <p>⚠️ اگر SDK نبود (مرورگر)، این متد بی‌اثر است و خطا هم نمی‌دهد.
   */
  close(): void {
    call((app) => app.close?.());
  },

  /** بازخورد لمسی سبک — روی دسکتاپ بی‌اثر است و خطا هم نمی‌دهد. */
  haptic(): void {
    call((app) => app.HapticFeedback?.selectionChanged?.());
  },

  /**
   * آیا نسخه SDK از این قابلیت پشتیبانی می‌کند؟ — Feature Detection
   *
   * <p>⚠️ قاعده: هر متد SDK فقط پس از این بررسی صدا زده می‌شود. اگر نبود،
   * <b>Fallback خودمان</b> اجرا می‌شود، نه اینکه به وجودش تکیه کنیم.
   */
  atLeast(version: string): boolean {
    try {
      return readApp()?.isVersionAtLeast?.(version) ?? false;
    } catch {
      return false;
    }
  },

  /** باز کردن لینک بیرونی — با Fallback مرورگر اگر SDK نبود. */
  openLink(url: string): void {
    try {
      const app = readApp();
      if (app?.openLink) {
        app.openLink(url);
        return;
      }
    } catch {
      // ⚠️ عمداً: اگر SDK خطا داد، به مرورگر برمی‌گردیم.
    }
    window.open(url, '_blank', 'noopener,noreferrer');
  },

  /** هشدار فارسی — اگر SDK نبود، به `alert` برمی‌گردد. */
  showAlert(message: string): void {
    try {
      const app = readApp();
      if (app?.showAlert) {
        app.showAlert(message);
        return;
      }
    } catch {
      // ⚠️ عمداً: به Fallback می‌رویم.
    }
    window.alert(message);
  },
} as const;

/**
 * صدا زدن امن یک متد SDK.
 *
 * <p>⚠️ الگوی تکراری این فایل: حضور متد را چک کن، در {@code try} صدا بزن،
 * و شکست را بی‌صدا بپذیر. متدهای UI (لمس، آماده‌سازی، بسط) هیچ‌وقت آن‌قدر
 * مهم نیستند که یک صفحه سفید بسازند.
 *
 * <p>⚠️ نام پارامتر عمداً {@code invoke} است و نه {@code use}: ESLint
 * هر پارامتری که با {@code use} شروع شود را یک React Hook می‌بیند و این
 * تابع را به‌عنوان «Hook داخل try/catch» علامت می‌زند.
 *
 * @param invoke کاری که با SDK باید انجام شود
 */
function call(invoke: (app: EitaaWebAppApi) => void): void {
  const app = readApp();
  if (!app) {
    return;
  }
  try {
    invoke(app);
  } catch {
    // ⚠️ عمداً خالی: متدهای UI نباید جریان برنامه را بشکنند.
  }
}
