import { create } from 'zustand';
import { ApiError, configureAuth } from '../lib/api-client';
import { fetchCurrentUser, loginWithEitaa, logoutSession, refreshSession } from '../lib/auth-api';
import { getInitData, InitDataUnavailableError, isDevInitDataInUse } from '../eitaa/init-data';
import { ensureWriteAccess, type WriteAccessResult } from '../eitaa/write-access';
import { clearRefreshToken, readRefreshToken, writeRefreshToken } from '../lib/token-storage';
import type { AuthTenant, AuthUser } from '../types/api';

/**
 * وضعیت جریان ورود.
 *
 * <p>⚠️ تفکیک «ناموفق» از «مسدود» عمدی است و کل تجربه کاربر به آن بند
 * است: فقط «ناموفق» با تکرار درست می‌شود.
 */
export type AuthStatus =
  /** هنوز شروع نشده. */
  | 'idle'
  /** در حال ورود اولیه. */
  | 'authenticating'
  /** وارد شده. */
  | 'authenticated'
  /** وارد نشده — <b>خطای گذرا</b> (شبکه، ۵xx). تلاش دوباره معنا دارد. */
  | 'unauthenticated'
  /** ورود ممکن نیست — تلاش دوباره بی‌فایده است. */
  | 'blocked';

/**
 * پیامی که وقتی <b>همان</b> {@code initData} رد‌شده دوباره خواسته شود
 * نشان داده می‌شود.
 *
 * <p>⚠️ چرا متن خودمان و نه پیام سرور؟ چون در این حالت اصلاً درخواستی
 * نمی‌فرستیم (پایین را ببینید)، پس پیام سروری هم در کار نیست.
 */
const STALE_SESSION_MESSAGE =
  'این نشست ایتا دیگر معتبر نیست. برنامک را ببندید و دوباره از ایتا باز کنید.';

/**
 * پیام حالت توسعه وقتی {@code VITE_DEV_INIT_DATA} رد شده است.
 *
 * <p>🔴 در مرورگر، «برنامک را دوباره باز کنید» گمراه‌کننده است — چون
 * {@code initData} از فایل env خوانده می‌شود، نه از ایتا.
 *
 * <p>⚠️ سه علت رایج همان کد سرور را می‌گیرند (منقضی / یک‌بارمصرف /
 * امضای نادرست). Vite مقدار env را فقط هنگام استارت می‌خواند؛ بدون
 * ری‌استارت، فایل تازه روی دیسک هم بی‌اثر است.
 */
const STALE_DEV_INIT_DATA_MESSAGE =
  'ورود توسعه ناموفق بود (VITE_DEV_INIT_DATA منقضی، مصرف‌شده، یا ناهماهنگ با توکن بک‌اند). ' +
  '۱) در ریشه پروژه: scripts\\New-DevInitData.ps1 را اجرا کنید. ' +
  '۲) سرور Vite را کامل متوقف و دوباره npm run dev کنید (رفرش صفحه کافی نیست). ' +
  '۳) صفحه را فقط یک‌بار باز کنید — initData یک‌بارمصرف است.';

interface AuthState {
  status: AuthStatus;
  /** ⚠️ فقط در حافظه — عمداً persist نمی‌شود. */
  accessToken: string | null;
  user: AuthUser | null;
  tenant: AuthTenant | null;
  /** پیام فارسی برای نمایش. */
  errorMessage: string | null;
  /** نتیجه درخواست اجازه ارسال پیام. */
  writeAccess: WriteAccessResult | null;

  bootstrap: () => Promise<void>;
  login: () => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<string | null>;
  /**
   * تازه‌سازی هنگام بازگشت اپ از پس‌زمینه.
   *
   * <p>⚠️ بدون این، توکن دسترسی ۱۵ دقیقه‌ای در پس‌زمینه منقضی می‌شود و
   * اولین درخواستِ پس از بازگشت ۴۰۱ می‌گیرد — کاربر یک صفحه خطا
   * می‌بیند، در حالی که توکن تازه‌سازی ۳۰روزه‌اش سالم بوده.
   */
  refreshOnResume: () => Promise<void>;
  onSessionLost: () => void;
}

