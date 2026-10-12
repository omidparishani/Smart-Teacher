import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { applyTheme, detectTheme, useUiStore } from './ui.store';
import { installFakeEitaa } from '../test/fake-eitaa';

/**
 * آزمون وضعیت UI — تشخیص و اعمال تم.
 *
 * <p>🔴 <b>چرا این فایل اضافه شد؟</b> چون در بازبینی همین فاز معلوم شد
 * {@code detectTheme} نوشته شده بود ولی <b>هیچ‌جا صدا زده نمی‌شد</b>: صفحه
 * ورود به‌جای آن وضعیت اولیه فروشگاه را می‌خواند که همیشه {@code light}
 * است. نتیجه: تنظیم رنگ ایتا کاملاً نادیده می‌ماند و هیچ آزمونی هم آن را
 * نمی‌گرفت، چون تابعِ درست آزموده می‌شد و <i>مسیر استفاده‌اش</i> نه.
 *
 * <p>⚠️ آزمون رگرسیونِ خود نقص در {@code LoginGate.test.tsx} است — اینجا
 * فقط واحدها سنجیده می‌شوند.
 */

/** جعل {@code matchMedia} با نتیجه دلخواه. */
function stubMatchMedia(matches: boolean): void {
  vi.stubGlobal(
    'matchMedia',
    vi.fn((query: string) => ({
      matches,
      media: query,
      onchange: null,
      addListener: () => undefined,
      removeListener: () => undefined,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
      dispatchEvent: () => false,
    })),
  );
}

beforeEach(() => {
  useUiStore.setState({ theme: 'light' });
  delete document.documentElement.dataset.theme;
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('detectTheme', () => {
  it('🔴 رنگ‌بندی ایتا بر ترجیح سیستم‌عامل مقدم است', () => {
    installFakeEitaa({ colorScheme: 'dark' });
    // ⚠️ سیستم‌عامل روشن است، ولی ایتا تیره — ایتا باید ببرد.
    stubMatchMedia(false);

    expect(detectTheme()).toBe('dark');
  });

  it('بدون ایتا، ترجیح سیستم‌عامل خوانده می‌شود', () => {
    stubMatchMedia(true);

    expect(detectTheme()).toBe('dark');
  });

  it('اگر matchMedia نبود، روشن برگردانده می‌شود', () => {
    vi.stubGlobal(
      'matchMedia',
      vi.fn(() => {
        throw new Error('در این WebView نیست');
      }),
    );

    // ⚠️ پرتاب نکردن مهم است: نبود matchMedia نباید صفحه سفید بسازد.
    expect(detectTheme()).toBe('light');
  });
});

describe('applyTheme', () => {
  it('صفت data-theme را روی ریشه سند می‌گذارد', () => {
    applyTheme('dark');

    // 🔴 CSS فقط با همین صفت کار می‌کند (index.css)، پس اگر اینجا
    // ننشیند، تم هیچ اثری ندارد.
    expect(document.documentElement.dataset.theme).toBe('dark');
  });
});

describe('setTheme و toggleTheme', () => {
  it('setTheme هم اعمال می‌کند و هم ذخیره', () => {
    useUiStore.getState().setTheme('dark');

    expect(useUiStore.getState().theme).toBe('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
  });

  it('toggleTheme برمی‌گرداند و اعمال می‌کند', () => {
    useUiStore.getState().toggleTheme();

    expect(useUiStore.getState().theme).toBe('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');

    useUiStore.getState().toggleTheme();

    expect(useUiStore.getState().theme).toBe('light');
    expect(document.documentElement.dataset.theme).toBe('light');
  });
});
