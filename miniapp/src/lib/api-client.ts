import { isSessionDeadCode } from '../types/error-codes';
import type { ApiErrorBody } from '../types/api';

/**
 * تنها نقطه تماس با HTTP.
 *
 * <p><b>🔴 چرا یک فایل مرکزی؟</b> چون چهار چیز باید در <i>هر</i> درخواست
 * رعایت شود و اگر هر فراخوان خودش {@code fetch} بنویسد، یکی از آن‌ها را
 * فراموش می‌کند:
 * <ol>
 *   <li>چسباندن {@code Authorization} — منبع توکن، نه از کامپوننت.</li>
 *   <li>تازه‌سازی خودکار در ۴۰۱ — <b>یک‌بار</b>، و فقط برای کدهای «نشست
 *       مرده».</li>
 *   <li>تبدیل بدنه خطا به یک استثنای قابل‌تشخیص.</li>
 *   <li>هیچ توکنی هرگز لاگ نشود.</li>
 * </ol>
 */

/** نشانی پایه — از متغیر محیطی. */
const BASE_URL: string = import.meta.env.VITE_API_URL ?? '/api/v1';

/**
 * مهلت پیش‌فرض درخواست.
 *
 * <p>⚠️ در WebView ایتا، یک درخواست بدون مهلت می‌تواند تا ابد معلق بماند
 * (شبکه موبایل، پراکسی، یا سرور بی‌پاسخ) و کاربر یک Skeleton بی‌پایان
 * ببیند. بدون مهلت، هیچ خطایی هم ثبت نمی‌شود.
 */
const DEFAULT_TIMEOUT_MS = 20_000;

/**
 * پل احراز هویت — تا این فایل به {@code auth.store} وابسته نشود.
 *
 * <p>⚠️ اگر مستقیم import می‌کردیم، حلقه وابستگی می‌ساختیم:
 * {@code store → api-client → store}. با این پل، جهت وابستگی یک‌طرفه
 * می‌ماند و آزمون هم آسان است: یک پل ساختگی می‌دهیم.
 */
export interface AuthBridge {
  /** توکن دسترسی فعلی — از حافظه، نه از حافظه‌های مرورگر. */
  getAccessToken(): string | null;
  /** تازه‌سازی؛ توکن تازه یا `null` اگر نشست مرده است. */
  refresh(): Promise<string | null>;
  /** نشست از دست رفت — پاک‌سازی وضعیت. */
  onSessionLost(): void;
}

let authBridge: AuthBridge | null = null;

/** یک‌بار در راه‌اندازی برنامه صدا زده می‌شود. */
export function configureAuth(bridge: AuthBridge): void {
  authBridge = bridge;
}

/** فقط برای آزمون — پل را برمی‌دارد. */
export function resetAuthBridgeForTest(): void {
  authBridge = null;
  refreshInFlight = null;
}

/** خطای API — با کد قابل‌تشخیص. */
export class ApiError extends Error {
  /** کد وضعیت HTTP — صفر یعنی درخواست اصلاً به سرور نرسید. */
  readonly status: number;

  /** کد خطای بک‌اند — اگر نبود یا ناشناخته بود، خام نگه داشته می‌شود. */
  readonly code: string | undefined;

  /** شناسه ردیابی سرور — برای پیگیری در لاگ‌ها. */
  readonly traceId: string | undefined;

  /** جزئیات اعتبارسنجی فیلدها — اگر بک‌اند فرستاده باشد. */
  readonly details: unknown;

  constructor(params: {
    message: string;
    status: number;
    code?: string | undefined;
    traceId?: string | undefined;
    details?: unknown;
  }) {
    super(params.message);
    this.name = 'ApiError';
    this.status = params.status;
    this.code = params.code;
    this.traceId = params.traceId;
    this.details = params.details;
  }

  /** آیا این خطا یعنی باید دوباره وارد شویم؟ */
  isSessionDead(): boolean {
    return this.status === 401 || isSessionDeadCode(this.code);
  }
}

/** گزینه‌های درخواست. */
export interface ApiRequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  /** مهلت سفارشی — مثلاً برای فراخوانی‌های AI بلندتر. */
  timeoutMs?: number;
  /** ⚠️ فقط آزمون — پیش‌فرض `fetch` جهانی. */
  fetchImpl?: typeof fetch;
}

/**
 * یک درخواست به بک‌اند.
 *
 * @throws ApiError در هر پاسخ ناموفق — و همچنین در خطای شبکه یا مهلت
 * @throws Error اگر پل احراز هویت تنظیم نشده باشد
 */
