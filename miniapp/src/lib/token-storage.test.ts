import { beforeEach, describe, expect, it } from 'vitest';
import { clearRefreshToken, readRefreshToken, writeRefreshToken } from './token-storage';

/**
 * آزمون محل نگه‌داری توکن.
 *
 * <p>🔴 چهار ادعا اینجا آزموده می‌شود و همه‌شان حیاتی‌اند:
 * <ol>
 *   <li>توکن تازه‌سازی <b>از بسته‌شدن WebView جان سالم به در می‌برد</b> —
 *       این همان چیزی است که حلقه «دوباره تلاش کنین» را می‌بندد.</li>
 *   <li>توکن نسخه قدیمی (<b>sessionStorage</b>) یک‌بار منتقل می‌شود و
 *       کاربر با استقرار بیرون نمی‌افتد.</li>
 *   <li>نبود یا خرابی حافظه، ورود را <b>نمی‌شکند</b>.</li>
 *   <li>خروج، <b>هر دو</b> محل و هر دو کلید را پاک می‌کند.</li>
 * </ol>
 */

/** کلید جاری — باید با `token-storage.ts` یکی بماند. */
const KEY = 'smart-teacher.session.v1';

/** کلید نسخه پیشین — فقط برای آزمون مهاجرت. */
const LEGACY_KEY = 'smart-teacher.refresh-token';

/**
 * اجرای یک بلوک با {@code window.localStorage} جانشین‌شده.
 *
 * <p>🔴 <b>چرا جانشینیِ خاصیت و نه {@code vi.spyOn}؟</b> چون در jsdom،
 * {@code localStorage} یک Proxy است و
 * {@code vi.spyOn(window.localStorage, 'setItem')} قابل‌اعتماد نیست: یا
 * متد را اصلاً جایگزین نمی‌کند — و آن‌وقت آزمون سبز می‌شود بدون آنکه
 * چیزی را بسنجد — یا هر دو حافظه را با هم می‌بندد. هر دو حالت آزمون را
 * بی‌معنا می‌کند. جایگزینی خودِ خاصیت قطعی است و در همین فایل عملاً
 * جواب می‌دهد (آزمون «اگر localStorage نبود…»).
 *
 * @param descriptor توصیف خاصیت جانشین — مثلاً {@code get}ی که پرتاب
 *        می‌کند، یا {@code value}ی که حافظه‌ای خراب را می‌دهد.
 */
function withReplacedLocalStorage(
  descriptor: PropertyDescriptor,
  run: () => void,
): void {
  const original = Object.getOwnPropertyDescriptor(window, 'localStorage');

  Object.defineProperty(window, 'localStorage', {
    configurable: true,
    ...descriptor,
  });

  try {
    run();
  } finally {
    if (original) {
      Object.defineProperty(window, 'localStorage', original);
    } else {
      // ⚠️ خاصیت روی زنجیره prototype است؛ با حذف نسخه سایه‌انداز،
      // نسخه اصلی دوباره دیده می‌شود.
      Reflect.deleteProperty(window, 'localStorage');
    }
  }
}

/**
 * اجرای یک بلوک در حالی که خواندن {@code window.localStorage} پرتاب می‌کند.
 *
 * <p>⚠️ حالت واقعی: WebView با ذخیره‌سازی غیرفعال یا صفحه‌ای که با
 * {@code Object.defineProperty} خاصیت را قفل کرده. اینجا فقط
 * <i>دسترسی</i> می‌شکند — نه {@code getItem}.
 */
function withoutLocalStorage(run: () => void): void {
  withReplacedLocalStorage(
    {
      get() {
        throw new DOMException('SecurityError');
      },
    },
    run,
  );
}

