import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { resetAuthFlowForTest, useAuthStore, wireAuthBridge } from './auth.store';
import { installFakeEitaa } from '../test/fake-eitaa';
import { ApiError, resetAuthBridgeForTest } from '../lib/api-client';
import { readRefreshToken, writeRefreshToken } from '../lib/token-storage';
import { resetWriteAccessPromptForTest } from '../eitaa/write-access';

/** کلید جاری توکن تازه‌سازی — باید با `token-storage.ts` یکی بماند. */
const SESSION_KEY = 'smart-teacher.session.v1';

/**
 * آزمون جریان ورود.
 *
 * <p>🔴 سه چیز اینجا آزموده می‌شود که اگر بشکنند، کاربر یا وارد نمی‌شود یا
 * با هر بار باز کردن اپ یک خطای امنیتی می‌بیند:
 * <ol>
 *   <li>ورود تازه با {@code initData} و ذخیره توکن تازه‌سازی.</li>
 *   <li>بارگذاری دوباره، {@code initData} را <b>دوباره مصرف نمی‌کند</b> —
 *       چون در بک‌اند یک‌بارمصرف و ضدبازپخش است.</li>
 *   <li>نبودن {@code initData} به وضعیت «مسدود» می‌رود، نه «ناموفق».</li>
 * </ol>
 */

const AUTH_PATH = '/auth/eitaa';

interface Call {
  url: string;
  method: string;
  body: string | undefined;
}

const AUTH_RESPONSE = {
  accessToken: 'access-1',
  refreshToken: 'refresh-1',
  tokenType: 'Bearer',
  expiresIn: 900,
  refreshExpiresIn: 2_592_000,
  user: { id: '11111111-1111-1111-1111-111111111111', fullName: 'زهرا محمدی', roles: ['TEACHER'] },
  tenant: {
    id: '22222222-2222-2222-2222-222222222222',
    name: 'دبیرستان شهید بهشتی',
    type: 'TEACHER',
  },
  subscription: null,
  isNewUser: false,
};

/**
 * نشانی یک درخواست، مستقل از شکل ورودی.
 *
 * <p>⚠️ {@code String(input)} اینجا کافی نیست: اگر ورودی یک شیء باشد،
 * نتیجه‌اش {@code "[object Object]"} می‌شود و آزمون به‌جای اینکه نشانی
 * اشتباه را نشان دهد، یک رشته بی‌معنی را مقایسه می‌کند.
 */
function urlOf(input: RequestInfo | URL): string {
  if (typeof input === 'string') {
    return input;
  }

  return input instanceof URL ? input.href : input.url;
}

function stubServer(handler: (call: Call) => Response | Promise<Response>): Call[] {
  const calls: Call[] = [];

  vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) => {
    const call: Call = {
      url: urlOf(input),
      method: init?.method ?? 'GET',
      body: typeof init?.body === 'string' ? init.body : undefined,
    };

    calls.push(call);

    return Promise.resolve(handler(call));
  });

  return calls;
}

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

/** بازگرداندن فروشگاه به حال اولیه — وگرنه آزمون‌ها به هم وابسته می‌شوند. */
function resetStore(): void {
  useAuthStore.setState({
    status: 'idle',
    accessToken: null,
    user: null,
    tenant: null,
    errorMessage: null,
    writeAccess: null,
  });
}

beforeEach(() => {
  resetStore();
  resetAuthFlowForTest();
  window.sessionStorage.clear();
  // ⚠️ توکن تازه‌سازی حالا در localStorage است؛ پاک نکردنش آزمون‌ها را
  // به هم وابسته می‌کند.
  window.localStorage.clear();
  resetWriteAccessPromptForTest();
  wireAuthBridge();
});

afterEach(() => {
  vi.unstubAllGlobals();
  resetAuthBridgeForTest();
  resetAuthFlowForTest();
  resetStore();
});

