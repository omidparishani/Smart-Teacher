/**
 * کدهای خطای بک‌اند — آینه دقیق `ErrorCode.java`.
 *
 * <p>⚠️ <b>چرا این فایل دستی نگه داشته می‌شود و از بک‌اند تولید نمی‌شود؟</b>
 * چون تولید خودکار به یک مرحله بیلد اضافه نیاز دارد که هنوز وجود ندارد و
 * ساختنش کار فاز ۱۲ است. تا آن‌وقت، این فایل یک <i>کپی صریح</i> است.
 *
 * <p>🔴 <b>ریسک شناخته‌شده:</b> اگر بک‌اند کدی اضافه کند و اینجا نیاید،
 * کلاینت آن خطا را در شاخه {@code default} می‌اندازد و پیام عمومی نشان
 * می‌دهد — یعنی <b>بی‌صدا</b> یک تجربه ناقص، نه یک خطا. برای همین، فیلد
 * کد روی سیم {@code string} است و نه این اتحاد
 * ({@code src/types/api.ts}) — ولی {@link isKnownErrorCode} اجازه می‌دهد
 * هرجا لازم بود به این اتحاد محدود شویم.
 *
 * <p>منبع: {@code backend/src/main/java/ir/omid/smartteacher/common/error/ErrorCode.java}
 * — ۲۵ کد، شمرده‌شده و نه حدس‌زده‌شده.
 */
export const ERROR_CODES = [
  // ── ۴۰۰ ──
  'VALIDATION_FAILED',
  'INVALID_REQUEST',
  // ── ۴۰۱ ──
  'UNAUTHENTICATED',
  'INVALID_CREDENTIALS',
  'TOKEN_EXPIRED',
  'TOKEN_REUSE_DETECTED',
  'EITAA_INIT_DATA_INVALID',
  // ── ۴۰۳ ──
  'ACCESS_DENIED',
  'FEATURE_NOT_ENTITLED',
  'SUBSCRIPTION_EXPIRED',
  'ACCOUNT_DISABLED',
  // ── ۴۰۴ ──
  'RESOURCE_NOT_FOUND',
  'ENDPOINT_NOT_FOUND',
  // ── ۴۰۹ ──
  'RESOURCE_CONFLICT',
  'DUPLICATE_RESOURCE',
  'COUPON_INVALID',
  // ── ۴۲۹ ──
  'USAGE_LIMIT_REACHED',
  'AI_USAGE_LIMIT_REACHED',
  'RATE_LIMIT_EXCEEDED',
  // ── ۵۰۱ / ۵۰۲ ──
  'EITAA_CAPABILITY_NOT_SUPPORTED',
  'AI_PROVIDER_UNAVAILABLE',
  'AI_STRUCTURED_OUTPUT_FAILED',
  'PAYMENT_GATEWAY_ERROR',
  'EITAA_API_ERROR',
  // ── ۵۰۰ ──
  'INTERNAL_ERROR',
] as const;

/** یکی از ۲۵ کد شناخته‌شده. */
export type ApiErrorCode = (typeof ERROR_CODES)[number];

/** آیا این رشته یکی از کدهای شناخته‌شده است؟ */
export function isKnownErrorCode(code: string | undefined): code is ApiErrorCode {
  return typeof code === 'string' && (ERROR_CODES as readonly string[]).includes(code);
}

/**
 * کدهایی که یعنی «نشست مرده — باید دوباره وارد شوی».
 *
 * <p>⚠️ این فهرست عمدی و کوتاه است. {@code ACCESS_DENIED} (۴۰۳) اینجا
 * <b>نیست</b>: آن یعنی «وارد شده‌ای ولی اجازه نداری» و تازه‌سازی توکن
 * هیچ‌چیز را عوض نمی‌کند. گذاشتنش اینجا یعنی یک حلقه بی‌فایده تازه‌سازی
 * برای هر درخواست بی‌اجازه.
 */
export const SESSION_DEAD_CODES: readonly string[] = [
  'UNAUTHENTICATED',
  'TOKEN_EXPIRED',
  'TOKEN_REUSE_DETECTED',
  /** امضای initData باطل شده — تنها راه، ورود دوباره است. */
  'EITAA_INIT_DATA_INVALID',
];

/** آیا این کد یعنی نشست مرده است؟ */
export function isSessionDeadCode(code: string | undefined): boolean {
  return typeof code === 'string' && SESSION_DEAD_CODES.includes(code);
}

/**
 * پیام فارسی برای کدهای مهم.
 *
 * <p>⚠️ بک‌اند هم {@code message} فارسی می‌فرستد و در حالت عادی همان
 * نمایش داده می‌شود. این نقشه فقط برای جاهایی است که UI باید متن
 * <i>خودش</i> را بگوید — مثلاً وقتی خطای شبکه رخ داده و اصلاً بدنه‌ای
 * نرسیده، یا وقتی می‌خواهیم دکمه «ارتقای پلن» را به یک خطای خاص بچسبانیم
 * ({@code docs/mini-app.md} بخش ۱۱).
 */
export const ERROR_CODE_MESSAGES: Partial<Record<ApiErrorCode, string>> = {
  RATE_LIMIT_EXCEEDED: 'تعداد درخواست‌های شما بیش از حد مجاز است. کمی بعد تلاش کنید.',
  USAGE_LIMIT_REACHED: 'سقف استفاده از این قابلیت به پایان رسیده است.',
  AI_USAGE_LIMIT_REACHED: 'سقف استفاده از هوش مصنوعی این پلن به پایان رسیده است.',
  AI_PROVIDER_UNAVAILABLE: 'سرویس هوش مصنوعی موقتاً در دسترس نیست.',
  SUBSCRIPTION_EXPIRED: 'اشتراک شما منقضی شده است.',
  EITAA_CAPABILITY_NOT_SUPPORTED: 'این قابلیت در ایتا وجود ندارد.',
};

/** کدهایی که در {@code docs/mini-app.md} بخش ۱۱ دکمه «ارتقای پلن» می‌خواهند. */
export const UPGRADE_PROMPT_CODES: readonly string[] = [
  'USAGE_LIMIT_REACHED',
  'AI_USAGE_LIMIT_REACHED',
  'FEATURE_NOT_ENTITLED',
  'SUBSCRIPTION_EXPIRED',
];