describe('token-storage', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
    window.localStorage.clear();
  });

  it('دور کامل نوشتن و خواندن', () => {
    writeRefreshToken('refresh-abc');

    expect(readRefreshToken()).toBe('refresh-abc');
  });

  it('پاک‌کردن، مقدار را برمی‌دارد', () => {
    writeRefreshToken('refresh-abc');
    clearRefreshToken();

    expect(readRefreshToken()).toBeNull();
  });

  it('در نبود مقدار، null می‌دهد', () => {
    expect(readRefreshToken()).toBeNull();
  });

  it('🔴 توکن در localStorage می‌ماند — تا از بسته‌شدن WebView جان سالم ببرد', () => {
    writeRefreshToken('refresh-abc');

    // ⚠️ این همان ادعای اصلی است. نسخه پیشین اینجا
    // `expect(window.localStorage.length).toBe(0)` داشت — یعنی دقیقاً
    // همان باگی را تضمین می‌کرد که کاربر گزارش داد: با هر پس‌زمینه‌شدن
    // اپ، توکن می‌رفت و ورود تازه با initData کهنه رد می‌شد.
    expect(window.localStorage.getItem(KEY)).toBe('refresh-abc');
  });

  it('🔴 فقط یک کلید می‌نویسد — نه کلید پراکنده', () => {
    writeRefreshToken('refresh-abc');

    // ⚠️ دو کلید (یکی برای توکن، یکی برای چیز دیگری) یعنی خواندن
    // تصادفی نسخه کهنه — و سرور آن را «استفاده مجدد» می‌بیند.
    expect(window.localStorage.length).toBe(1);
    expect(window.sessionStorage.length).toBe(0);
  });

  it('🔴 توکن نسخه قدیمی را از sessionStorage منتقل می‌کند', () => {
    // ⚠️ وضعیت کاربری که دقیقاً همین حالا اپ بازش است و ما در حال
    // استقرار هستیم: توکنش زیر کلید قدیمی نشسته.
    window.sessionStorage.setItem(LEGACY_KEY, 'refresh-legacy');

    expect(readRefreshToken()).toBe('refresh-legacy');

    // 🔴 دو چیز باید هم‌زمان درست باشد: توکن منتقل شده باشد، و کلید
    // قدیمی رفته باشد. ماندن هر دو نسخه، سرور را به «استفاده مجدد»
    // می‌رساند و کل خانواده توکن باطل می‌شود.
    expect(window.localStorage.getItem(KEY)).toBe('refresh-legacy');
    expect(window.sessionStorage.getItem(LEGACY_KEY)).toBeNull();
  });

  it('🔴 اگر localStorage نبود، توکن قدیمی گم نمی‌شود', () => {
    window.sessionStorage.setItem(LEGACY_KEY, 'refresh-legacy');

    withoutLocalStorage(() => {
      // ⚠️ انتقال ممکن نیست، پس توکن باید از جای خودش خوانده شود.
      expect(readRefreshToken()).toBe('refresh-legacy');
    });

    // ⚠️ و باید سر جایش مانده باشد تا نسخه بعدی هم پیدایش کند.
    expect(window.sessionStorage.getItem(LEGACY_KEY)).toBe('refresh-legacy');
  });

  it('🔴 وقتی localStorage پرتاب می‌کند، نوشتن به sessionStorage برمی‌گردد', () => {
    /*
     * ⚠️ نسخه پیشین اینجا `vi.spyOn(window.localStorage, 'setItem')` بود
     * که در jsdom کار نمی‌کند (دلیلش در `withReplacedLocalStorage`).
     * نتیجه‌اش این بود که این آزمون <b>هرگز</b> شاخه بازگشتی را
     * نمی‌سنجید: نوشتن در `localStorage` بی‌صدا موفق می‌شد، شرط رد
     * می‌شد و `sessionStorage` خالی می‌ماند.
     */
    withoutLocalStorage(() => {
      expect(() => writeRefreshToken('refresh-abc')).not.toThrow();
    });

    // ⚠️ ماندگاری کمتر، ولی از «هیچ» بهتر است — و مهم‌تر: ورود نمی‌شکند.
    expect(window.sessionStorage.getItem(KEY)).toBe('refresh-abc');
    expect(readRefreshToken()).toBe('refresh-abc');
  });

  it('🔴 نوشتن ناموفق، نسخه کهنه localStorage را زنده نمی‌گذارد', () => {
    /*
     * 🔴 حالت «سهمیه پر»: کلید از قبل آنجاست (توکن کهنه)، ولی نوشتن
     * شکست می‌خورد. اگر نسخه کهنه پاک نشود، `readRefreshToken` اول
     * `localStorage` را می‌خواند و همان توکن <b>مصرف‌شده</b> را برمی‌دارد؛
     * سرور آن را «استفاده مجدد» می‌بیند و کل خانواده را باطل می‌کند.
     */
    const backing = new Map<string, string>([[KEY, 'refresh-stale']]);

    withReplacedLocalStorage(
      {
        value: {
          // ⚠️ نوشتن همیشه شکست می‌خورد، ولی پاک‌کردن کار می‌کند — دقیقاً
          // رفتار مرورگر واقعی وقتی سهمیه پر است (`removeItem` فضا آزاد
          // می‌کند و شکست نمی‌خورد).
          setItem: () => {
            throw new DOMException('QuotaExceededError');
          },
          removeItem: (key: string) => {
            backing.delete(key);
          },
        },
      },
      () => {
        expect(() => writeRefreshToken('refresh-fresh')).not.toThrow();
      },
    );

    expect(backing.has(KEY)).toBe(false);
    expect(window.sessionStorage.getItem(KEY)).toBe('refresh-fresh');
    expect(readRefreshToken()).toBe('refresh-fresh');
  });

  it('🔴 وقتی خواندن پرتاب می‌کند، null می‌دهد نه استثنا', () => {
    // ⚠️ بدون `try/catch` در `readKey`، اینجا یک استثنا بیرون می‌زد و کل
    // ورود می‌شکست — فقط به‌خاطر اینکه حافظه در دسترس نیست.
    withReplacedLocalStorage(
      {
        value: {
          getItem: () => {
            throw new DOMException('SecurityError');
          },
        },
      },
      () => {
        expect(readRefreshToken()).toBeNull();
      },
    );
  });

  it('🔴 وقتی پاک‌کردن پرتاب می‌کند، استثنا بیرون نمی‌زند', () => {
    withReplacedLocalStorage(
      {
        value: {
          removeItem: () => {
            throw new DOMException('SecurityError');
          },
        },
      },
      () => {
        expect(() => clearRefreshToken()).not.toThrow();
      },
    );
  });

  it('🔴 خروج، هر دو محل و هر دو کلید را پاک می‌کند', () => {
    window.localStorage.setItem(KEY, 'refresh-new');
    window.sessionStorage.setItem(KEY, 'refresh-new');
    window.sessionStorage.setItem(LEGACY_KEY, 'refresh-legacy');

    clearRefreshToken();

    // 🔴 یک خروج نیمه‌کاره بدترین حالت است: کاربر فکر می‌کند خارج شده
    // ولی توکنی جایی زنده مانده. پس همه‌جا باید پاک شود.
    expect(window.localStorage.getItem(KEY)).toBeNull();
    expect(window.sessionStorage.getItem(KEY)).toBeNull();
    expect(window.sessionStorage.getItem(LEGACY_KEY)).toBeNull();
    expect(readRefreshToken()).toBeNull();
  });
});