describe('ورود تازه', () => {
  it('ورود موفق، توکن‌ها و پروفایل را می‌نشاند', async () => {
    installFakeEitaa({ initData: 'query_id=a&hash=b' });
    stubServer(() => json(200, AUTH_RESPONSE));

    await useAuthStore.getState().bootstrap();

    const state = useAuthStore.getState();

    expect(state.status).toBe('authenticated');
    expect(state.accessToken).toBe('access-1');
    expect(state.user?.fullName).toBe('زهرا محمدی');
    expect(state.tenant?.name).toBe('دبیرستان شهید بهشتی');
    // 🔴 توکن تازه‌سازی باید در localStorage بنشیند. همین یک خط است که
    // تعیین می‌کند آیا با پس‌زمینه‌شدن WebView کاربر بیرون می‌افتد یا نه.
    expect(window.localStorage.getItem(SESSION_KEY)).toBe('refresh-1');
    expect(readRefreshToken()).toBe('refresh-1');
  });

  it('initData خام را همان‌طور که هست می‌فرستد', async () => {
    installFakeEitaa({ initData: 'query_id=a&hash=b' });
    const calls = stubServer(() => json(200, AUTH_RESPONSE));

    await useAuthStore.getState().bootstrap();

    // ⚠️ روی <b>پسوند</b> بررسی می‌شود و نه روی نشانی کامل: مقدار
    // `VITE_API_URL` در زمان بیلد خوانده می‌شود و ممکن است روی دستگاه
    // دیگری فرق کند. آنچه این آزمون می‌سنجد مسیر است، نه میزبان.
    expect(calls[0]?.url.endsWith(AUTH_PATH)).toBe(true);
    expect(calls[0]?.method).toBe('POST');
    // 🔴 دست‌کاری سمت کلاینت ممنوع است: هر تغییری در رشته، امضای
    // HMAC را می‌شکند و بک‌اند آن را رد می‌کند.
    expect(calls[0]?.body).toBe(JSON.stringify({ initData: 'query_id=a&hash=b' }));
  });

  it('🔴 رد شدن initData وضعیت «مسدود» می‌دهد، نه «تلاش مجدد»', async () => {
    installFakeEitaa();
    stubServer(() =>
      json(401, {
        status: 401,
        code: 'EITAA_INIT_DATA_INVALID',
        message: 'اطلاعات ورود ایتا معتبر نیست.',
      }),
    );

    await useAuthStore.getState().bootstrap();

    const state = useAuthStore.getState();

    // 🔴 «مسدود» و نه «ناموفق». این تفکیک کل باگ کاربر است: در حالت
    // «ناموفق» دکمه «تلاش مجدد» نشان داده می‌شد و هر بار با همان
    // `initData` کهنه یک درخواست محکوم‌به‌شکست می‌رفت.
    expect(state.status).toBe('blocked');
    expect(state.errorMessage).toBe('اطلاعات ورود ایتا معتبر نیست.');
    expect(state.accessToken).toBeNull();
  });

  it('🔴 خطای گذرا وضعیت «ناموفق» می‌دهد — تلاش دوباره معنا دارد', async () => {
    // ⚠️ تفکیک عمدی: ۵xx یعنی سرور خراب بود، نه اینکه `initData` باطل
    // است. اینجا کاربر باید بتواند دوباره تلاش کند.
    installFakeEitaa();
    stubServer(() =>
      json(500, { status: 500, code: 'INTERNAL_ERROR', message: 'خطای سرور' }),
    );

    await useAuthStore.getState().bootstrap();

    expect(useAuthStore.getState().status).toBe('unauthenticated');
  });

  it('🔴 بیرون از ایتا وضعیت «مسدود» است، نه «ناموفق»', async () => {
    // ⚠️ صریح و عمدی: بدون این خط، یک `.env.local` روی دستگاه توسعه
    // می‌تواند داده جعلی را به این آزمون برساند و آن را — به‌جای بررسی
    // رفتار برنامه — به بررسی پیکربندی همان دستگاه تبدیل کند.
    vi.stubEnv('VITE_DEV_INIT_DATA', '');

    const calls = stubServer(() => json(200, AUTH_RESPONSE));

    await useAuthStore.getState().bootstrap();

    const state = useAuthStore.getState();

    expect(state.status).toBe('blocked');
    // ⚠️ در حالت مسدود، اصلاً درخواستی فرستاده نمی‌شود.
    expect(calls).toHaveLength(0);
  });

  it('🔴 دو فراخوانی هم‌زمان bootstrap فقط یک درخواست ورود می‌سازد', async () => {
    // ⚠️ React 19 در StrictMode افکت‌ها را دو بار اجرا می‌کند. بدون
    // محافظ، درخواست دوم با EITAA_INIT_DATA_REPLAYED رد می‌شد و ما در
    // حالت توسعه باگی می‌دیدیم که در Production وجود ندارد.
    installFakeEitaa();
    const calls = stubServer(() => json(200, AUTH_RESPONSE));

    await Promise.all([
      useAuthStore.getState().bootstrap(),
      useAuthStore.getState().bootstrap(),
    ]);

    expect(calls.filter((call) => call.url.endsWith('/auth/eitaa'))).toHaveLength(1);
  });
});

