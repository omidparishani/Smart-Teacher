import type { ButtonHTMLAttributes, ReactNode } from 'react';

type Variant = 'primary' | 'secondary' | 'ghost';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  children: ReactNode;
}

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-primary text-white hover:bg-primary/90',
  secondary: 'bg-muted text-foreground hover:bg-muted/80',
  ghost: 'bg-transparent text-primary hover:bg-primary/10',
};

/**
 * دکمه پایه.
 *
 * <p>⚠️ {@code disabled:opacity-50} و {@code disabled:pointer-events-none}
 * با هم می‌آیند: شفافیت تنها نشانه غیرفعال بودن برای کاربر کم‌بینا کافی
 * نیست، و {@code pointer-events-none} جلوی کلیک روی دکمه در حال ارسال را
 * می‌گیرد — که اگر نباشد، کاربر می‌تواند دو بار ارسال کند.
 *
 * <p>⚠️ حداقل ارتفاع از {@code index.css} می‌آید (۴۴px، الزام ناحیه لمس
 * سند بخش ۹)، پس اینجا تکرار نشده.
 */
export function Button({ variant = 'primary', children, className, type, ...rest }: ButtonProps) {
  return (
    <button
      // ⚠️ پیش‌فرض `button` است نه `submit`. بدون این، هر دکمه‌ای که
      // اتفاقی داخل یک `<form>` بیفتد صفحه را رفرش می‌کند.
      type={type ?? 'button'}
      className={[
        'inline-flex items-center justify-center gap-2 rounded-xl px-4 text-sm font-medium transition-colors',
        'disabled:pointer-events-none disabled:opacity-50',
        VARIANTS[variant],
        className ?? '',
      ].join(' ')}
      {...rest}
    >
      {children}
    </button>
  );
}