/**
 * ورود تنها یک‌بار در هر بارگذاری اجرا می‌شود.
 *
 * <p>⚠️ React 19 در حالت توسعه، افکت‌ها را دو بار اجرا می‌کند
 * (StrictMode). بدون این محافظ، دو درخواست ورود هم‌زمان می‌رفت و چون
 * {@code initData} در بک‌اند <b>ضدبازپخش</b> است، دومی با
 * {@code EITAA_INIT_DATA_REPLAYED} رد می‌شد — یعنی در حالت توسعه یک باگ
 * می‌دیدیم که در Production وجود ندارد، و دنبال دلیلش می‌گشتیم.
 */
let bootstrapInFlight: Promise<void> | null = null;

/**
 * تازه‌سازی در جریان — <b>یک</b> درخواست برای همه فراخوان‌ها.
 *
 * <p>🔴 این محافظ حیاتی است، نه بهینه‌سازی. توکن تازه‌سازی
 * <b>یک‌بارمصرف</b> است: اگر دو مسیر هم‌زمان تازه‌سازی کنند — مثلاً
 * {@code refreshOnResume} و یک پرس‌وجوی React Query که در بازگشت به
 * صفحه ۴۰۱ گرفته — سرور توکن دوم را «استفاده مجدد» می‌بیند و
 * <b>کل خانواده را باطل می‌کند</b>. یعنی بدون این خط، بازگشت از
 * پس‌زمینه می‌توانست خودش کاربر را بیرون بیندازد.
 *
 * <p>⚠️ {@code api-client} هم محافظ خودش را دارد؛ این دومی لازم است چون
 * {@code refreshOnResume} آن مسیر را دور می‌زند و مستقیم به اینجا می‌آید.
 */
let refreshInFlight: Promise<string | null> | null = null;

/**
 * آخرین {@code initData}ی که سرور رد کرده — در همین بارگذاری صفحه.
 *
 * <p>⚠️ چرا نگه داشتنش درست است؟ چون {@code initData} تا پایان عمر
 * WebView <b>ثابت</b> می‌ماند. پس اگر یک بار رد شد، تا وقتی صفحه
 * دوباره بارگذاری نشود همان رشته است — و نتیجه‌اش هم عوض نمی‌شود.
 */
let rejectedInitData: string | null = null;

