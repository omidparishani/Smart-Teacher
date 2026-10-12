/**
 * محل نگه‌داری توکن‌ها — {@code docs/mini-app.md} بخش ۵.
 *
 * <p><b>⚠️ چرا دو محل مختلف؟</b>
 * <table>
 *   <tr><td>Access (۱۵ دقیقه)</td><td>حافظه (Zustand، بدون persist)</td>
 *       <td>کوتاه‌عمر است و در RAM می‌ماند؛ در دسترس XSS پایدار نیست</td></tr>
 *   <tr><td>Refresh (۳۰ روز)</td><td>{@code localStorage} (این فایل)</td>
 *       <td>باید از بسته‌شدن WebView جان سالم ببرد</td></tr>
 * </table>
 *
 * <p>🔴 <b>تغییر تصمیم — و دلیلش.</b> نسخه پیشین توکن تازه‌سازی را در
 * {@code sessionStorage} می‌گذاشت، با این استدلال که «با بستن اپ پاک
 * می‌شود». آن استدلال یک باگ پروداکشنی می‌ساخت:
 *
 * <pre>
 * پس‌زمینه‌کردن WebView ایتا
 *   → sessionStorage پاک می‌شود
 *     → هر بازگشت ناچار است با initData وارد شود
 *       → initData یک‌بارمصرف و ۵دقیقه‌معتبر است
 *         → سرور رد می‌کند؛ دکمه «تلاش مجدد» هم همان رشته کهنه را
 *           دوباره می‌فرستد → حلقه بی‌پایان «دوباره تلاش کنین»
 * </pre>
 *
 * <p>پس ماندگاری نشست، خودِ راه‌حل است. ریسک XSSِ باقی‌مانده آگاهانه و
 * مهارشده است:
 * <ul>
 *   <li>توکن تازه‌سازی <b>چرخشی و یک‌بارمصرف</b> است
 *       ({@code AuthTokenService.consume}) — هر استفاده، توکن تازه می‌دهد.</li>
 *   <li><b>تشخیص استفاده مجدد خانوادگی</b>: اگر توکن دزدیده‌شده یک بار
 *       مصرف شود، بک‌اند <b>کل خانواده</b> را باطل می‌کند و کاربر واقعی
 *       بیرون می‌افتد — یعنی سرقت <i>دیده</i> می‌شود، نه اینکه بی‌صدا
 *       بماند.</li>
 *   <li>توکن دسترسی ۱۵ دقیقه‌ای در RAM می‌ماند، نه اینجا.</li>
 *   <li>خروج و «از دست رفتن نشست» هر دو این کلید را پاک می‌کنند.</li>
 * </ul>
 *
 * <p>⚠️ <b>و چرا همه‌جا {@code try/catch}؟</b> چون دسترسی به حافظه در سه
 * حالت واقعاً <i>پرتاب</i> می‌کند: حالت خصوصی مرورگر، WebView با
 * ذخیره‌سازی غیرفعال، و سهمیه پر. اگر اینجا استثنا بیرون بزند، کل ورود
 * می‌شکند — در حالی که <b>نبودِ حافظه نباید مانع ورود شود</b>. پس نبود
 * {@code localStorage} به {@code sessionStorage} برمی‌گردد و اگر آن هم
 * نبود، فقط ماندگاری از دست می‌رود (هر باز کردن اپ یک ورود تازه) — ولی
 * برنامه کار می‌کند.
 */

/** کلید جاری — نسخه‌دار، تا تغییر شکل در آینده نشکند. */
const REFRESH_TOKEN_KEY = 'smart-teacher.session.v1';

/**
 * کلید نسخه پیشین — فقط در {@code sessionStorage}.
 *
 * <p>⚠️ نگه داشته شده فقط برای <b>مهاجرت یک‌باره</b>: کاربری که همین حالا
 * اپ بازش دارد، نباید با این استقرار بیرون بیفتد.
 */
const LEGACY_REFRESH_TOKEN_KEY = 'smart-teacher.refresh-token';

function storageOf(kind: 'local' | 'session'): Storage | null {
  try {
    // ⚠️ خودِ دسترسی به `window.localStorage` می‌تواند پرتاب کند.
    return kind === 'local' ? window.localStorage : window.sessionStorage;
  } catch {
    return null;
  }
}

/** خواندن امن — هر شکستی `null` می‌شود، نه استثنا. */
function readKey(storage: Storage | null, key: string): string | null {
  try {
    return storage?.getItem(key) ?? null;
  } catch {
    return null;
  }
}

