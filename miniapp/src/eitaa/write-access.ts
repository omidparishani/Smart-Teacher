import { eitaa } from './sdk';
import type { EitaaWebAppApi } from './types';

/** نتیجه درخواست اجازه ارسال — سه حالت، نه دو تا. */
export type WriteAccessResult =
  /** از قبل دارد. */
  | 'already-granted'
  /** کاربر همین حالا اجازه داد. */
  | 'granted'
  /** کاربر رد کرد. */
  | 'denied'
  /** نسخه SDK این متد را ندارد (یا پاسخ نداد). */
  | 'unsupported';

/**
 * مهلت انتظار برای پاسخ کاربر.
 *
 * <p>⚠️ <b>چرا مهلت لازم است؟</b> چون رفتار {@code requestWriteAccess} در
 * نسخه دسکتاپ ویندوز <b>راستی‌آزمایی نشده</b> است ({@code docs/eitaa.md}
 * بخش ۳.۵ و {@code docs/verification-log.md} بخش ۲). اگر آن نسخه کال‌بک
 * را هرگز صدا نزند، بدون مهلت این Promise تا ابد معلق می‌ماند و صفحه ورود
 * <b>بی‌صدا</b> قفل می‌شود — کاربر منتظر می‌ماند و هیچ خطایی هم دیده نمی‌شود.
 *
 * <p>با مهلت، بدترین حالت یک نتیجه {@code 'unsupported'} است: برنامه جلو
 * می‌رود و اعلان‌ها فقط درون‌برنامه‌ای می‌شوند. این دقیقاً سیاست محصولی
 * شماره ۳ است، پس مهلت چیزی را خراب نمی‌کند.
 */
const RESPONSE_TIMEOUT_MS = 10_000;

let askedThisSession = false;

/**
 * اجازه ارسال پیام به کاربر — {@code docs/mini-app.md} بخش ۴.۶.
 *
 * <p>سیاست محصولی که این تابع پیاده می‌کند:
 * <ol>
 *   <li>یک‌بار در هر نشست پرسیده می‌شود — با توضیح فارسی روشن.</li>
 *   <li>اگر کاربر رد کرد، <b>هرگز</b> دوباره پرسیده نمی‌شود.</li>
 *   <li>در آن حالت اعلان‌ها فقط داخل Mini App دیده می‌شوند.</li>
 *   <li>وضعیت در پروفایل سرور ذخیره می‌شود، نه {@code localStorage}.</li>
 * </ol>
 *
 * <p>⚠️ چرا در {@code localStorage} ذخیره نمی‌شود؟ چون آن یک تصمیم
 * <i>سراسری حساب</i> است، نه دستگاه. اگر روی گوشی رد شود و روی دسکتاپ
 * ذخیره، دو رفتار متناقض می‌گیریم — و کاربر هم نمی‌فهمد چرا.
 *
 * <p>⚠️ و چرا «یک‌بار در هر نشست» و نه «یک‌بار برای همیشه»؟ چون «هرگز
 * دوباره نپرس» یک تصمیم محصولی دربارهٔ <i>یک پاسخ</i> است، نه یک قفل
 * دائمی: اگر اجازه از سمت ایتا پس گرفته شود، راهی برای پرسیدن دوباره
 * باید بماند.
 */
export async function ensureWriteAccess(): Promise<WriteAccessResult> {
  const app = eitaa.app;

  if (!app) {
    return 'unsupported';
  }

  // ۱) از قبل دارد — در همان initData اعلام شده.
  if (app.initDataUnsafe?.user?.allows_write_to_pm === true) {
    return 'already-granted';
  }

  // ۲) در همین نشست قبلاً پرسیده‌ایم — هرگز دوباره نمی‌پرسیم.
  if (askedThisSession) {
    return 'denied';
  }

  if (typeof app.requestWriteAccess !== 'function') {
    return 'unsupported';
  }

  askedThisSession = true;

  return askUser(app);
}

/** فقط برای آزمون — وضعیت «در این نشست پرسیده شد» را پاک می‌کند. */
export function resetWriteAccessPromptForTest(): void {
  askedThisSession = false;
}

function askUser(app: EitaaWebAppApi): Promise<WriteAccessResult> {
  return new Promise<WriteAccessResult>((resolve) => {
    let settled = false;

    const finish = (result: WriteAccessResult) => {
      // ⚠️ محافظ دو-بار-حل: اگر کال‌بک دیر برسد (بعد از مهلت)، نباید
      // نتیجه‌ای را عوض کند که UI روی آن تصمیم گرفته است.
      if (settled) {
        return;
      }
      settled = true;
      clearTimeout(timer);
      resolve(result);
    };

    const timer = setTimeout(() => finish('unsupported'), RESPONSE_TIMEOUT_MS);

    try {
      app.requestWriteAccess((granted) => finish(granted ? 'granted' : 'denied'));
    } catch {
      // ⚠️ SDK در نسخه‌های قدیمی ممکن است پرتاب کند. «نامعلوم» یعنی
      // نتیجه‌اش را نامساعد فرض کن: اعلان درون‌برنامه‌ای، بدون مزاحمت.
      finish('unsupported');
    }
  });
}
