import {
  Bot,
  BookOpen,
  ClipboardList,
  LayoutDashboard,
  Settings,
  type LucideIcon,
} from 'lucide-react';

/** یک زبانه در نوار پایین. */
export interface NavTab {
  /** مسیر مطلق. */
  path: string;
  /** برچسب فارسی. */
  label: string;
  icon: LucideIcon;
  /**
   * آیا برای فعال بودن، تطبیق باید دقیق باشد؟
   *
   * <p>⚠️ فقط برای زبانه خانه {@code true} است. بدون آن، مسیر {@code /}
   * پیشوند همه مسیرهای دیگر است و همه زبانه‌ها هم‌زمان فعال نشان داده
   * می‌شوند.
   */
  exact?: boolean;
}

/**
 * زبانه‌های نوار پایین — {@code docs/mini-app.md} بخش ۶.
 *
 * <p>⚠️ سند صریح می‌گوید حداکثر <b>۵</b> زبانه. دلیلش طراحی خود ایتا است:
 * نوار پایین باریک است و بیش از پنج زبانه روی گوشی‌های کوچک برچسب‌ها را
 * می‌شکند.
 *
 * <p>⚠️ این فهرست تنها منبع حقیقت است: هم {@code TabBar} و هم
 * {@code routes} از همین جا ساخته می‌شوند تا روزی یک زبانه به مسیرها
 * اضافه نشود بدون آنکه در نوار پایین بیاید.
 */
export const NAV_TABS: readonly NavTab[] = [
  { path: '/', label: 'خانه', icon: LayoutDashboard, exact: true },
  { path: '/books', label: 'کتاب‌ها', icon: BookOpen },
  { path: '/assignments', label: 'تکالیف', icon: ClipboardList },
  { path: '/assistant', label: 'دستیار', icon: Bot },
  { path: '/settings', label: 'تنظیمات', icon: Settings },
] as const;

/**
 * آیا این مسیر با این زبانه مطابقت دارد؟
 *
 * <p>⚠️ تطبیق پیشوندی عمدی است: در مسیر {@code /books/12} زبانه
 * «کتاب‌ها» باید فعال بماند. تطبیق دقیق فقط برای خانه لازم است.
 */
export function isTabActive(tab: NavTab, pathname: string): boolean {
  if (tab.exact) {
    return pathname === tab.path;
  }

  return pathname === tab.path || pathname.startsWith(`${tab.path}/`);
}
