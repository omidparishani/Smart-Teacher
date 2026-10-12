interface LoadingSkeletonProps {
  /** تعداد سطرهای نمایشی. */
  lines?: number;
  /** برچسب دسترس‌پذیری — صفحه‌خوان باید بداند مشغول بارگذاری است. */
  label?: string;
  className?: string;
}

/**
 * اسکلت بارگذاری — {@code docs/mini-app.md} بخش ۱۱.
 *
 * <p>⚠️ سند صریح می‌گوید <b>Skeleton و نه Spinner تمام‌صفحه</b>. دلیلش
 * تجربه کاربر است: اسکلت، شکل محتوای آینده را نشان می‌دهد و پرش چیدمان
 * را کم می‌کند؛ Spinner فقط می‌گوید «منتظر باش» بدون اینکه بگوید چه
 * چیزی می‌آید.
 *
 * <p>⚠️ {@code aria-busy} و {@code role="status"} عمدی‌اند: بدون آن‌ها،
 * کاربر صفحه‌خوان هیچ اطلاعی از وضعیت بارگذاری نمی‌گیرد و صفحه بی‌صدا
 * خالی به نظر می‌رسد.
 */
export function LoadingSkeleton({ lines = 3, label = 'در حال بارگذاری', className }: LoadingSkeletonProps) {
  return (
    <div
      role="status"
      aria-busy="true"
      aria-label={label}
      className={`flex flex-col gap-3 ${className ?? ''}`}
    >
      {Array.from({ length: lines }, (_, index) => (
        <div
          key={index}
          className="h-4 animate-pulse rounded-md bg-muted"
          // ⚠️ آخرین سطر کوتاه‌تر — تا شبیه متن باشد، نه جدول.
          style={{ width: index === lines - 1 ? '60%' : '100%' }}
        />
      ))}
    </div>
  );
}