export const useAuthStore = create<AuthState>()((set, get) => ({
  status: 'idle',
  accessToken: null,
  user: null,
  tenant: null,
  errorMessage: null,
  writeAccess: null,

  /**
   * راه‌اندازی ورود — یک‌بار در شروع برنامه.
   *
   * <p>ترتیب عمدی است:
   * <ol>
   *   <li>اگر نشست قبلی هست، اول <b>تازه‌سازی</b> می‌کنیم و {@code initData}
   *       را دوباره مصرف نمی‌کنیم.</li>
   *   <li>وگرنه ورود تازه با {@code initData}.</li>
   * </ol>
   * ⚠️ چرا ترتیب مهم است؟ چون {@code initData} در بک‌اند یک‌بارمصرف است.
   * اگر هر بارگذاری صفحه دوباره با آن وارد شویم، بازپخش تشخیص داده می‌شود
   * و کاربر با هر بار بستن و باز کردن اپ یک خطای امنیتی می‌بیند.
   *
   * <p>🔴 و چرا حالا واقعاً کار می‌کند؟ چون توکن تازه‌سازی در
   * {@code localStorage} است ({@code lib/token-storage.ts}) و
   * پس‌زمینه‌شدن WebView پاکش نمی‌کند. پیش از آن، این شاخه عملاً هرگز
   * اجرا نمی‌شد و هر بازگشت به شاخه دوم می‌افتاد — همان حلقه‌ای که کاربر
   * گزارش داد.
   */
  bootstrap: async () => {
    if (bootstrapInFlight) {
      return bootstrapInFlight;
    }

    bootstrapInFlight = runBootstrap(set, get).finally(() => {
      bootstrapInFlight = null;
    });

    return bootstrapInFlight;
  },

  login: async () => {
    set({ status: 'authenticating', errorMessage: null });

    let initData: string;

    try {
      initData = getInitData();
    } catch (error) {
      // ⚠️ حالت «مسدود» با «ناموفق» یکی نیست: تلاش دوباره اینجا
      // هیچ‌چیز را عوض نمی‌کند، پس دکمه «تلاش مجدد» نباید نشان داده شود.
      set({
        status: error instanceof InitDataUnavailableError ? 'blocked' : 'unauthenticated',
        errorMessage: error instanceof Error ? error.message : 'ورود ممکن نشد.',
      });
      return;
    }

    /*
     * 🔴 همان رشته‌ای که سرور قبلاً ردش کرده. فرستادن دوباره‌اش
     * <b>قطعاً</b> شکست می‌خورد، پس حتی درخواست هم نمی‌فرستیم.
     *
     * ⚠️ چرا «قطعاً»؟ چون هر سه بررسی سرور روی یک رشته ثابت، یک‌طرفه
     * بدتر می‌شوند و هیچ‌کدام با گذشت زمان درست نمی‌شود:
     * <ul>
     *   <li><b>امضا</b> — تابعی از خود رشته و توکن ربات؛ ثابت است.</li>
     *   <li><b>تازگی {@code auth_date}</b> — زمان فقط جلو می‌رود، پس
     *       منقضی‌شده منقضی می‌ماند.</li>
     *   <li><b>ضدبازپخش</b> — اثر انگشت ثبت شده و تا پایان پنجره
     *       می‌ماند؛ و بعد از آن هم {@code auth_date} منقضی شده است.</li>
     * </ul>
     * یعنی نسخه پیشین که هر بار همین را می‌فرستاد، یک حلقه بی‌پایان
     * می‌ساخت که تنها خروجی‌اش یک خطای امنیتی تازه در لاگ بود.
     */
    if (rejectedInitData === initData) {
      set({
        status: 'blocked',
        errorMessage: isDevInitDataInUse()
          ? STALE_DEV_INIT_DATA_MESSAGE
          : STALE_SESSION_MESSAGE,
      });
      return;
    }

    try {
      const response = await loginWithEitaa(initData);

      applySession(set, response);
    } catch (error) {
      if (isInitDataRejected(error)) {
        rejectedInitData = initData;

        // ⚠️ «مسدود» و نه «ناموفق»: تکرارِ همین درخواست بی‌فایده است.
        // در توسعهٔ مرورگر پیام سرور («برنامک را دوباره باز کنید») گمراه‌کننده
        // است — initData از فایل env می‌آید، نه از ایتا.
        set({
          status: 'blocked',
          errorMessage: isDevInitDataInUse()
            ? STALE_DEV_INIT_DATA_MESSAGE
            : messageOf(error),
        });
        return;
      }

      set({ status: 'unauthenticated', errorMessage: messageOf(error) });
    }
  },

  logout: async () => {
    const refreshToken = readRefreshToken();

    // ⚠️ اول وضعیت محلی پاک می‌شود تا حتی اگر درخواست به سرور نرسد،
    // کاربر واقعاً خارج شده باشد. یک خروج نیمه‌کاره بدترین حالت است.
    clearRefreshToken();
    set({
      status: 'unauthenticated',
      accessToken: null,
      user: null,
      tenant: null,
      errorMessage: null,
    });

    if (refreshToken) {
      try {
        await logoutSession(refreshToken);
      } catch {
        // ⚠️ عمداً نادیده: توکن سمت سرور تا انقضایش می‌ماند، ولی
        // نشست محلی پاک شده. گزارش دادن این خطا به کاربر، فقط او را
        // نگران می‌کند بدون آنکه کاری از دستش بربیاید.
      }
    }
  },

  /**
   * تازه‌سازی نشست — پل {@code AuthBridge}.
   *
   * <p>⚠️ این تابع <b>نباید</b> استثنا پرتاب کند: {@code api-client} آن را
   * در مسیر ۴۰۱ صدا می‌زند و یک استثنا آنجا به یک خطای گمراه‌کننده تبدیل
   * می‌شود. شکست، {@code null} است.
   *
   * <p>🔴 و فراخوانی‌های هم‌زمان، یک درخواست می‌سازند — دلیلش در
   * {@code refreshInFlight}.
   */
  refresh: () => {
    if (refreshInFlight) {
      return refreshInFlight;
    }

    const inFlight = runRefresh(set);

    refreshInFlight = inFlight;

    // ⚠️ پاک‌کردن حال <b>پس از</b> نشستنش انجام می‌شود، وگرنه یک فراخوان
    // هم‌زمان که بین دو خط برسد یک درخواست دوم می‌سازد.
    //
    // ⚠️ شرط برابری لازم است: اگر این Promise دیرتر از یک تازه‌سازی
    // بعدی تمام شود، نباید حالِ آن تازه‌سازی را پاک کند.
    void inFlight.finally(() => {
      if (refreshInFlight === inFlight) {
        refreshInFlight = null;
      }
    });

    return inFlight;
  },

  refreshOnResume: async () => {
    // ⚠️ فقط وقتی نشستی هست که ارزش تازه‌کردن دارد. در حالت «مسدود»
    // یا «در حال ورود»، تازه‌سازی فقط یک درخواست بی‌فایده است.
    if (get().status !== 'authenticated' || readRefreshToken() === null) {
      return;
    }

    const token = await get().refresh();

    // ⚠️ `refresh()` توکن را فقط وقتی پاک می‌کند که سرور آن را
    // <b>قطعاً</b> رد کرده باشد. پس نبودِ توکن بعد از یک شکست یعنی نشست
    // واقعاً مرده است — نه اینکه اینترنت لحظه‌ای قطع بوده. بدون این
    // تفکیک، هر قطعی شبکه کاربر را به صفحه ورود پرت می‌کرد.
    if (token === null && readRefreshToken() === null) {
      get().onSessionLost();
    }
  },

  onSessionLost: () => {
    clearRefreshToken();
    set({
      status: 'unauthenticated',
      accessToken: null,
      user: null,
      tenant: null,
      errorMessage: 'نشست شما منقضی شده است. دوباره وارد شوید.',
    });
  },
}));

