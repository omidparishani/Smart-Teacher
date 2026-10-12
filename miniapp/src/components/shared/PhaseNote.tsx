import { Construction } from 'lucide-react';

interface PhaseNoteProps {
  /** شماره فازی که این بخش را می‌سازد — مطابق `docs/roadmap.md`. */
  phase: number;
  /** چه چیزی اینجا خواهد آمد. */
  description: string;
}

/**
 * یادداشت «این بخش هنوز ساخته نشده».
 *
 * <p><b>🔴 چرا این کامپوننت وجود دارد؟</b> چون فرق است بین «فهرست خالی
 * است» و «این قابلیت هنوز ساخته نشده». اگر جای این یادداشت یک
 * {@code EmptyState} بگذاریم که می‌گوید «هنوز کتابی ثبت نشده»، ما یک
 * <i>ادعای نادرست</i> به کاربر نشان داده‌ایم: نه اینکه کتابی نیست، بلکه
 * ما هنوز بخش کتاب‌ها را ننوشته‌ایم. کاربر بعد از ثبت کتاب هم همین پیام
 * را می‌بیند و دنبال باگ می‌گردد.
 *
 * <p>⚠️ این کامپوننت باید با پیشرفت فازها <b>حذف</b> شود. اگر جایی ماند،
 * یعنی آن صفحه واقعاً پیاده نشده است.
 */
export function PhaseNote({ phase, description }: PhaseNoteProps) {
  return (
    <div className="flex items-start gap-3 rounded-xl border border-dashed border-border bg-muted/40 p-4">
      <Construction className="mt-0.5 size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
      <div className="text-sm">
        <p className="font-medium">این بخش در فاز {phase} ساخته می‌شود</p>
        <p className="mt-1 text-muted-foreground">{description}</p>
      </div>
    </div>
  );
}