describe('بارگذاری مجدد با نشست موجود', () => {
  it('🔴 initData را دوباره مصرف نمی‌کند و از توکن تازه‌سازی استفاده می‌کند', async () => {
    installFakeEitaa({ initData: 'should-not-be-used' });
    writeRefreshToken('refresh-old');

    const calls = stubServer((call) => {
      if (call.url.endsWith('/auth/refresh')) {
        return json(200, { ...AUTH_RESPONSE, accessToken: 'access-2', refreshToken: 'refresh-2' });
      }

      if (call.url.endsWith('/auth/me')) {
        return json(200, { user: AUTH_RESPONSE.user, tenant: AUTH_RESPONSE.tenant, subscription: null });
      }

      return json(500, { code: 'UNEXPECTED', message: 'نباید صدا زده شود' });
    });

    await useAuthStore.getState().bootstrap();

    const state = useAuthStore.getState();

    expect(state.status).toBe('authenticated');
    expect(state.accessToken).toBe('access-2');
    expect(state.user?.fullName).toBe('زهرا محمدی');
    // 🔴 این مهم‌ترین ادعای این آزمون است: مسیر ورود تازه صدا زده نشده.
    expect(calls.some((call) => call.url.endsWith('/auth/eitaa'))).toBe(false);
    expect(readRefreshToken()).toBe('refresh-2');
  });

  it('اگر توکن تازه‌سازی مرده باشد، به ورود تازه برمی‌گردد', async () => {
    installFakeEitaa({ initData: 'fresh-data' });
    writeRefreshToken('refresh-dead');

    stubServer((call) => {
      if (call.url.endsWith('/auth/refresh')) {
        return json(401, {
          status: 401,
          code: 'TOKEN_EXPIRED',
          message: 'نشست منقضی شده است.',
        });
      }

      return json(200, AUTH_RESPONSE);
    });

    await useAuthStore.getState().bootstrap();

    // ⚠️ مسیر: تازه‌سازی شکست خورد → وضعیت «وارد نشده». کاربر با
    // زدن «تلاش مجدد» یک ورود تازه می‌کند.
    expect(useAuthStore.getState().status).toBe('unauthenticated');
    expect(readRefreshToken()).toBeNull();
  });

  it('اگر پروفایل نیامد ولی توکن تازه شد، کاربر بیرون انداخته نمی‌شود', async () => {
    installFakeEitaa();
    writeRefreshToken('refresh-old');

    stubServer((call) => {
      if (call.url.endsWith('/auth/refresh')) {
        return json(200, { ...AUTH_RESPONSE, accessToken: 'access-2' });
      }

      return json(500, { code: 'INTERNAL_ERROR', message: 'خطای سرور' });
    });

    await useAuthStore.getState().bootstrap();

    const state = useAuthStore.getState();

    // ⚠️ نشست معتبر است؛ نبود پروفایل نباید به خروج منجر شود.
    expect(state.status).toBe('authenticated');
    expect(state.accessToken).toBe('access-2');
  });
});

