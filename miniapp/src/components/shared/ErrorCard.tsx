import type { ReactNode } from 'react';

interface ErrorCardProps {
  title: string;
  message: string;
  /** دکمه اقدام — مثلاً «تلاش مجدد». */
  action?: ReactNode;
}

/**
 * کارت خطا.
 *
 * <p>⚠️ چرا پیام جدا از عنوان؟ چون عنوان می‌گوید <i>چه اتفاقی افتاد</i> و
 * پیام می‌گوید <i>حالا چه کار کنم</i>. اگر یکی شوند، کاربر فقط می‌فهمد
 * خراب شده، نه اینکه چه کند.
 *
 * <p>{@code role="alert"} عمدی است تا صفحه‌خوان خطا را فوراً اعلام کند.
 *
 * <p>⚠️ عنوان یک {@code h2} است و نه {@code p}: در صفحه‌هایی که همین کارت
 * کل محتواست (مثل شکست ورود) هیچ سرتیتر دیگری وجود ندارد و صفحه‌خوان
 * راهی برای پرش به «موضوع صفحه» پیدا نمی‌کند. اندازه و ضخامت ظاهری با
 * کلاس‌ها تعیین می‌شود، پس این تغییر ظاهر را عوض نمی‌کند.
 */
export function ErrorCard({ title, message, action }: ErrorCardProps) {
  return (
    <div
      role="alert"
      className="flex flex-col items-start gap-2 rounded-xl border border-danger/40 bg-danger/5 p-4"
    >
      <h2 className="text-base font-medium text-danger-strong">{title}</h2>
      <p className="text-sm text-foreground">{message}</p>
      {action ? <div className="pt-1">{action}</div> : null}
    </div>
  );
}
