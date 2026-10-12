import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  ApiError,
  apiFetch,
  apiFetchAnonymous,
  configureAuth,
  resetAuthBridgeForTest,
  type AuthBridge,
} from './api-client';

/**
 * آزمون لایه HTTP.
 *
 * <p>🔴 تمرکز این فایل روی <b>توکن</b> است: چه زمانی فرستاده می‌شود، چه
 * زمانی نه، و تازه‌سازی در ۴۰۱ چند بار رخ می‌دهد. یک اشتباه اینجا یا
 * توکن را به جایی می‌فرستد که نباید، یا با تازه‌سازی هم‌زمان کل نشست
 * کاربر را می‌کشد.
 */

/** یک پاسخ JSON با کد وضعیت دلخواه. */
function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

/** خطای بک‌اند با شکل واقعی `ApiError`. */
function errorBody(code: string, message = 'خطا'): unknown {
  return { timestamp: '2026-01-01T00:00:00Z', status: 401, code, message, path: '/x' };
}

interface RecordedCall {
  url: string;
  method: string;
  authorization: string | null;
  credentials: RequestCredentials | undefined;
}

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

function recordingFetch(
  handler: (call: RecordedCall, index: number) => Response | Promise<Response>,
): { fetchImpl: typeof fetch; calls: RecordedCall[] } {
  const calls: RecordedCall[] = [];

  const fetchImpl = (input: RequestInfo | URL, init?: RequestInit) => {
    const headers = new Headers(init?.headers);
    const call: RecordedCall = {
      url: urlOf(input),
      method: init?.method ?? 'GET',
      authorization: headers.get('Authorization'),
      credentials: init?.credentials,
    };

    calls.push(call);

    return Promise.resolve(handler(call, calls.length - 1));
  };

  return { fetchImpl, calls };
}

function bridge(overrides: Partial<AuthBridge> = {}): AuthBridge {
  return {
    getAccessToken: () => 'access-1',
    refresh: () => Promise.resolve('access-2'),
    onSessionLost: () => undefined,
    ...overrides,
  };
}

beforeEach(() => {
  resetAuthBridgeForTest();
});

afterEach(() => {
  vi.unstubAllGlobals();
  resetAuthBridgeForTest();
});

describe('چسباندن توکن', () => {
  it('توکن دسترسی را در هدر می‌گذارد', async () => {
    const { fetchImpl, calls } = recordingFetch(() => jsonResponse(200, { ok: true }));
    configureAuth(bridge());

    await apiFetch<{ ok: boolean }>('/auth/me', { fetchImpl });

    expect(calls[0]?.authorization).toBe('Bearer access-1');
  });

  it('وقتی توکنی نیست، هدر نمی‌گذارد — نه هدر خالی', async () => {
    const { fetchImpl, calls } = recordingFetch(() => jsonResponse(200, {}));
    configureAuth(bridge({ getAccessToken: () => null }));

    await apiFetch('/auth/me', { fetchImpl });

    expect(calls[0]?.authorization).toBeNull();
  });

  it('🔴 درخواست ناشناس حتی با وجود توکن، آن را نمی‌فرستد', async () => {
    const { fetchImpl, calls } = recordingFetch(() => jsonResponse(200, {}));
    configureAuth(bridge());

    await apiFetchAnonymous('/auth/refresh', { fetchImpl, method: 'POST', body: {} });

    expect(calls[0]?.authorization).toBeNull();
  });

  it('کوکی نمی‌فرستد — احراز هویت فقط توکنی است', async () => {
    const { fetchImpl, calls } = recordingFetch(() => jsonResponse(200, {}));
    configureAuth(bridge());

    await apiFetch('/books', { fetchImpl });

    expect(calls[0]?.credentials).toBe('omit');
  });
});