export async function apiFetch<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const response = await send(path, options, /* allowRefresh */ true);

  return parseSuccess<T>(response);
}

/**
 * درخواست بدون توکن — فقط برای ورود.
 *
 * <p>⚠️ جدا است تا هیچ مسیر دیگری نتواند ناخواسته بدون احراز هویت برود.
 */
export async function apiFetchAnonymous<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const response = await send(path, { ...options, skipAuth: true }, false);

  return parseSuccess<T>(response);
}

// ─────────────────────────── درونی ───────────────────────────

/** تازه‌سازی در جریان — برای پاسخ‌های هم‌زمان، فقط یکی. */
let refreshInFlight: Promise<string | null> | null = null;

interface SendOptions extends ApiRequestOptions {
  skipAuth?: boolean;
}

async function send(
  path: string,
  options: SendOptions,
  allowRefresh: boolean,
): Promise<Response> {
  const response = await rawFetch(path, options);

  if (response.status !== 401 || !allowRefresh || options.skipAuth) {
    if (!response.ok) {
      throw await toApiError(response);
    }
    return response;
  }

  // 🔴 ۴۰۱ — اما نه هر ۴۰۱. اول باید بدانیم بک‌اند چه کدی داده.
  // بدنه یک‌بار خوانده می‌شود، پس همان را برای خطای نهایی نگه می‌داریم.
  const body = await readJsonSafely(response);

  if (!isSessionDeadCode(codeOf(body))) {
    // ⚠️ ۴۰۱ با کد دیگر (یا بدون کد) تازه‌سازی نمی‌شود: تازه‌کردن
    // توکن روی «اجازه نداری» فقط یک درخواست بی‌فایده اضافه می‌کند.
    throw toApiErrorFromBody(response.status, body);
  }

  const token = await refreshOnce();

  if (!token) {
    authBridge?.onSessionLost();
    throw toApiErrorFromBody(401, body);
  }

  // ⚠️ فقط یک بار تلاش دوباره. اگر این هم ۴۰۱ بدهد، نشست مرده است.
  //
  // 🔴 توکن تازه <b>صریحاً</b> پاس داده می‌شود و به حال داخلی پل تکیه
  // نمی‌کنیم. بدون آن، درستیِ این خط به این وابسته بود که پیاده‌سازی پل
  // هنگام {@code refresh()} مقدار {@code getAccessToken()} را هم عوض کند —
  // یک قرارداد نانوشته که با یک پل دیگر (یا یک آزمون با پل ثابت) بی‌صدا
  // می‌شکند: درخواست دوباره با توکن <i>قدیمی</i> می‌رفت و کاربر با اینکه
  // تازه‌سازی موفق بوده، ۴۰۱ می‌گرفت.
  const retried = await rawFetch(path, options, token);

  if (!retried.ok) {
    const retriedBody = await readJsonSafely(retried);

    if (retried.status === 401) {
      authBridge?.onSessionLost();
    }

    throw toApiErrorFromBody(retried.status, retriedBody);
  }

  return retried;
}

/**
 * تازه‌سازی با محافظ تک‌پروازی.
 *
 * <p>⚠️ چرا لازم است؟ اگر صفحه پنج درخواست هم‌زمان بفرستد و همه ۴۰۱
 * بگیرند، بدون این محافظ <b>پنج</b> تازه‌سازی هم‌زمان می‌رود. سرور
 * توکن تازه‌سازی را یک‌بارمصرف می‌بیند و چهار درخواست دیگر را «استفاده
 * مجدد» تشخیص می‌دهد — که بک‌اند آن را یک رویداد امنیتی می‌داند
 * ({@code TOKEN_REUSE_DETECTED}) و <b>کل نشست را باطل می‌کند</b>.
 * یعنی بدون این محافظ، یک صفحه شلوغ خودش نشست کاربر را می‌کشد.
 */
function refreshOnce(): Promise<string | null> {
  if (refreshInFlight) {
    return refreshInFlight;
  }

  const bridge = authBridge;

  if (!bridge) {
    return Promise.resolve(null);
  }

  refreshInFlight = bridge.refresh().finally(() => {
    refreshInFlight = null;
  });

  return refreshInFlight;
}

/**
 * درخواست خام.
 *
 * @param explicitToken توکن صریح — فقط در تلاش دوباره پس از تازه‌سازی.
 *        وقتی داده می‌شود، به حال پل نگاه نمی‌کنیم.
 */
