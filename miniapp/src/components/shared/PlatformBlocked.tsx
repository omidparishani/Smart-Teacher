import { Smartphone } from 'lucide-react';
import { PLATFORM_MESSAGES } from '../../lib/platform-guard';

interface PlatformBlockedProps {
  message: string;
}

/**
 * صفحه «پلتفرم پشتیبانی نمی‌شود».
 *
 * <p><b>🔴 این صفحه یک الزام است، نه یک بهبود.</b> ایتا برنامک‌ها را فقط
 * روی اندروید و ویندوز اجرا می‌کند؛ iOS پشتیبانی نمی‌شود. بدون این صفحه،
 * کاربر iOS یا کسی که لینک را در مرورگر باز کرده، یک صفحه سفید می‌بیند و
 * فکر می‌کند برنامه خراب است — و این دقیقاً چیزی است که
 * {@code docs/mini-app.md} بخش ۴.۵ می‌گوید نباید رخ دهد.
 *
 * <p>⚠️ هیچ دکمه «تلاش مجدد» وجود ندارد: دوباره زدن همان دکمه هیچ‌چیز را
 * عوض نمی‌کند چون مشکل در محیط کاربر است، نه در برنامه. دکمه‌ای که هیچ
 * کاری نمی‌کند بدتر از نبودنش است.
 */
export function PlatformBlocked({ message }: PlatformBlockedProps) {
  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-4 px-6 text-center">
      <div className="flex size-16 items-center justify-center rounded-full bg-muted">
        <Smartphone className="size-8 text-muted-foreground" aria-hidden="true" />
      </div>

      <h1 className="text-lg font-semibold">{message}</h1>

      <p className="max-w-sm text-sm text-muted-foreground">
        {message === PLATFORM_MESSAGES.ios
          ? 'روی گوشی اندروید یا نسخه دسکتاپ ویندوز ایتا، برنامه کامل کار می‌کند.'
          : 'برای استفاده از دستیار هوشمند معلم، لینک را داخل ایتا باز کنید.'}
      </p>
    </div>
  );
}