describe('تازه‌سازی در ۴۰۱', () => {
  it('با کد «نشست مرده» یک بار تازه می‌کند و درخواست را تکرار می‌کند', async () => {
    const refresh = vi.fn(() => Promise.resolve('access-2'));
    const { fetchImpl, calls } = recordingFetch((_call, index) =>
      index === 0 ? jsonResponse(401, errorBody('TOKEN_EXPIRED')) : jsonResponse(200, { ok: true }),
    );
    configureAuth(bridge({ refresh }));

    await expect(apiFetch('/books', { fetchImpl })).resolves.toEqual({ ok: true });

    expect(refresh).toHaveBeenCalledTimes(1);
    expect(calls).toHaveLength(2);
    // 🔴 پل این آزمون عمداً «بی‌حافظه» است: `getAccessToken` همیشه
    // `access-1` می‌دهد. پس اگر درخواست دوم توکن `access-2` را دارد،
    // یعنی `api-client` توکن بازگشتی از تازه‌سازی را خودش به کار برده و
    // به این وابسته نیست که پل حال داخلی‌اش را عوض کند. این آزمون پیش از
    // اصلاح، شکست خورد و همان قرارداد نانوشته را لو داد.
    expect(calls[1]?.authorization).toBe('Bearer access-2');
  });

  it('🔴 با ۴۰۱ و کد غیر «نشست مرده» تازه نمی‌کند', async () => {
    const refresh = vi.fn(() => Promise.resolve('access-2'));
    const { fetchImpl, calls } = recordingFetch(() => jsonResponse(401, errorBody('ACCESS_DENIED')));
    configureAuth(bridge({ refresh }));

    await expect(apiFetch('/books', { fetchImpl })).rejects.toBeInstanceOf(ApiError);

    // ⚠️ تازه‌کردن روی «اجازه نداری» فقط یک درخواست بی‌فایده اضافه
    // می‌کند و می‌تواند توکن تازه‌سازی را بی‌دلیل مصرف کند.
    expect(refresh).not.toHaveBeenCalled();
    expect(calls).toHaveLength(1);
  });

  it('اگر تازه‌سازی شکست بخورد، نشست از دست رفته اعلام می‌شود', async () => {
    const onSessionLost = vi.fn();
    const { fetchImpl } = recordingFetch(() => jsonResponse(401, errorBody('TOKEN_EXPIRED')));
    configureAuth(
      bridge({ refresh: () => Promise.resolve(null), onSessionLost }),
    );

    await expect(apiFetch('/books', { fetchImpl })).rejects.toBeInstanceOf(ApiError);

    expect(onSessionLost).toHaveBeenCalledTimes(1);
  });

  it('اگر تلاش دوباره هم ۴۰۱ بدهد، دوباره تازه نمی‌کند', async () => {
    const refresh = vi.fn(() => Promise.resolve('access-2'));
    const { fetchImpl, calls } = recordingFetch(() => jsonResponse(401, errorBody('TOKEN_EXPIRED')));
    configureAuth(bridge({ refresh }));

    await expect(apiFetch('/books', { fetchImpl })).rejects.toBeInstanceOf(ApiError);

    expect(refresh).toHaveBeenCalledTimes(1);
    expect(calls).toHaveLength(2);
  });

  it('🔴 پنج ۴۰۱ هم‌زمان فقط یک تازه‌سازی می‌سازد', async () => {
    // ⚠️ این آزمون همان سناریوی مرگ‌آور است: بدون محافظ تک‌پروازی،
    // سرور توکن تازه‌سازی را «چندبار مصرف» می‌بیند و
    // TOKEN_REUSE_DETECTED کل نشست را باطل می‌کند.
    const refresh = vi.fn(
      () => new Promise<string | null>((resolve) => setTimeout(() => resolve('access-2'), 0)),
    );

    const { fetchImpl, calls } = recordingFetch((_call, index) =>
      index < 5 ? jsonResponse(401, errorBody('TOKEN_EXPIRED')) : jsonResponse(200, { ok: true }),
    );

    configureAuth(bridge({ refresh }));

    await Promise.all(
      Array.from({ length: 5 }, () => apiFetch('/books', { fetchImpl })),
    );

    expect(refresh).toHaveBeenCalledTimes(1);
    // ۵ درخواست اول + ۵ تلاش دوباره
    expect(calls).toHaveLength(10);
  });
});