// ─────────────────────────── درونی ───────────────────────────

/**
 * اتصال پل احراز هویت — یک‌بار در راه‌اندازی.
 *
 * <p>⚠️ جدا از {@code create} نگه داشته شده تا حلقه وابستگی نسازد:
 * {@code api-client} نباید {@code auth.store} را import کند.
 */
export function wireAuthBridge(): void {
  configureAuth({
    getAccessToken: () => useAuthStore.getState().accessToken,
    refresh: () => useAuthStore.getState().refresh(),
    onSessionLost: () => useAuthStore.getState().onSessionLost(),
  });
}

/** فقط برای آزمون — حال درونی ماژول را پاک می‌کند. */
export function resetAuthFlowForTest(): void {
  bootstrapInFlight = null;
  refreshInFlight = null;
  rejectedInitData = null;
}

async function runBootstrap(
  set: (partial: Partial<AuthState>) => void,
  get: () => AuthState,
): Promise<void> {
  const refreshToken = readRefreshToken();

  if (refreshToken) {
    set({ status: 'authenticating' });

    const token = await get().refresh();

    if (token) {
      // ⚠️ تازه‌سازی توکن را می‌دهد ولی پروفایل را نه. برای داشتن نام و
      // مستأجر، یک بار {@code /auth/me} لازم است.
      try {
        const me = await fetchCurrentUser();

        set({ user: me.user, tenant: me.tenant, status: 'authenticated' });
      } catch {
        // ⚠️ تازه‌سازی موفق بود ولی پروفایل نیامد — نشست معتبر است،
        // پس کاربر را بیرون نمی‌اندازیم؛ فقط پروفایل خالی می‌ماند.
        set({ status: 'authenticated' });
      }

      // ⚠️ پرسیدن اجازه ارسال فقط پس از ورود <b>موفق</b> و فقط یک‌بار.
      //
      // 🔴 چرا داخل این شاخه و نه بعد از هر دو؟ چون در شاخه دیگر کاربر
      // وارد <i>نشده</i> است و صفحه «ورود انجام نشد» را می‌بیند. پرسیدن
      // اجازه ارسال پیام در آن لحظه یعنی کاربر یک پنجره ایتا را روی یک
      // صفحه خطا می‌بیند، برای قابلیتی که هنوز نمی‌تواند از آن استفاده
      // کند. (خودِ تابع هم یک‌بار-در-نشست است، پس این پرسشِ بی‌مورد
      // تا پایان عمر برنامک باقی می‌ماند و دیگر پرسیده نمی‌شود.)
      void askWriteAccessOnce(set, get);
    } else {
      set({ status: 'unauthenticated' });
    }

    return;
  }

  await get().login();
}

