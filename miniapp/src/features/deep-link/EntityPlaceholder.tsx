import { useParams } from 'react-router-dom';
import { PageHeader } from '../../components/shared/PageHeader';
import { PhaseNote } from '../../components/shared/PhaseNote';
import { NotFoundPage } from '../misc/NotFoundPage';

interface EntityPlaceholderProps {
  title: string;
  /** نام فارسی نوع منبع — برای پیام خطا. */
  entityLabel: string;
  phase: number;
  description: string;
}

/**
 * مقصد Deep Link — پوسته صفحه جزئیات.
 *
 * <p><b>🔴 چرا شناسه دوباره اینجا اعتبارسنجی می‌شود؟</b> چون
 * {@code useStartParamRedirect} <i>یکی</i> از راه‌های رسیدن به این صفحه
 * است. کاربر می‌تواند مسیر را دستی هم بزند. اعتبارسنجی در نقطه ورود کافی
 * نیست؛ هر نقطه‌ای که با داده کاربر کار می‌کند باید خودش بررسی کند — همان
 * اصلی که در بک‌اند با {@code TenantGuard} اجرا می‌شود.
 *
 * <p>⚠️ حتی با شناسه معتبر، این صفحه <b>هیچ داده‌ای از سرور نمی‌گیرد</b>،
 * چون آن API هنوز وجود ندارد. اگر شناسه را بدون درخواست نشان دهیم درست
 * است (چیزی ادعا نکرده‌ایم)، ولی اگر داده‌ای جعل کنیم نه.
 */
export function EntityPlaceholder({
  title,
  entityLabel,
  phase,
  description,
}: EntityPlaceholderProps) {
  const { id } = useParams<{ id: string }>();

  // ⚠️ همان الگوی start-param: UUID یا عدد. `Number()` اینجا کافی نیست چون
  // `"1e5"` و `" 12 "` را هم قبول می‌کرد.
  if (
    !id ||
    (!/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(
      id,
    ) &&
      !/^[0-9]{1,19}$/.test(id))
  ) {
    return <NotFoundPage message={`${entityLabel} خواسته‌شده پیدا نشد.`} />;
  }

  return (
    <>
      <PageHeader title={title} description={`شناسه: ${id}`} />

      <PhaseNote phase={phase} description={description} />
    </>
  );
}
