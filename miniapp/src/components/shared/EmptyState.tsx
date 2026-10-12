import type { ReactNode } from 'react';

interface EmptyStateProps {
  title: string;
  description: string;
  action?: ReactNode;
  icon?: ReactNode;
}

/**
 * حالت خالی — {@code docs/mini-app.md} بخش ۱۱: «تصویر + متن راهنما + دکمه اقدام».
 *
 * <p>⚠️ چرا هیچ‌وقت فهرست خالی بدون توضیح رها نمی‌شود؟ چون «هیچ چیزی
 * نیست» و «چیزی بارگذاری نشده» از هم تشخیص داده نمی‌شوند و کاربر فکر
 * می‌کند برنامه خراب است.
 */
export function EmptyState({ title, description, action, icon }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 rounded-xl border border-dashed border-border px-6 py-12 text-center">
      {icon ? <div className="text-muted-foreground">{icon}</div> : null}
      <p className="text-base font-medium">{title}</p>
      <p className="max-w-xs text-sm text-muted-foreground">{description}</p>
      {action ? <div className="pt-2">{action}</div> : null}
    </div>
  );
}
