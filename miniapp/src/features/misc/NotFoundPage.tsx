import { Link } from 'react-router-dom';

interface NotFoundPageProps {
  message?: string;
}

/** صفحه «پیدا نشد» — هم برای مسیر ناشناخته، هم برای شناسه نامعتبر. */
export function NotFoundPage({ message }: NotFoundPageProps) {
  return (
    // ⚠️ `60dvh` و نه `min-h-dvh`: این صفحه هم داخل پوسته (مسیر ناشناخته)
    // و هم داخل صفحه جزئیات (شناسه نامعتبر) استفاده می‌شود. ارتفاع کامل
    // صفحه در حالت دوم یک ناحیه خالی بزرگ می‌سازد.
    <div className="flex min-h-[60dvh] flex-col items-center justify-center gap-3 px-6 text-center">
      <p className="text-lg font-semibold">{message ?? 'این صفحه پیدا نشد.'}</p>
      <p className="text-sm text-muted-foreground">
        ممکن است لینک قدیمی باشد یا دسترسی به آن نداشته باشید.
      </p>
      <Link to="/" className="text-sm text-primary underline underline-offset-4">
        بازگشت به خانه
      </Link>
    </div>
  );
}