async function rawFetch(
  path: string,
  options: SendOptions,
  explicitToken?: string,
): Promise<Response> {
  const fetchImpl = options.fetchImpl ?? globalThis.fetch;

  const headers = new Headers({ Accept: 'application/json' });

  if (!options.skipAuth) {
    const token = explicitToken ?? authBridge?.getAccessToken() ?? null;

    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }
  }

  const hasBody = options.body !== undefined;
  const isFormData =
    typeof FormData !== 'undefined' && options.body instanceof FormData;

  if (hasBody && !isFormData) {
    headers.set('Content-Type', 'application/json');
  }

  const controller = new AbortController();
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  try {
    return await fetchImpl(url(path), {
      method: options.method ?? 'GET',
      headers,
      ...(hasBody
        ? {
            body: isFormData
              ? (options.body as FormData)
              : JSON.stringify(options.body),
          }
        : {}),
      signal: controller.signal,
      // ⚠️ کوکی‌ها فرستاده نمی‌شوند: احراز هویت فقط با توکن است، پس
      // نیازی به CSRF و کوکی نیست.
      credentials: 'omit',
    });
  } catch (cause) {
    // ⚠️ پیام خطا هرگز URL یا توکن را تکرار نمی‌کند — پیام fetch گاهی
    // URL کامل را دارد و URL هیچ رازی ندارد، ولی عادت بدی است.
    const aborted = cause instanceof DOMException && cause.name === 'AbortError';

    throw new ApiError({
      status: 0,
      message: aborted
        ? 'پاسخی از سرور نیامد. اتصال اینترنت را بررسی کنید.'
        : 'ارتباط با سرور برقرار نشد.',
      ...(aborted ? { code: 'NETWORK_TIMEOUT' } : { code: 'NETWORK_ERROR' }),
    });
  } finally {
    clearTimeout(timer);
  }
}

function url(path: string): string {
  const base = BASE_URL.endsWith('/') ? BASE_URL.slice(0, -1) : BASE_URL;
  const suffix = path.startsWith('/') ? path : `/${path}`;

  return `${base}${suffix}`;
}

async function parseSuccess<T>(response: Response): Promise<T> {
  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();

  if (text.length === 0) {
    return undefined as T;
  }

  try {
    return JSON.parse(text) as T;
  } catch {
    throw new ApiError({
      status: response.status,
      message: 'پاسخ سرور قابل خواندن نبود.',
      code: 'MALFORMED_RESPONSE',
    });
  }
}

/** خواندن بدنه به‌صورت JSON، بدون پرتاب. */
async function readJsonSafely(response: Response): Promise<ApiErrorBody | null> {
  try {
    const text = await response.text();

    if (text.length === 0) {
      return null;
    }

    const parsed: unknown = JSON.parse(text);

    // ⚠️ اینجا فقط «شیء بودن» بررسی می‌شود، نه شکل دقیق فیلدها — و این
    // عمدی است: پاسخ خطا از سروری می‌آید که ممکن است نسخه‌اش قدیمی‌تر یا
    // جدیدتر از این کلاینت باشد. اگر شکل را اکید می‌گرفتیم، یک پاسخ سالم
    // ولی ناشناخته به «بدنه نامعتبر» تبدیل می‌شد و کاربر پیام عمومی
    // می‌دید. اعتبار هر فیلد <i>هنگام خواندنش</i> بررسی می‌شود
    // (`codeOf` و `messageOf` هر کدام نوع را چک می‌کنند).
    return typeof parsed === 'object' && parsed !== null ? parsed : null;
  } catch {
    return null;
  }
}

async function toApiError(response: Response): Promise<ApiError> {
  return toApiErrorFromBody(response.status, await readJsonSafely(response));
}

function toApiErrorFromBody(status: number, body: ApiErrorBody | null): ApiError {
  const code = codeOf(body);

  return new ApiError({
    status,
    message: messageOf(body, status),
    ...(code === undefined ? {} : { code }),
    ...(typeof body?.traceId === 'string' ? { traceId: body.traceId } : {}),
    details: body?.details,
  });
}

function codeOf(body: ApiErrorBody | null): string | undefined {
  return typeof body?.code === 'string' ? body.code : undefined;
}

function messageOf(body: ApiErrorBody | null, status: number): string {
  // ⚠️ پیام بک‌اند فارسی و کاربرپسند است، پس اولویت دارد. متن‌های
  // زیر فقط برای حالتی هستند که بدنه‌ای نرسیده باشد.
  if (typeof body?.message === 'string' && body.message.length > 0) {
    return body.message;
  }

  if (status >= 500) {
    return 'خطای غیرمنتظره در سرور رخ داد. کمی بعد تلاش کنید.';
  }

  return 'درخواست انجام نشد.';
}
