import { Monitor } from 'lucide-react';
import { eitaa } from '../../eitaa/sdk';
import { PLATFORM_MESSAGES } from '../../lib/platform-guard';

/**
 * بنر «فقط اندروید و ویندوز» — الزام {@code docs/eitaa.md} بخش ۱۴.
 *
 * <p><b>🔴 چرا این بنر لازم است؟</b> ایتا برنامک‌ها را فقط روی اندروید و
 * دسکتاپ ویندوز اجرا می‌کند. این محدودیت <i>مالِ ما نیست</i> ولی کاربر
 * ماست: اگر معلمی برنامک را روی دسکتاپ باز کند و دکمه‌ای کار نکند، بدون
 * این توضیح فکر می‌کند برنامه ناقص است.
 *
 * <p>⚠️ این بنر روی <b>اندروید نشان داده نمی‌شود</b> فقط چون اضافه است؛
 * آنجا همه‌چیز پشتیبانی می‌شود و بنر فقط فضای مفید صفحه را می‌گیرد.
 *
 * <p>⚠️ متن عمداً «ممکن است» دارد. ما <i>نمی‌دانیم</i> کدام قابلیت روی
 * کدام نسخه دسکتاپ کار می‌کند — این یکی از موارد باز بخش ۱۴ است. ادعای
 * قطعی درباره‌ی چیزی که تست نکرده‌ایم، همان حدس زدن است که ممنوع است.
 */
export function PlatformNotice() {
  const app = eitaa.app;

  // بیرون از ایتا این بنر بی‌معنی است — آن حالت صفحه کامل مسدود دارد.
  if (!app) {
    return null;
  }

  const platform = eitaa.platform();

  if (platform.toLowerCase().includes('android')) {
    return null;
  }

  return (
    <div className="flex items-start gap-2 border-b border-border bg-muted px-4 py-2 text-xs text-muted-foreground">
      <Monitor className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
      <p>
        برنامک‌های ایتا فقط روی <b>اندروید</b> و <b>دسکتاپ ویندوز</b> اجرا می‌شوند.
        {' '}
        {platform.toLowerCase().includes('ios')
          ? PLATFORM_MESSAGES.ios
          : 'روی این دستگاه، برخی امکانات وابسته به نسخه ایتا ممکن است در دسترس نباشند.'}
      </p>
    </div>
  );
}
