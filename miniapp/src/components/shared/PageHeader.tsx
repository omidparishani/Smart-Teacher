import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: string;
  description?: string;
  action?: ReactNode;
}

/**
 * سرصفحه صفحه.
 *
 * <p>⚠️ هدینگ {@code h1} است و نه {@code div} با فونت درشت: صفحه‌خوان‌ها
 * از روی ساختار هدینگ می‌جهند و یک صفحه بدون {@code h1} برای کاربر
 * صفحه‌خوان فهرست ناوبری ندارد.
 */
export function PageHeader({ title, description, action }: PageHeaderProps) {
  return (
    <header className="mb-4 flex items-start justify-between gap-3">
      <div>
        <h1 className="text-xl font-semibold">{title}</h1>
        {description ? (
          <p className="mt-1 text-sm text-muted-foreground">{description}</p>
        ) : null}
      </div>
      {action ? <div className="shrink-0">{action}</div> : null}
    </header>
  );
}
