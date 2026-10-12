import type { Page } from '@playwright/test';
import type { EitaaWebAppApi, EitaaUnsafeUser } from '../../src/eitaa/types';

/**
 * جایگزین SDK ایتا برای آزمون E2E.
 *
 * <p><b>🔴 چرا SDK واقعی بارگذاری نمی‌شود؟</b> چون {@code index.html} اسکریپت
 * {@code https://developer.eitaa.com/eitaa-web-app.js} را می‌خواند و در آزمون
 * خودکار نه شبکه تضمین‌شده است و نه آن اسکریپت باید اجرا شود. اگر اجرا شود،
 * {@code window.Eitaa} را بازنویسی می‌کند و جایگزین ما را بی‌اثر می‌کند. پس
 * درخواست آن <i>مسدود</i> می‌شود (نه اینکه به شکست شبکه امید ببندیم) و
 * جایگزین پیش از بارگذاری برنامه تزریق می‌شود.
 *
 * <p>⚠️ شیء ساخته‌شده با {@link EitaaWebAppApi} نوع‌دهی می‌شود. این مهم است:
 * اگر روزی SDK رشد کند، TypeScript اینجا خطا می‌دهد و جایگزین از سطح واقعی
 * SDK عقب نمی‌ماند.
 *
 * <p>⚠️ تابع تزریق‌شده <b>در بستر مرورگر</b> اجرا می‌شود، پس رشته‌سازی
 * می‌شود و هیچ مقدارِ import‌شده‌ای در آن قابل استفاده نیست؛ فقط نوع‌ها که
 * هنگام کامپایل پاک می‌شوند.
 */

/** رشته `initData` جعلی — ساختار مستندشده، بدون امضای معتبر. */
export const FAKE_INIT_DATA =
  'query_id=AAF_test&user=%7B%22id%22%3A123456789%7D&auth_date=1700000000&hash=0000000000000000000000000000000000000000000000000000000000000000';

export interface FakeEitaaOptions {
  /** پلتفرم اعلامی SDK — `android`، `ios`، `windows`، … */
  platform?: string;
  /** رشته `initData`؛ رشته خالی یعنی «داده‌ای نیست». */
  initData?: string;
  /** پارامتر Deep Link — مثلاً `asg_123`. */
  startParam?: string;
  /** رنگ‌بندی ایتا — منبع حقیقت تم برنامه. */
  colorScheme?: 'light' | 'dark';
}

/**
 * مسدود کردن SDK واقعی ایتا.
 *
 * <p>⚠️ جدا است تا آزمون «بیرون از ایتا» بتواند SDK را مسدود کند
 * <i>بدون</i> تزریق جایگزین.
 */
export async function blockEitaaSdk(page: Page): Promise<void> {
  await page.route('**developer.eitaa.com/**', (route) => route.abort());
}

/**
 * مسدود کردن SDK واقعی و تزریق جایگزین.
 *
 * <p>⚠️ باید <b>پیش از</b> {@code page.goto} صدا زده شود، وگرنه اسکریپت
 * برنامه زودتر اجرا می‌شود و {@code checkPlatform} مقدار قدیمی را می‌بیند.
 */
export async function installFakeEitaa(
  page: Page,
  options: FakeEitaaOptions = {},
): Promise<void> {
  const {
    platform = 'android',
    initData = FAKE_INIT_DATA,
    startParam,
    colorScheme = 'light',
  } = options;

  // 🔴 مسدود کردن SDK واقعی — عمدی و صریح، نه تکیه بر قطع بودن شبکه.
  await blockEitaaSdk(page);

  await page.addInitScript(
    (args: {
      platform: string;
      initData: string;
      startParam: string | undefined;
      colorScheme: 'light' | 'dark';
    }) => {
      const noop = (): void => undefined;

      const button = {
        text: '',
        isVisible: false,
        isActive: false,
        show: noop,
        hide: noop,
        enable: noop,
        disable: noop,
        onClick: noop,
        setText: noop,
      };

      const user: EitaaUnsafeUser = {
        id: 123456789,
        first_name: 'زهرا',
        last_name: 'محمدی',
        language_code: 'fa',
        allows_write_to_pm: true,
      };

      const app: EitaaWebAppApi = {
        initData: args.initData,
        initDataUnsafe: {
          user,
          auth_date: 1_700_000_000,
          hash: '0000000000000000000000000000000000000000000000000000000000000000',
          chat_type: 'private',
          ...(args.startParam === undefined ? {} : { start_param: args.startParam }),
        },
        version: '6.0.0',
        platform: args.platform,
        colorScheme: args.colorScheme,
        themeParams: {},
        isExpanded: false,
        MainButton: button,
        SecondaryButton: button,
        BackButton: { show: noop, hide: noop, onClick: noop },
        HapticFeedback: {
          impactOccurred: noop,
          notificationOccurred: noop,
          selectionChanged: noop,
        },
        ready: noop,
        expand: noop,
        close: noop,
        isVersionAtLeast: () => true,
        onEvent: noop,
        offEvent: noop,
        showAlert: noop,
        showConfirm: noop,
        openLink: noop,
        openEitaaLink: noop,
        requestWriteAccess: (cb?: (granted: boolean) => void) => {
          cb?.(true);
        },
        requestContact: noop,
      };

      window.Eitaa = { WebApp: app };
    },
    { platform, initData, startParam, colorScheme },
  );
}

/** پاسخ واقعیِ `POST /auth/eitaa` — همان قرارداد `AuthResponse` بک‌اند. */
export const AUTH_RESPONSE = {
  accessToken: 'access-e2e',
  refreshToken: 'refresh-e2e',
  tokenType: 'Bearer',
  expiresIn: 900,
  refreshExpiresIn: 2_592_000,
  user: {
    id: '11111111-1111-1111-1111-111111111111',
    fullName: 'زهرا محمدی',
    roles: ['TEACHER'],
  },
  tenant: {
    id: '22222222-2222-2222-2222-222222222222',
    name: 'دبیرستان شهید بهشتی',
    type: 'TEACHER',
  },
  subscription: null,
  isNewUser: false,
} as const;

/**
 * جایگزین بک‌اند.
 *
 * <p>⚠️ الگو {@code **&#47;api/v1/**} است و نه یک میزبان مشخص: مقدار
 * {@code VITE_API_URL} در زمان بیلد خوانده می‌شود و روی دستگاه‌های مختلف
 * فرق می‌کند. آزمون باید به «مسیر» ببندد، نه به «میزبان» — وگرنه از آزمون
 * برنامه به آزمون پیکربندی یک دستگاه تبدیل می‌شود.
 */
export async function stubBackend(page: Page): Promise<void> {
  await page.route('**/api/v1/**', async (route) => {
    const path = new URL(route.request().url()).pathname;

    if (path.endsWith('/auth/eitaa')) {
      await route.fulfill({ json: AUTH_RESPONSE });
      return;
    }

    if (path.endsWith('/auth/me')) {
      await route.fulfill({
        json: {
          user: AUTH_RESPONSE.user,
          tenant: AUTH_RESPONSE.tenant,
          subscription: null,
        },
      });
      return;
    }

    await route.fulfill({
      status: 404,
      json: { status: 404, code: 'NOT_FOUND', message: 'این مسیر در آزمون تعریف نشده است.' },
    });
  });
}
