import { QueryClient } from '@tanstack/react-query';
import { ApiError } from './api-client';

/**
 * ساخت QueryClient.
 *
 * <p>⚠️ چرا یک تابع و نه یک نمونه ثابت؟ چون آزمون‌ها هرکدام کلاینت تازه
 * می‌خواهند؛ یک نمونه ماژولی بین آزمون‌ها حافظه پنهان به اشتراک می‌گذارد و
 * آزمون‌ها به هم وابسته می‌شوند.
 *
 * <p><b>🔴 قاعده مهم: خطای ۴۰۱ نباید Retry شود.</b> اگر نشست مرده باشد،
 * تلاش دوباره فقط سه درخواست بی‌فایده می‌فرستد و — مهم‌تر — می‌تواند
 * توکن تازه‌سازی را دوباره مصرف کند که در بک‌اند
 * {@code TOKEN_REUSE_DETECTED} می‌سازد و کل نشست را می‌کشد.
 */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        // ⚠️ داده آموزشی به‌سرعت کهنه نمی‌شود؛ ۳۰ ثانیه پنجره معقولی است.
        staleTime: 30_000,
        // ⚠️ تمرکز مجدد پنجره، در WebView ایتا رویدادهای عجیبی می‌دهد.
        refetchOnWindowFocus: false,
        retry: (failureCount, error) => {
          if (error instanceof ApiError) {
            // 🔴 ۴۰۱ و ۴۰۳ هرگز — نشست یا دسترسی، با تکرار درست نمی‌شوند.
            if (error.status === 401 || error.status === 403) {
              return false;
            }

            // ⚠️ ۴xx دیگر هم خطای درخواست است؛ تکرار چیزی را عوض نمی‌کند.
            if (error.status >= 400 && error.status < 500) {
              return false;
            }
          }

          // ⚠️ خطای شبکه یا ۵xx: حداکثر دو بار.
          return failureCount < 2;
        },
      },
      mutations: {
        // ⚠️ Mutation هرگز خودکار تکرار نمی‌شود. یک ارسال دوباره می‌تواند
        // برای معلم گران تمام شود (پیام تکراری به گروه).
        retry: false,
      },
    },
  });
}
