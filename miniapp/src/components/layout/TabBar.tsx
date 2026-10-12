import { NavLink } from 'react-router-dom';
import { NAV_TABS } from '../../router/navigation';

/**
 * نوار زبانه‌های پایین.
 *
 * <p>⚠️ چرا {@code NavLink} و نه {@code Link} با منطق دستی؟ چون
 * {@code NavLink} خودش {@code aria-current="page"} را می‌گذارد و صفحه‌خوان
 * می‌فهمد کاربر کجاست. با {@code Link} باید این را دستی می‌نوشتیم و دیر یا
 * زود یکی از زبانه‌ها فراموش می‌شد.
 *
 * <p>⚠️ متن برچسب زیر آیکون است (و نه کنارش) چون عرض نوار پایین روی گوشی
 * کم است؛ در حالت افقی، «کتاب‌ها» و «تکالیف» به هم می‌چسبند.
 */
export function TabBar() {
  return (
    <nav
      aria-label="زبانه‌های اصلی"
      className="fixed inset-x-0 bottom-0 z-20 border-t border-border bg-card pb-[env(safe-area-inset-bottom)]"
    >
      <ul className="mx-auto flex max-w-lg items-stretch justify-around">
        {NAV_TABS.map((tab) => (
          <li key={tab.path} className="flex-1">
            <NavLink
              to={tab.path}
              // ⚠️ `tab.exact === true` و نه `tab.exact`: با
              // `exactOptionalPropertyTypes` مقدار `undefined` با نوع
              // `boolean` سازگار نیست، ولی اینجا نتیجه صریحاً بولین است.
              end={tab.exact === true}
              className={({ isActive }) =>
                [
                  'flex min-h-14 flex-col items-center justify-center gap-1 px-1 py-2 text-[11px] transition-colors',
                  isActive ? 'text-primary' : 'text-muted-foreground',
                ].join(' ')
              }
            >
              {({ isActive }) => (
                <>
                  <tab.icon
                    className="size-5"
                    aria-hidden="true"
                    // ⚠️ ضخامت خط، تنها نشانه فعال بودن برای کاربران
                    // کم‌بینا است — رنگ تنها کافی نیست.
                    strokeWidth={isActive ? 2.4 : 1.8}
                  />
                  <span>{tab.label}</span>
                </>
              )}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
