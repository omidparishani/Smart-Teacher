import { describe, expect, it } from 'vitest';
import { routes } from './routes';

/**
 * قفل مسیرهای فاز ۳ — تا کسی تصادفی مقصد واقعی کلاس/دانش‌آموز را
 * به Placeholder برنگرداند.
 */
describe('routes — فاز ۳', () => {
  const children = routes[0]?.children ?? [];

  function pathOf(path: string) {
    return children.find((route) => route.path === path);
  }

  it('مسیرهای کلاس و دانش‌آموز تعریف شده‌اند', () => {
    expect(pathOf('classes')).toBeDefined();
    expect(pathOf('classes/:id')).toBeDefined();
    expect(pathOf('classes/:id/students')).toBeDefined();
    expect(pathOf('students')).toBeDefined();
    expect(pathOf('students/new')).toBeDefined();
    expect(pathOf('students/:id')).toBeDefined();
  });

  it('جزئیات کلاس دیگر Placeholder فاز ۵ نیست', () => {
    const classRoute = pathOf('classes/:id');
    // عنصر واقعی ClassDetailPage است — Placeholder نیست
    expect(classRoute?.element).toBeTruthy();
    const typeName = (classRoute?.element as { type?: { name?: string } } | undefined)?.type
      ?.name;
    expect(typeName).toBe('ClassDetailPage');
  });
});