/** نوشتن امن — `true` فقط اگر واقعاً نوشته شد. */
function writeKey(storage: Storage | null, key: string, value: string): boolean {
  if (!storage) {
    return false;
  }

  try {
    storage.setItem(key, value);
    return true;
  } catch {
    return false;
  }
}

/** پاک‌کردن امن. */
function removeKey(storage: Storage | null, key: string): void {
  try {
    storage?.removeItem(key);
  } catch {
    // ⚠️ عمداً: اگر پاک نشد، توکن در حافظه می‌ماند ولی نشست در RAM
    // پاک شده — و در بدترین حالت کاربر دوباره وارد می‌شود.
  }
}

/**
 * توکن تازه‌سازی — یا `null` اگر نباشد یا حافظه در دسترس نباشد.
 *
 * <p>⚠️ این تابع یک <b>عوارض جانبی عمدی</b> دارد: توکن نسخه قدیمی را
 * یک‌بار به کلید تازه منتقل می‌کند. اگر انتقال موفق شود، کلید قدیمی پاک
 * می‌شود؛ اگر نشود، <i>سر جایش می‌ماند</i> تا نسخه بعدی هم پیدایش کند.
 */
export function readRefreshToken(): string | null {
  const local = storageOf('local');
  const session = storageOf('session');

  // ⚠️ `sessionStorage` با کلید تازه یعنی نشستی که در حالت تخریب‌شده
  // (بدون localStorage) نوشته شده — پس خواندنش لازم است.
  const current = readKey(local, REFRESH_TOKEN_KEY) ?? readKey(session, REFRESH_TOKEN_KEY);

  if (current) {
    return current;
  }

  const legacy = readKey(session, LEGACY_REFRESH_TOKEN_KEY);

  if (!legacy) {
    return null;
  }

  if (writeKey(local, REFRESH_TOKEN_KEY, legacy)) {
    removeKey(session, LEGACY_REFRESH_TOKEN_KEY);
  }

  // ⚠️ چه منتقل شده باشد چه نه، همین توکن معتبر است و باید استفاده شود.
  return legacy;
}

/** ذخیره توکن تازه‌سازی. شکست ذخیره‌سازی، بی‌صدا پذیرفته می‌شود. */
export function writeRefreshToken(token: string): void {
  const local = storageOf('local');
  const session = storageOf('session');

  if (writeKey(local, REFRESH_TOKEN_KEY, token)) {
    // ⚠️ نسخه `sessionStorage` هم باید برود. اگر بماند و بعداً نوشتن در
    // `localStorage` شکست بخورد، خواندن به یک توکن کهنه می‌افتد.
    removeKey(session, REFRESH_TOKEN_KEY);
  } else {
    // ⚠️ حالت تخریب‌شده: `localStorage` نیست یا نوشتن در آن شکست خورد.
    // `sessionStorage` ماندگاری کمتری دارد ولی از «هیچ» بهتر است — کاربر
    // تا بسته‌شدن اپ وارد می‌ماند.
    //
    // 🔴 و این `removeKey` حیاتی است، نه تمیزکاری: اگر نوشتن در
    // `localStorage` پس از یک نوشتنِ <i>موفق</i> قبلی شکست بخورد (سهمیه
    // پر)، نسخه کهنه همان‌جا می‌ماند. `readRefreshToken` اول
    // `localStorage` را می‌خواند، پس توکن <b>مصرف‌شده</b> را برمی‌داشت،
    // سرور آن را «استفاده مجدد» می‌دید و **کل خانواده را باطل می‌کرد** —
    // یعنی خودمان نشست کاربر را می‌کشتیم. در مرورگر واقعی `removeItem`
    // فضا آزاد می‌کند و شکست نمی‌خورد، پس این خط واقعاً کار می‌کند.
    removeKey(local, REFRESH_TOKEN_KEY);
    writeKey(session, REFRESH_TOKEN_KEY, token);
  }

  // 🔴 کلید قدیمی باید برود: ماندن دو نسخه از یک توکن یعنی خواندن
  // تصادفی نسخه کهنه، که سرور آن را «استفاده مجدد» می‌بیند و
  // **کل خانواده را باطل می‌کند** — یعنی خودمان نشست کاربر را می‌کشیم.
  removeKey(session, LEGACY_REFRESH_TOKEN_KEY);
}

/** پاک‌کردن توکن تازه‌سازی — هنگام خروج یا مرگ نشست. */
export function clearRefreshToken(): void {
  for (const kind of ['local', 'session'] as const) {
    const storage = storageOf(kind);

    removeKey(storage, REFRESH_TOKEN_KEY);
    removeKey(storage, LEGACY_REFRESH_TOKEN_KEY);
  }
}