describe('خروج', () => {
  it('وضعیت محلی را حتی پیش از پاسخ سرور پاک می‌کند', async () => {
    installFakeEitaa();
    writeRefreshToken('refresh-1');
    useAuthStore.setState({
      status: 'authenticated',
      accessToken: 'access-1',
      user: AUTH_RESPONSE.user,
      tenant: AUTH_RESPONSE.tenant,
    });

    stubServer(() => json(500, { code: 'INTERNAL_ERROR', message: 'سرور در دسترس نیست' }));

    await useAuthStore.getState().logout();

    const state = useAuthStore.getState();

    // 🔴 یک خروج نیمه‌کاره بدترین حالت است: کاربر فکر می‌کند خارج شده
    // ولی توکن سر جایش است.
    expect(state.status).toBe('unauthenticated');
    expect(state.accessToken).toBeNull();
    expect(state.user).toBeNull();
    expect(readRefreshToken()).toBeNull();
  });
});

describe('از دست رفتن نشست', () => {
  it('همه‌چیز را پاک می‌کند و پیام می‌دهد', () => {
    writeRefreshToken('refresh-1');
    useAuthStore.setState({ status: 'authenticated', accessToken: 'access-1' });

    useAuthStore.getState().onSessionLost();

    const state = useAuthStore.getState();

    expect(state.status).toBe('unauthenticated');
    expect(state.accessToken).toBeNull();
    expect(readRefreshToken()).toBeNull();
    expect(state.errorMessage).toContain('منقضی');
  });
});

describe('تازه‌سازی', () => {
  it('🔴 هرگز استثنا پرتاب نمی‌کند — قرارداد پل', async () => {
    // ⚠️ `api-client` این تابع را در مسیر ۴۰۱ صدا می‌زند؛ پرتاب کردن
    // آنجا به یک خطای گمراه‌کننده تبدیل می‌شود.
    writeRefreshToken('refresh-1');
    stubServer(() => json(500, { code: 'INTERNAL_ERROR', message: 'خطا' }));

    await expect(useAuthStore.getState().refresh()).resolves.toBeNull();
  });

  it('بدون توکن تازه‌سازی، بدون درخواست null می‌دهد', async () => {
    const calls = stubServer(() => json(200, AUTH_RESPONSE));

    await expect(useAuthStore.getState().refresh()).resolves.toBeNull();
    expect(calls).toHaveLength(0);
  });
});

describe('پل احراز هویت', () => {
  it('توکن دسترسی فروشگاه را به لایه HTTP می‌رساند', async () => {
    installFakeEitaa();
    const calls = stubServer((call) => {
      if (call.url.endsWith('/auth/eitaa')) {
        return json(200, AUTH_RESPONSE);
      }

      return json(200, { user: AUTH_RESPONSE.user, tenant: null, subscription: null });
    });

    await useAuthStore.getState().bootstrap();

    // ⚠️ فقط برای اطمینان از اینکه پل واقعاً وصل است — مقدار توکن در
    // هدر بررسی نمی‌شود چون آزمون‌های api-client آن را جدا پوشش می‌دهند.
    expect(useAuthStore.getState().accessToken).toBe('access-1');
    expect(calls.length).toBeGreaterThan(0);
  });
});

