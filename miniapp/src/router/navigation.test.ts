import { describe, expect, it } from 'vitest';
import { isTabActive, NAV_TABS } from './navigation';

/**
 * آزمون وضعیت فعال زبانه‌ها.
 *
 * <p>⚠️ باگ واقعی که این آزمون‌ها می‌گیرند: اگر «خانه» تطبیق دقیق نداشته
 * باشد، مسیر {@code /} پیشوند همه مسیرهای دیگر است و همه پنج زبانه
 * هم‌زمان فعال نشان داده می‌شوند.
 */
describe('isTabActive', () => {
  const home = NAV_TABS[0]!;
  const books = NAV_TABS[1]!;
  const assignments = NAV_TABS[2]!;

  it('خانه فقط در مسیر / فعال است', () => {
    expect(isTabActive(home, '/')).toBe(true);
    expect(isTabActive(home, '/books')).toBe(false);
  });

  it('در زیرمسیر، زبانه والد فعال می‌ماند', () => {
    expect(isTabActive(books, '/books/12')).toBe(true);
    expect(isTabActive(assignments, '/assignments/12')).toBe(true);
  });

  it('پیشوند ناقص، زبانه را فعال نمی‌کند', () => {
    // ⚠️ بدون بررسی جداکننده، `/bookshelf` زبانه «کتاب‌ها» را فعال
    // می‌کرد — یک اشتباه رایج در تطبیق پیشوندی.
    expect(isTabActive(books, '/bookshelf')).toBe(false);
  });

  it('مسیر نامرتبط هیچ زبانه‌ای را فعال نمی‌کند', () => {
    for (const tab of NAV_TABS) {
      expect(isTabActive(tab, '/classes/5')).toBe(false);
    }
  });

  it('فهرست زبانه‌ها سقف پنج‌تایی سند را رعایت می‌کند', () => {
    // ⚠️ سند بخش ۶ صریح می‌گوید حداکثر ۵ زبانه؛ بیشتر از آن برچسب‌ها
    // روی گوشی‌های کوچک می‌شکنند.
    expect(NAV_TABS.length).toBeLessThanOrEqual(5);
  });

  it('مسیرها یکتا و مطلق‌اند', () => {
    const paths = NAV_TABS.map((tab) => tab.path);

    expect(new Set(paths).size).toBe(paths.length);

    for (const path of paths) {
      expect(path.startsWith('/')).toBe(true);
    }
  });
});
