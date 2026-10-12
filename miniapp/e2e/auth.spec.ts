import { expect, test, type Page } from '@playwright/test';
import {
  AUTH_RESPONSE,
  FAKE_INIT_DATA,
  blockEitaaSdk,
  installFakeEitaa,
  stubBackend,
} from './support/eitaa';

/**
 * جریان E2E شماره ۱ از {@code docs/mini-app.md} بخش ۱۲:
 * <b>ورود با {@code initData} جعلی (Mock)</b>.
 *
 * <p>⚠️ این آزمون‌ها در یک مرورگر واقعی اجرا می‌شوند، پس چیزی را می‌سنجند
 * که آزمون واحد نمی‌تواند: بارگذاری ماژول‌ها، ترتیب واقعی افکت‌های React،
 * مسیریابی History API، و تصمیم واقعی {@code import.meta.env} در حالت
 * توسعه.
 *
 * <p>⚠️ ورودی‌های بیرونی هر دو جایگزین می‌شوند: SDK ایتا و بک‌اند. پس آزمون
 * نه به شبکه وابسته است و نه به بالا بودن سرور.
 */

/** ثبت درخواست‌های ورود — پیش از {@code goto} صدا زده می‌شود. */
function recordAuthRequests(page: Page): string[] {
  const bodies: string[] = [];

  page.on('request', (request) => {
    if (request.url().includes('/auth/eitaa')) {
      bodies.push(request.postData() ?? '');
    }
  });

  return bodies;
}

test('ورود با initData جعلی کاربر را به داشبورد می‌برد', async ({ page }) => {
  await installFakeEitaa(page);
  await stubBackend(page);
  const authBodies = recordAuthRequests(page);

  await page.goto('/');

  // ⚠️ نام کاربر از پاسخ سرور می‌آید، نه از `initDataUnsafe` — پس این
  // ادعا ثابت می‌کند کل زنجیره (SDK → درخواست → پاسخ → فروشگاه) کار کرده.
  await expect(page.getByRole('heading', { name: 'سلام زهرا محمدی' })).toBeVisible();
  await expect(page.getByText(AUTH_RESPONSE.tenant.name)).toBeVisible();

  // 🔴 `initData` خام فرستاده می‌شود و دست‌کاری نمی‌شود: هر تغییری امضای
  // HMAC را می‌شکند و بک‌اند آن را رد می‌کند.
  expect(authBodies).toHaveLength(1);
  expect(JSON.parse(authBodies[0] ?? '{}')).toEqual({ initData: FAKE_INIT_DATA });

  // ⚠️ بنر «حالت توسعه» نباید دیده شود: داده از SDK آمده، نه از متغیر
  // محیطی توسعه.
  await expect(page.getByText('حالت توسعه:')).toHaveCount(0);
});

test('پنج زبانه هست و ناوبری بین آن‌ها کار می‌کند', async ({ page }) => {
  await installFakeEitaa(page);
  await stubBackend(page);

  await page.goto('/');

  const tabs = page.getByRole('navigation', { name: 'زبانه‌های اصلی' }).getByRole('link');

  // 🔴 مستند می‌گوید حداکثر پنج زبانه — این ادعا همان را قفل می‌کند.
  await expect(tabs).toHaveCount(5);

  await tabs.filter({ hasText: 'کتاب‌ها' }).click();
  await expect(page).toHaveURL(/\/books$/);

  // ⚠️ `exact: true` عمدی است: `stubBackend` فقط `/auth/eitaa` و `/auth/me`
  // را پاسخ می‌دهد و بقیه مسیرها ۴۰۴ می‌شوند، پس صفحه کتاب‌ها سرصفحه
  // خطای «کتاب‌ها بارگذاری نشد» هم رندر می‌کند. بدون تطبیق دقیق، locator
  // به هر دو سرصفحه می‌خورد و با strict mode violation می‌شکند.
  await expect(page.getByRole('heading', { name: 'کتاب‌ها', exact: true })).toBeVisible();

  // ⚠️ زبانه فعال باید `aria-current` داشته باشد — نشانه‌ای که صفحه‌خوان
  // برای فهم «کجای برنامه هستم» لازم دارد.
  await expect(tabs.filter({ hasText: 'کتاب‌ها' })).toHaveAttribute('aria-current', 'page');

  await tabs.filter({ hasText: 'تنظیمات' }).click();
  await expect(page).toHaveURL(/\/settings$/);
});