describe('حلقه «تلاش مجدد» — باگی که کاربر گزارش داد', () => {
  it('🔴 تلاش دوباره با همان initData ردشده، هیچ درخواستی نمی‌فرستد', async () => {
    /*
     * ⚠️ این دقیقاً همان حلقه‌ای است که کاربر دید: «هر بار که برنامه رو
     * پایین و بالا می‌کنم وارد نمی‌شه و می‌گه دوباره تلاش کنین».
     *
     * نسخه پیشین در هر تلاش یک درخواست محکوم‌به‌شکست می‌فرستاد، چون
     * `getInitData()` داخل یک WebView همیشه همان رشته را برمی‌گرداند.
     */
    installFakeEitaa({ initData: 'stale-data' });

    const calls = stubServer(() =>
      json(401, {
        status: 401,
        code: 'EITAA_INIT_DATA_INVALID',
        message: 'نشست ایتا منقضی شده است. برنامک را دوباره باز کنید.',
      }),
    );

    await useAuthStore.getState().bootstrap();

    expect(calls).toHaveLength(1);

    await useAuthStore.getState().login();
    await useAuthStore.getState().login();
    await useAuthStore.getState().login();

    // 🔴 هیچ درخواست تازه‌ای — و پیامی که راه‌حل واقعی را می‌گوید.
    expect(calls).toHaveLength(1);
    expect(useAuthStore.getState().status).toBe('blocked');
    expect(useAuthStore.getState().errorMessage).toContain('دوباره از ایتا باز کنید');
  });

  it('🔴 اگر SDK داده تازه بدهد، ورود دوباره مجاز است', async () => {
    // ⚠️ این حالت واقعیِ «کاربر برنامک را بست و دوباره باز کرد» است:
    // WebView تازه یعنی `initData` تازه، و آن دیگر کهنه نیست.
    installFakeEitaa({ initData: 'stale-data' });

    const calls = stubServer((call) =>
      call.body?.includes('fresh-data') === true
        ? json(200, AUTH_RESPONSE)
        : json(401, {
            status: 401,
            code: 'EITAA_INIT_DATA_INVALID',
            message: 'کهنه است.',
          }),
    );

    await useAuthStore.getState().bootstrap();

    expect(useAuthStore.getState().status).toBe('blocked');

    installFakeEitaa({ initData: 'fresh-data' });

    await useAuthStore.getState().login();

    expect(useAuthStore.getState().status).toBe('authenticated');
    expect(calls).toHaveLength(2);
  });
});

describe('امنیت توکن تازه‌سازی', () => {
  it('🔴 قطعی شبکه توکن ۳۰روزه را دور نمی‌اندازد', async () => {
    /*
     * ⚠️ این یک باگ واقعی و پرهزینه بود: نسخه پیشین روی هر شکستی — از
     * جمله یک لحظه بی‌اینترنتی — توکن تازه‌سازی را پاک می‌کرد. کاربر به
     * ورود تازه با `initData` پرت می‌شد و آنجا، چون `initData` کهنه بود،
     * به بن‌بست می‌رسید.
     */
    writeRefreshToken('refresh-alive');
    stubServer(() =>
      json(500, { status: 500, code: 'INTERNAL_ERROR', message: 'خطای سرور' }),
    );

    await expect(useAuthStore.getState().refresh()).resolves.toBeNull();

    expect(readRefreshToken()).toBe('refresh-alive');
  });

  it('🔴 نرسیدن درخواست به سرور هم توکن را دور نمی‌اندازد', async () => {
    writeRefreshToken('refresh-alive');
    vi.stubGlobal('fetch', () => Promise.reject(new TypeError('Failed to fetch')));

    await expect(useAuthStore.getState().refresh()).resolves.toBeNull();

    expect(readRefreshToken()).toBe('refresh-alive');
  });

  it('🔴 ۴۰۱ توکن را باطل می‌کند — نشست واقعاً مرده است', async () => {
    writeRefreshToken('refresh-dead');
    stubServer(() =>
      json(401, { status: 401, code: 'TOKEN_REUSE_DETECTED', message: 'باطل شد' }),
    );

    await expect(useAuthStore.getState().refresh()).resolves.toBeNull();

    expect(readRefreshToken()).toBeNull();
  });

  it('🔴 دو تازه‌سازی هم‌زمان فقط یک درخواست می‌سازد', async () => {
    /*
     * 🔴 چرا حیاتی است؟ توکن تازه‌سازی یک‌بارمصرف است. دو درخواست
     * هم‌زمان با همان توکن، در سرور «استفاده مجدد» تشخیص داده می‌شود و
     * **کل خانواده توکن باطل می‌شود** — یعنی یک صفحه شلوغ می‌توانست خودش
     * کاربر را بیرون بیندازد.
     */
    writeRefreshToken('refresh-1');

    const calls = stubServer((call) =>
      call.url.endsWith('/auth/refresh')
        ? json(200, { ...AUTH_RESPONSE, refreshToken: 'refresh-2' })
        : json(500, { code: 'UNEXPECTED', message: 'نباید صدا زده شود' }),
    );

    const [first, second] = await Promise.all([
      useAuthStore.getState().refresh(),
      useAuthStore.getState().refresh(),
    ]);

    expect(calls.filter((call) => call.url.endsWith('/auth/refresh'))).toHaveLength(1);
    expect(first).toBe('access-1');
    expect(second).toBe('access-1');
  });
});

