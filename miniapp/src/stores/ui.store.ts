import { create } from 'zustand';
import { eitaa } from '../eitaa/sdk';

/** تم فعال. */
export type Theme = 'light' | 'dark';

/** تشخیص تم — ابتدا ایتا، بعد ترجیح سیستمعامل. */
export function detectTheme(): Theme {
  // ⚠️ اول ایتا: رنگ‌بندی خود اپ منبع حقیقت است. اگر کاربر ایتا را
  // روشن کرده، برنامک هم باید روشن باشد — نه برعکس.
  if (eitaa.app) {
    return eitaa.colorScheme();
  }

  try {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  } catch {
    // ⚠️ matchMedia در همه WebViewها نیست. روشن، پیش‌فرض بی‌خطر است.
    return 'light';
  }
}

/** اعمال تم روی ریشه سند — CSS با صفت `data-theme` کار می‌کند. */
export function applyTheme(theme: Theme): void {
  try {
    document.documentElement.dataset.theme = theme;
  } catch {
    // ⚠️ عمداً: نبود DOM نباید برنامه را بشکند (آزمون یا رندر سمت سرور).
  }
}

interface UiState {
  theme: Theme;
  setTheme: (theme: Theme) => void;
  toggleTheme: () => void;
}

/**
 * وضعیت UI — تم.
 *
 * <p>⚠️ <b>تم عمداً persist نمی‌شود.</b> سند بخش ۹ می‌گوید انتخاب کاربر
 * باید در <b>پروفایل سرور</b> ذخیره شود تا بین دستگاه‌ها هماهنگ باشد،
 * نه در {@code localStorage}. تا فاز ۴ که پروفایل سرور ساخته شود، تم در
 * هر بارگذاری از ایتا تشخیص داده می‌شود — که رفتار درستی هم هست.
 *
 * <p>⚠️ فیلدهای {@code devBannerVisible} و {@code showDevBanner} حذف
 * شدند: {@code DevDataBanner} وضعیت خودش را مستقیم از
 * {@code isDevInitDataInUse()} می‌گیرد. دو منبع حقیقت برای یک چیز، یعنی
 * روزی یکی از آن دو اشتباه می‌گوید.
 */
export const useUiStore = create<UiState>()((set, get) => ({
  theme: 'light',

  setTheme: (theme) => {
    applyTheme(theme);
    set({ theme });
  },

  toggleTheme: () => {
    const next: Theme = get().theme === 'dark' ? 'light' : 'dark';
    applyTheme(next);
    set({ theme: next });
  },
}));
