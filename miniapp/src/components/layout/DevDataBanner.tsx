import { TriangleAlert } from 'lucide-react';
import { isDevInitDataInUse } from '../../eitaa/init-data';

/**
 * بنر «حالت توسعه».
 *
 * <p>⚠️ چرا لازم است؟ چون در حالت توسعه، ورود با یک {@code initData} جعلی
 * انجام می‌شود. اگر این موضوع دیده نشود، دو اتفاق بد می‌افتد:
 * <ol>
 *   <li>کسی یک نشست توسعه را با نشست واقعی اشتباه می‌گیرد و دنبال باگی
 *       می‌گردد که وجود ندارد.</li>
 *   <li>بدتر: کسی فکر می‌کند ورود بدون ایتا کار می‌کند و همین را به
 *       Production می‌برد.</li>
 * </ol>
 *
 * <p>🔴 این بنر در باندل Production <b>هرگز</b> دیده نمی‌شود، چون
 * {@code getInitData} در Production مسیر توسعه را کامل رد می‌کند و
 * {@code isDevInitDataInUse} هیچ‌وقت {@code true} نمی‌شود.
 */
export function DevDataBanner() {
  if (!isDevInitDataInUse()) {
    return null;
  }

  return (
    <div
      role="status"
      className="flex items-center gap-2 border-b border-warning/40 bg-warning/10 px-4 py-2 text-xs text-foreground"
    >
      <TriangleAlert className="size-4 shrink-0 text-warning" aria-hidden="true" />
      <p>
        <b>حالت توسعه:</b> ورود با داده جعلی انجام می‌شود، نه با حساب واقعی ایتا.
      </p>
    </div>
  );
}
