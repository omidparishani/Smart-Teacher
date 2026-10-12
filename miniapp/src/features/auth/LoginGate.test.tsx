import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { RouterProvider, createMemoryRouter } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { routes } from '../../router/routes';
import { createQueryClient } from '../../lib/query-client';
import { resetAuthFlowForTest, useAuthStore, wireAuthBridge } from '../../stores/auth.store';
import { resetAuthBridgeForTest } from '../../lib/api-client';
import { installFakeEitaa } from '../../test/fake-eitaa';
import { resetWriteAccessPromptForTest } from '../../eitaa/write-access';
import { PLATFORM_MESSAGES } from '../../lib/platform-guard';

/**
 * آزمون دروازه ورود — از دید کاربر.
 *
 * <p>⚠️ چرا آزمون کامپوننتی وقتی منطق جدا آزموده شده؟ چون چیزی که اینجا
 * سنجیده می‌شود <b>سیم‌کشی</b> است، نه منطق: آیا گارد پلتفرم واقعاً
 * <i>قبل از</i> صفحه نشان داده می‌شود؟ آیا Deep Link پس از ورود واقعاً
 * کاربر را جابه‌جا می‌کند؟ این‌ها با آزمون واحد دیده نمی‌شوند.
 */

const ASSIGNMENT_ID = '33333333-3333-3333-3333-333333333333';

const AUTH_RESPONSE = {
  accessToken: 'access-1',
  refreshToken: 'refresh-1',
  tokenType: 'Bearer',
  expiresIn: 900,
  refreshExpiresIn: 2_592_000,
  user: { id: '11111111-1111-1111-1111-111111111111', fullName: 'زهرا محمدی', roles: ['TEACHER'] },
  tenant: { id: '22222222-2222-2222-2222-222222222222', name: 'دبیرستان شهید بهشتی', type: 'TEACHER' },
  subscription: null,
  isNewUser: false,
};