test('بیرون از ایتا صفحه «باید از داخل ایتا باز شود» دیده می‌شود', async ({ page }) => {
  await blockEitaaSdk(page);
  await stubBackend(page);
  const authBodies = recordAuthRequests(page);

  await page.goto('/');

  await expect(
    page.getByRole('heading', { name: 'این برنامه باید از داخل ایتا باز شود.' }),
  ).toBeVisible();

  // 🔴 هیچ درخواست ورودی فرستاده نمی‌شود: نبود `initData` یعنی ورود
  // ممکن نیست، پس درخواست بی‌فایده فقط بار سرور است.
  expect(authBodies).toHaveLength(0);

  // ⚠️ و هیچ نوار زبانه‌ای رندر نمی‌شود — کاربر نباید به صفحه‌ای برسد که
  // داده‌اش را نمی‌تواند بگیرد.
  await expect(page.getByRole('navigation', { name: 'زبانه‌های اصلی' })).toHaveCount(0);
});

test('روی iOS پیام روشن دیده می‌شود، نه صفحه سفید', async ({ page }) => {
  await installFakeEitaa(page, { platform: 'ios' });
  await stubBackend(page);

  await page.goto('/');

  // 🔴 این همان معیار پذیرش فاز ۲ است: ایتا برنامک را روی iOS اجرا
  // نمی‌کند، و کاربر باید دلیلش را ببیند.
  await expect(
    page.getByRole('heading', {
      name: 'این برنامه در نسخه iOS ایتا پشتیبانی نمی‌شود. لطفاً از نسخه اندروید یا ویندوز استفاده کنید.',
    }),
  ).toBeVisible();
  await expect(page.getByText('روی گوشی اندروید یا نسخه دسکتاپ ویندوز ایتا')).toBeVisible();
});

test('start_param معتبر (UUID) کاربر را به صفحه تکلیف می‌برد', async ({ page }) => {
  const assignmentId = '33333333-3333-3333-3333-333333333333';

  await installFakeEitaa(page, { startParam: `asg_${assignmentId}` });
  await stubBackend(page);

  await page.route(`**/api/v1/assignments/${assignmentId}`, async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        json: {
          id: assignmentId,
          classroomId: '44444444-4444-4444-4444-444444444444',
          classroomName: 'کلاس پنجم',
          bookId: null,
          pageFrom: null,
          pageTo: null,
          title: 'تکلیف Deep Link',
          description: null,
          type: 'HOMEWORK',
          maxScore: 10,
          dueAt: null,
          status: 'PUBLISHED',
          aiEvaluationEnabled: true,
          submissionCount: 0,
          eitaaChatId: null,
          eitaaMessageStatus: null,
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        },
      });
      return;
    }
    await route.continue();
  });

  await page.route(`**/api/v1/assignments/${assignmentId}/submissions`, async (route) => {
    await route.fulfill({ json: [] });
  });

  await page.route('**/api/v1/classes/*/students*', async (route) => {
    await route.fulfill({
      json: {
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true,
      },
    });
  });

  await page.goto('/');

  await expect(page).toHaveURL(new RegExp(`/assignments/${assignmentId.replace(/-/g, '\\-')}$`));
  await expect(page.getByRole('heading', { name: 'تکلیف Deep Link' })).toBeVisible();
});

test('🔴 start_param نامعتبر نادیده گرفته می‌شود', async ({ page }) => {
  // ⚠️ هر کاربری می‌تواند لینکی با هر مقدار بسازد. این مقدار نباید به
  // مسیر تبدیل شود.
  await installFakeEitaa(page, { startParam: 'asg_1_2' });
  await stubBackend(page);

  await page.goto('/');

  await expect(page.getByRole('heading', { name: 'سلام زهرا محمدی' })).toBeVisible();
  await expect(page).toHaveURL(/\/$/);
});

/**
 * ⚠️ این آزمون عمداً «مسیر خوش» را نمی‌سنجد.
 *
 * <p>ورود ناموفق از طریق آزمون واحد پوشش داده شده است؛ اینجا فقط یک چیز
 * سنجیده می‌شود که فقط در مرورگر معلوم می‌شود: اینکه شکست سرور به
 * <b>صفحه سفید</b> تبدیل نمی‌شود.
 */
test('شکست سرور صفحه سفید نمی‌سازد و دکمه تلاش مجدد دارد', async ({ page }) => {
  await installFakeEitaa(page);
  await page.route('**/api/v1/**', (route) =>
    route.fulfill({
      status: 503,
      json: { status: 503, code: 'INTERNAL_ERROR', message: 'سرور در دسترس نیست.' },
    }),
  );

  await page.goto('/');

  await expect(page.getByRole('heading', { name: 'ورود انجام نشد' })).toBeVisible();
  await expect(page.getByText('سرور در دسترس نیست.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'تلاش مجدد' })).toBeVisible();
});
