/**
 * مسیریابی بر پایه `start_param` — تنها مسیر Deep Link واقعی در ایتا.
 *
 * <pre>
 * لینک تکلیف:  https://eitaa.com/&lt;bot&gt;?startapp=asg_&lt;uuid&gt;
 * لینک کلاس:   https://eitaa.com/&lt;bot&gt;?startapp=cls_&lt;uuid&gt;
 * لینک آزمون:  https://eitaa.com/&lt;bot&gt;?startapp=exm_&lt;uuid&gt;
 * </pre>
 *
 * <p><b>🔴 اینجا {@code start_param} داده غیرقابل‌اعتماد است.</b> هر کاربری
 * می‌تواند لینکی با هر مقدار بسازد و آن را برای معلم بفرستد. پس:
 * <ol>
 *   <li>فقط الگوی {@code type_<شناسه>} پذیرفته می‌شود — UUID یا عدد.</li>
 *   <li>مقدار <b>هرگز اجرا، تفسیر یا در مسیر فایل استفاده نمی‌شود</b>؛
 *       فقط به‌عنوان شناسه داخل یک مسیر ثابت قرار می‌گیرد.</li>
 *   <li>بک‌اند <b>مستقل</b> بررسی می‌کند که آن منبع به مستأجر جاری تعلق
 *       دارد. اعتبار این لایه هیچ تضمینی درباره دسترسی نمی‌دهد.</li>
 * </ol>
 *
 * <p>⚠️ <b>چرا یک شیء برمی‌گردانیم و نه رشته مسیر؟</b> چون اگر رشته
 * برگردانیم، فراخوان ممکن است آن را مستقیم داخل {@code navigate()} بگذارد
 * و روزی کسی بخش نااعتبارسنجی‌شده‌ای به آن اضافه کند. با شیء، نوع فیلد
 * {@code id} از قبل بررسی شده و مسیر فقط از یک تابع ثابت بیرون می‌آید.
 */

/** نقشه پیشوند → نوع مقصد. هر پیشوند ناشناخته نادیده گرفته می‌شود. */
const PREFIXES = {
  asg: 'assignment',
  cls: 'class',
  exm: 'exam',
} as const;

export type DeepLinkKind = (typeof PREFIXES)[keyof typeof PREFIXES];

/** یک مقصد Deep Link معتبر — `id` تضمیناً UUID یا عدد ASCII است. */
export interface DeepLinkTarget {
  kind: DeepLinkKind;
  id: string;
}

/**
 * سقف طول شناسه عددی.
 *
 * <p>⚠️ سقف دفاعی است، نه یک حد واقعی. شناسه‌های {@code long} حداکثر
 * ۱۹ رقم دارند. UUID جداگانه با الگوی ثابت ۳۶ کاراکتری بررسی می‌شود.
 */
const MAX_ID_DIGITS = 19;

/** UUID استاندارد — همان قالب شناسه‌های بک‌اند (v4/v7). */
const UUID_PATTERN =
  /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

/**
 * تبدیل `start_param` به مقصد قابل‌اعتماد — یا `null`.
 *
 * <p>هر مقدار نامعتبر (پیشوند ناشناخته، شناسه غیرمجاز، رشته خالی، ساختار
 * چندبخشی) به {@code null} تبدیل می‌شود. هیچ‌وقت استثنا نمی‌دهد و هیچ‌وقت
 * چیزی را اجرا نمی‌کند.
 */
export function resolveStartParam(param: string | undefined | null): DeepLinkTarget | null {
  if (typeof param !== 'string' || param.length === 0) {
    return null;
  }

  // ⚠️ دقیقاً یک زیرخط. `asg_1_2` یا `asg_1&cls_2` نامعتبر است — و
  // همین‌جاست که مقدارهای چندبخشی و مخرب رد می‌شوند.
  const parts = param.split('_');

  if (parts.length !== 2) {
    return null;
  }

  const [prefix, id] = parts;

  if (prefix === undefined || id === undefined) {
    return null;
  }

  const kind = PREFIXES[prefix as keyof typeof PREFIXES];

  if (!kind) {
    return null;
  }

  if (!isValidDeepLinkId(id)) {
    return null;
  }

  return { kind, id };
}

/**
 * آیا این رشته یک شناسه Deep Link قابل‌قبول است؟
 *
 * <p>⚠️ UUID یا عدد ASCII خالص — هیچ کاراکتر کنترلی، فاصله یا
 * {@code ../} مجاز نیست.
 */
function isValidDeepLinkId(value: string): boolean {
  if (value.length === 0) {
    return false;
  }
  if (UUID_PATTERN.test(value)) {
    return true;
  }
  return value.length <= MAX_ID_DIGITS && /^[0-9]+$/.test(value);
}

/**
 * مسیر قابل‌استفاده در React Router.
 *
 * <p>⚠️ ورودی این تابع از {@link resolveStartParam} می‌آید که {@code id}
 * آن تضمیناً UUID یا عدد است. پس مسیر ساخته‌شده نمی‌تواند شامل
 * {@code ../}، {@code ?}، {@code #} یا هیچ کاراکتر کنترلی باشد.
 */
export function deepLinkPath(target: DeepLinkTarget): string {
  switch (target.kind) {
    case 'assignment':
      return `/assignments/${target.id}`;
    case 'class':
      return `/classes/${target.id}`;
    case 'exam':
      return `/exams/${target.id}/take`;
  }
}