async function runRefresh(
  set: (partial: Partial<AuthState>) => void,
): Promise<string | null> {
  const refreshToken = readRefreshToken();

  if (!refreshToken) {
    return null;
  }

  try {
    const response = await refreshSession(refreshToken);

    applySession(set, response);

    return response.accessToken;
  } catch (error) {
    /*
     * 🔴 فقط خطای «نشست قطعاً مرده» توکن را باطل می‌کند.
     *
     * ⚠️ چرا این تفکیک مهم است؟ نسخه پیشین روی <b>هر</b> شکستی توکن
     * ۳۰روزه را دور می‌انداخت. یعنی یک لحظه بی‌اینترنتی هنگام باز شدن
     * اپ کافی بود تا کاربر به ورود تازه با {@code initData} پرت شود —
     * و آنجا، چون {@code initData} کهنه است، به بن‌بست می‌رسید. یعنی
     * «قطعی موقت شبکه» به «باید برنامک را ببندی و باز کنی» ترجمه
     * می‌شد. حالا فقط ۴۰۱ این کار را می‌کند، چون فقط ۴۰۱ یعنی خودِ
     * توکن تازه‌سازی بی‌اعتبار است.
     *
     * ⚠️ و چرا ۴۰۳ نه؟ چون {@code ACCOUNT_DISABLED} یا
     * {@code SUBSCRIPTION_EXPIRED} درباره <i>توکن</i> نیستند؛ کاربر
     * ممکن است بعد از رفع مشکل دوباره وارد شود و نباید مجبورش کنیم
     * برنامک را ببندد.
     */
    if (error instanceof ApiError && error.status === 401) {
      clearRefreshToken();
    }

    return null;
  }
}

function applySession(
  set: (partial: Partial<AuthState>) => void,
  response: {
    accessToken: string;
    refreshToken: string;
    user: AuthUser;
    tenant: AuthTenant | null;
  },
): void {
  writeRefreshToken(response.refreshToken);
  set({
    status: 'authenticated',
    accessToken: response.accessToken,
    user: response.user,
    tenant: response.tenant,
    errorMessage: null,
  });
}

/**
 * آیا سرور <b>خودِ {@code initData}</b> را رد کرد؟
 *
 * <p>⚠️ بک‌اند هر سه حالت (امضای نامعتبر، منقضی، تکراری) را با یک کد
 * برمی‌گرداند — {@code EITAA_INIT_DATA_INVALID} — چون تفکیکشان به بیرون
 * نشت نمی‌کند ({@code EitaaInitDataException}). پس کلاینت هم نمی‌تواند
 * و نباید تفکیک کند؛ هر سه یک معنا دارند: «برنامک را دوباره باز کن».
 */
function isInitDataRejected(error: unknown): boolean {
  return error instanceof ApiError && error.code === 'EITAA_INIT_DATA_INVALID';
}

/**
 * درخواست اجازه ارسال پیام — یک‌بار، بعد از ورود.
 *
 * <p>⚠️ نتیجه در <b>پروفایل سرور</b> ذخیره می‌شود، نه {@code localStorage}
 * ({@code docs/mini-app.md} بخش ۴.۶). اینجا فقط نگه داشته می‌شود تا UI
 * بتواند اعلان درون‌برنامه‌ای را توضیح دهد.
 */
async function askWriteAccessOnce(
  set: (partial: Partial<AuthState>) => void,
  get: () => AuthState,
): Promise<void> {
  if (get().writeAccess !== null) {
    return;
  }

  const result = await ensureWriteAccess();

  set({ writeAccess: result });
}

function messageOf(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return 'ورود ممکن نشد.';
}