function stubLogin(): void {
  const emptyPage = {
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  };

  vi.stubGlobal('fetch', (input: RequestInfo | URL) => {
    // ⚠️ `String(input)` روی یک شیء Request رشته `'[object Object]'`
    // می‌سازد. در عمل fetch همیشه با رشته صدا زده می‌شود، ولی اگر روزی
    // کسی Request بفرستد، آزمون نباید در سکوت سبز شود.
    let url: string;
    if (typeof input === 'string') {
      url = input;
    } else if (input instanceof URL) {
      url = input.href;
    } else {
      url = input.url;
    }

    // پس از ورود، داشبورد کلاس‌ها و دانش‌آموزان را می‌پرسد.
    if (url.includes('/classes') || url.includes('/students')) {
      return Promise.resolve(
        new Response(JSON.stringify(emptyPage), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );
    }

    if (url.includes(`/assignments/${ASSIGNMENT_ID}/submissions`)) {
      return Promise.resolve(
        new Response(JSON.stringify([]), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );
    }

    if (url.includes(`/assignments/${ASSIGNMENT_ID}`)) {
      return Promise.resolve(
        new Response(
          JSON.stringify({
            id: ASSIGNMENT_ID,
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
          }),
          {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          },
        ),
      );
    }

    return Promise.resolve(
      new Response(JSON.stringify(AUTH_RESPONSE), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
  });
}

function renderApp(initialPath = '/') {
  const router = createMemoryRouter(routes, { initialEntries: [initialPath] });

  return render(
    <QueryClientProvider client={createQueryClient()}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  window.sessionStorage.clear();
  // 🔴 حال درونی `auth.store` (از جمله «این initData قبلاً رد شد»)
  // بین آزمون‌ها پاک می‌شود. بدون آن، آزمونی که ورود ناموفق می‌سازد،
  // آزمون بعدی را هم — بی‌آنکه درخواستی بفرستد — «مسدود» می‌کند.
  resetAuthFlowForTest();
  resetWriteAccessPromptForTest();
  useAuthStore.setState({
    status: 'idle',
    accessToken: null,
    user: null,
    tenant: null,
    errorMessage: null,
    writeAccess: null,
  });
  wireAuthBridge();
});

afterEach(() => {
  vi.unstubAllGlobals();
  resetAuthBridgeForTest();
  resetAuthFlowForTest();
});

describe('گارد پلتفرم', () => {
  it('🔴 بیرون از ایتا، صفحه مسدود نشان داده می‌شود نه صفحه سفید', async () => {
    stubLogin();

    renderApp();

    expect(await screen.findByText(PLATFORM_MESSAGES.notInEitaa)).toBeInTheDocument();
  });

  it('🔴 روی iOS، کاربر پیام اندروید/ویندوز می‌بیند', async () => {
    installFakeEitaa({ platform: 'ios' });
    stubLogin();

    renderApp();

    expect(await screen.findByText(PLATFORM_MESSAGES.ios)).toBeInTheDocument();
  });

  it('بیرون از ایتا حتی یک درخواست ورود هم فرستاده نمی‌شود', async () => {
    const fetchSpy = vi.fn();
    vi.stubGlobal('fetch', fetchSpy);

    renderApp();

    await screen.findByText(PLATFORM_MESSAGES.notInEitaa);

    expect(fetchSpy).not.toHaveBeenCalled();
  });
});

describe('ورود', () => {
  it('پس از ورود، صفحه خانه نام کاربر را نشان می‌دهد', async () => {
    installFakeEitaa();
    stubLogin();

    renderApp();

    expect(await screen.findByText(/زهرا محمدی/)).toBeInTheDocument();
    expect(screen.getByText('دبیرستان شهید بهشتی')).toBeInTheDocument();
  });

  it('نوار پایین هر پنج زبانه را دارد', async () => {
    installFakeEitaa();
    stubLogin();

    renderApp();

    await screen.findByText(/زهرا محمدی/);

    const tabBar = screen.getByRole('navigation', { name: 'زبانه‌های اصلی' });

    for (const label of ['خانه', 'کتاب‌ها', 'تکالیف', 'دستیار', 'تنظیمات']) {
      expect(
        // ⚠️ فقط داخل نوار پایین — میان‌بُرهای داشبورد هم لینک دارند.
        tabBar.querySelector(`a`) &&
          Array.from(tabBar.querySelectorAll('a')).some((link) =>
            link.textContent?.includes(label),
          ),
      ).toBe(true);
    }
  });

  it('🔴 رد شدن initData دکمه «تلاش مجدد» نمی‌سازد — حلقه را می‌بندد', async () => {
    /*
     * ⚠️ این آزمون رفتار عمداً تغییر‌یافته‌ای را قفل می‌کند. نسخه پیشین
     * در این حالت دکمه «تلاش مجدد» نشان می‌داد و کاربر با هر فشار، یک
     * درخواست محکوم‌به‌شکست می‌فرستاد — چون `getInitData()` داخل یک
     * WebView همیشه همان رشته را برمی‌گرداند. همان چیزی که کاربر گزارش
     * داد: «می‌گه دوباره تلاش کنین».
     */
    const app = installFakeEitaa();
    const closeSpy = vi.spyOn(app, 'close');

    vi.stubGlobal('fetch', () =>
      Promise.resolve(
        new Response(
          JSON.stringify({
            status: 401,
            code: 'EITAA_INIT_DATA_INVALID',
            message: 'اطلاعات ورود ایتا معتبر نیست.',
          }),
          { status: 401, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    );

    renderApp();

    expect(await screen.findByText('اطلاعات ورود ایتا معتبر نیست.')).toBeInTheDocument();

    // 🔴 تلاش دوباره در این حالت بی‌فایده است، پس نباید پیشنهاد شود.
    expect(screen.queryByRole('button', { name: /تلاش مجدد/ })).not.toBeInTheDocument();

    // ⚠️ تنها اقدامی که واقعاً `initData` تازه می‌سازد.
    fireEvent.click(screen.getByRole('button', { name: /بستن برنامک/ }));

    expect(closeSpy).toHaveBeenCalledTimes(1);
  });

  it('خطای گذرا، دکمه تلاش مجدد نشان می‌دهد', async () => {
    // ⚠️ تفکیک عمدی: ۵xx یعنی سرور خراب بود، نه اینکه `initData` باطل
    // است — اینجا تلاش دوباره کاملاً معنا دارد.
    installFakeEitaa();

    vi.stubGlobal('fetch', () =>
      Promise.resolve(
        new Response(
          JSON.stringify({
            status: 500,
            code: 'INTERNAL_ERROR',
            message: 'خطای غیرمنتظره در سرور رخ داد.',
          }),
          { status: 500, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    );

    renderApp();

    expect(await screen.findByText('خطای غیرمنتظره در سرور رخ داد.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /تلاش مجدد/ })).toBeInTheDocument();
  });
});

describe('مسیریابی Deep Link', () => {
  it('🔴 پس از ورود، کاربر به مقصد start_param می‌رود', async () => {
    installFakeEitaa({ startParam: `asg_${ASSIGNMENT_ID}` });
    stubLogin();

    renderApp();

    expect(await screen.findByRole('heading', { name: 'تکلیف Deep Link' })).toBeInTheDocument();
    expect(screen.queryByText(/زهرا محمدی/)).not.toBeInTheDocument();
  });

  it('start_param نامعتبر، کاربر را جابه‌جا نمی‌کند', async () => {
    installFakeEitaa({ startParam: 'asg_42_evil' });
    stubLogin();

    renderApp();

    // ⚠️ کاربر در خانه می‌ماند — نه در صفحه‌ای با شناسه آلوده.
    expect(await screen.findByText(/زهرا محمدی/)).toBeInTheDocument();
    expect(screen.queryByText(/شناسه:/)).not.toBeInTheDocument();
  });

  it('مسیر دستی با شناسه نامعتبر، صفحه «پیدا نشد» می‌دهد', async () => {
    installFakeEitaa();
    stubLogin();

    renderApp('/assignments/not-a-valid-id');

    expect(await screen.findByText('تکلیف خواسته‌شده پیدا نشد.')).toBeInTheDocument();
  });
});

describe('تم', () => {
  /*
   * 🔴 آزمون رگرسیون برای یک نقص واقعی همین فاز.
   *
   * صفحه ورود به‌جای `detectTheme()` وضعیت اولیه فروشگاه را می‌خواند که
   * همیشه `light` است. نتیجه: تنظیم رنگ ایتا کاملاً نادیده می‌ماند. هیچ
   * آزمونی آن را نمی‌گرفت چون `detectTheme` جداگانه آزموده می‌شد و کسی
   * نمی‌سنجید که <i>صدا زده می‌شود</i> یا نه.
   */
  it('🔴 تم از رنگ‌بندی ایتا گرفته می‌شود', async () => {
    installFakeEitaa({ colorScheme: 'dark' });
    stubLogin();

    renderApp();

    await screen.findByText(/زهرا محمدی/);

    expect(document.documentElement.dataset.theme).toBe('dark');
  });

  it('با رنگ‌بندی روشن ایتا، تم روشن می‌ماند', async () => {
    installFakeEitaa({ colorScheme: 'light' });
    stubLogin();

    renderApp();

    await screen.findByText(/زهرا محمدی/);

    expect(document.documentElement.dataset.theme).toBe('light');
  });
});

describe('بنرها', () => {
  it('🔴 روی دسکتاپ، بنر «فقط اندروید و ویندوز» دیده می‌شود', async () => {
    installFakeEitaa({ platform: 'tdesktop' });
    stubLogin();

    renderApp();

    await screen.findByText(/زهرا محمدی/);

    expect(screen.getByText(/فقط روی/)).toBeInTheDocument();
  });

  it('روی اندروید بنر نمایش داده نمی‌شود — آنجا همه‌چیز پشتیبانی می‌شود', async () => {
    installFakeEitaa({ platform: 'android' });
    stubLogin();

    renderApp();

    await screen.findByText(/زهرا محمدی/);

    expect(screen.queryByText(/فقط روی/)).not.toBeInTheDocument();
  });
});