describe('بازگشت از پس‌زمینه', () => {
  it('🔴 در حالت واردشده، نشست را بی‌صدا تازه می‌کند', async () => {
    // ⚠️ سناریوی کاربر: اپ را نیم‌ساعت کنار گذاشته و برگشته. توکن
    // دسترسی ۱۵ دقیقه‌ای مرده، ولی توکن تازه‌سازی سالم است.
    writeRefreshToken('refresh-1');
    useAuthStore.setState({ status: 'authenticated', accessToken: 'access-old' });

    const calls = stubServer(() =>
      json(200, { ...AUTH_RESPONSE, accessToken: 'access-new', refreshToken: 'refresh-2' }),
    );

    await useAuthStore.getState().refreshOnResume();

    expect(calls.filter((call) => call.url.endsWith('/auth/refresh'))).toHaveLength(1);
    expect(useAuthStore.getState().accessToken).toBe('access-new');
    expect(useAuthStore.getState().status).toBe('authenticated');
  });

  it('خارج از حالت واردشده، هیچ درخواستی نمی‌فرستد', async () => {
    const calls = stubServer(() => json(200, AUTH_RESPONSE));

    await useAuthStore.getState().refreshOnResume();

    expect(calls).toHaveLength(0);
  });

  it('🔴 توکن مرده، کاربر را بیرون می‌آورد', async () => {
    writeRefreshToken('refresh-dead');
    useAuthStore.setState({ status: 'authenticated', accessToken: 'access-old' });
    stubServer(() =>
      json(401, { status: 401, code: 'TOKEN_EXPIRED', message: 'منقضی شده است.' }),
    );

    await useAuthStore.getState().refreshOnResume();

    expect(useAuthStore.getState().status).toBe('unauthenticated');
    expect(readRefreshToken()).toBeNull();
  });

  it('🔴 قطعی شبکه کاربر را بیرون نمی‌آورد', async () => {
    writeRefreshToken('refresh-alive');
    useAuthStore.setState({ status: 'authenticated', accessToken: 'access-old' });
    stubServer(() =>
      json(503, { status: 503, code: 'INTERNAL_ERROR', message: 'در دسترس نیست' }),
    );

    await useAuthStore.getState().refreshOnResume();

    // ⚠️ کاربر وارد می‌ماند. یک قطعی موقت شبکه نباید به «دوباره وارد
    // شو» ترجمه شود — و آن هم وقتی `initData` کهنه است، به بن‌بست می‌رسد.
    expect(useAuthStore.getState().status).toBe('authenticated');
    expect(readRefreshToken()).toBe('refresh-alive');
  });
});

describe('ApiError', () => {
  it('۴۰۱ را «نشست مرده» می‌داند و ۴۰۳ را نه', () => {
    const unauthorized = new ApiError({ message: 'x', status: 401 });
    const forbidden = new ApiError({ message: 'x', status: 403 });

    expect(unauthorized.isSessionDead()).toBe(true);
    expect(forbidden.isSessionDead()).toBe(false);
  });
});