describe('تبدیل خطا', () => {
  it('پیام فارسی بک‌اند را ترجیح می‌دهد', async () => {
    const { fetchImpl } = recordingFetch(() =>
      jsonResponse(400, {
        status: 400,
        code: 'VALIDATION_FAILED',
        message: 'نام کتاب الزامی است.',
      }),
    );
    configureAuth(bridge());

    await expect(apiFetch('/books', { fetchImpl })).rejects.toThrow('نام کتاب الزامی است.');
  });

  it('کد و شناسه ردیابی را نگه می‌دارد', async () => {
    const { fetchImpl } = recordingFetch(() =>
      jsonResponse(409, { status: 409, code: 'CONFLICT', message: 'تکراری', traceId: 'tr-1' }),
    );
    configureAuth(bridge());

    try {
      await apiFetch('/books', { fetchImpl });
      throw new Error('باید پرتاب می‌کرد');
    } catch (error) {
      expect(error).toBeInstanceOf(ApiError);
      expect((error as ApiError).status).toBe(409);
      expect((error as ApiError).code).toBe('CONFLICT');
      expect((error as ApiError).traceId).toBe('tr-1');
    }
  });

  it('برای ۵xx پیام عمومی فارسی می‌دهد اگر بدنه پیام نداشت', async () => {
    const { fetchImpl } = recordingFetch(() => new Response('', { status: 503 }));
    configureAuth(bridge());

    await expect(apiFetch('/books', { fetchImpl })).rejects.toThrow(/سرور/);
  });

  it('خطای شبکه، کد وضعیت صفر و کد NETWORK_ERROR می‌گیرد', async () => {
    const fetchImpl = (() => Promise.reject(new TypeError('Failed to fetch'))) as typeof fetch;
    configureAuth(bridge());

    try {
      await apiFetch('/books', { fetchImpl });
      throw new Error('باید پرتاب می‌کرد');
    } catch (error) {
      expect(error).toBeInstanceOf(ApiError);
      expect((error as ApiError).status).toBe(0);
      expect((error as ApiError).code).toBe('NETWORK_ERROR');
    }
  });

  it('🔴 مهلت تمام‌شده، درخواست را رها می‌کند نه اینکه معلق بماند', async () => {
    vi.useFakeTimers();

    try {
      const fetchImpl = ((_input: RequestInfo | URL, init?: RequestInit) =>
        new Promise<Response>((_resolve, reject) => {
          init?.signal?.addEventListener('abort', () => {
            reject(new DOMException('Aborted', 'AbortError'));
          });
        })) as typeof fetch;

      configureAuth(bridge());

      const pending = apiFetch('/books', { fetchImpl, timeoutMs: 20_000 });

      // ⚠️ انتظار <b>پیش از</b> جلو بردن زمان وصل می‌شود. اگر بعد از آن
      // وصل شود، رد شدن Promise بی‌صاحب می‌ماند و Node آن را به‌عنوان
      // «Unhandled Rejection» گزارش می‌کند — که در آزمون واقعی، یک خطای
      // گمراه‌کننده است که به آزمون بی‌ربط نسبت داده می‌شود.
      const assertion = expect(pending).rejects.toMatchObject({
        code: 'NETWORK_TIMEOUT',
        status: 0,
      });

      await vi.advanceTimersByTimeAsync(20_001);

      await assertion;
    } finally {
      vi.useRealTimers();
    }
  });

  it('خطای شبکه توکن را در پیام تکرار نمی‌کند', async () => {
    const fetchImpl = (() => Promise.reject(new TypeError('Failed to fetch'))) as typeof fetch;
    configureAuth(bridge({ getAccessToken: () => 'super-secret-token' }));

    await expect(apiFetch('/books', { fetchImpl })).rejects.not.toThrow(/super-secret-token/);
  });
});

describe('بدنه پاسخ', () => {
  it('پاسخ ۲۰۴ را بدون خطا خالی برمی‌گرداند', async () => {
    const { fetchImpl } = recordingFetch(() => new Response(null, { status: 204 }));
    configureAuth(bridge());

    await expect(apiFetch('/books/1', { fetchImpl, method: 'DELETE' })).resolves.toBeUndefined();
  });

  it('بدنه JSON نامعتبر را خطای قابل‌تشخیص می‌کند', async () => {
    const { fetchImpl } = recordingFetch(
      () => new Response('{ not json', { status: 200 }),
    );
    configureAuth(bridge());

    await expect(apiFetch('/books', { fetchImpl })).rejects.toMatchObject({
      code: 'MALFORMED_RESPONSE',
    });
  });
});

describe('پل احراز هویت', () => {
  it('اگر پل تنظیم نشده باشد، درخواست بدون توکن می‌رود ولی پرتاب نمی‌کند', async () => {
    const { fetchImpl, calls } = recordingFetch(() => jsonResponse(200, {}));

    // ⚠️ نه `configureAuth` صدا زده می‌شود و نه پل هست.
    await apiFetch('/health', { fetchImpl });

    expect(calls[0]?.authorization).toBeNull();
  });
});
