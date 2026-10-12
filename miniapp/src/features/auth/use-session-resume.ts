import { useEffect } from 'react';
import { useAuthStore } from '../../stores/auth.store';

/**
 * فاصله کمینه بین دو تازه‌سازی هنگام بازگشت.
 *
 * <p>⚠️ چرا لازم است؟ سه رویداد جدا ({@code visibilitychange}،
 * {@code pageshow}، {@code focus}) روی یک بازگشت واحد شلیک می‌شوند.
 * هر تازه‌سازی، توکن تازه‌سازی را <b>می‌چرخاند</b> و توکن قبلی را
 * می‌سوزاند؛ پس بدون این فاصله، یک بازگشت می‌توانست سه چرخش پشت‌سرهم
 * بسازد. محافظ تک‌پروازیِ خودِ {@code refresh()} جلوی هم‌زمانی را
 * می‌گیرد، ولی چرخش‌های <i>پشت‌سرهم</i> را نه.
 */
const MIN_INTERVAL_MS = 30_000;

/**
 * تازه‌سازی نشست وقتی اپ از پس‌زمینه برمی‌گردد.
 *
 * <p>🔴 <b>چرا لازم است؟</b> توکن دسترسی ۱۵ دقیقه اعتبار دارد. معلمی که
 * اپ را نیم‌ساعت کنار می‌گذارد و برمی‌گردد، بدون این هوک با یک توکن
 * منقضی به اولین صفحه می‌رسد: چند درخواست ۴۰۱ می‌گیرند و او یک صفحه
 * خطا می‌بیند — در حالی که توکن تازه‌سازی ۳۰روزه‌اش کاملاً سالم بوده.
 *
 * <p>⚠️ سه رویداد گوش داده می‌شود و نه یکی، چون WebViewهای مختلف رفتار
 * متفاوتی دارند: اندروید {@code visibilitychange} می‌دهد، برخی نسخه‌ها
 * فقط {@code pageshow} (بازیابی از bfcache)، و دسکتاپ {@code focus}.
 * گرفتن هر سه، هیچ‌کدام را از دست نمی‌دهد و تکرارشان با فاصله کمینه
 * مهار شده است.
 *
 * <p>⚠️ تازه‌سازی هیچ‌وقت هنگام <b>رفتن</b> به پس‌زمینه اجرا نمی‌شود:
 * آنجا مرورگر ممکن است درخواست را نیمه‌کاره بکشد و توکن چرخیده ولی
 * پاسخ هرگز ذخیره نشده باشد — یعنی سوختن یک توکن بدون جایگزین.
 */
export function useSessionResume(): void {
  const refreshOnResume = useAuthStore((state) => state.refreshOnResume);

  useEffect(() => {
    let lastRunAt = 0;

    const resume = () => {
      // ⚠️ `hidden` یعنی داریم می‌رویم، نه برمی‌گردیم.
      if (document.visibilityState === 'hidden') {
        return;
      }

      const now = Date.now();

      if (now - lastRunAt < MIN_INTERVAL_MS) {
        return;
      }

      lastRunAt = now;

      // ⚠️ `void` صریح: این تابع هیچ‌وقت پرتاب نمی‌کند و کسی هم منتظر
      // نتیجه‌اش نیست — رها کردنش یک Floating Promise است.
      void refreshOnResume();
    };

    document.addEventListener('visibilitychange', resume);
    window.addEventListener('pageshow', resume);
    window.addEventListener('focus', resume);

    return () => {
      document.removeEventListener('visibilitychange', resume);
      window.removeEventListener('pageshow', resume);
      window.removeEventListener('focus', resume);
    };
  }, [refreshOnResume]);
}
