import { describe, expect, it } from 'vitest';
import { routes } from './routes';

/** قفل مسیرهای فاز ۸ — آزمون. */
describe('routes — فاز ۸', () => {
  const children = routes[0]?.children ?? [];

  function pathOf(path: string) {
    return children.find((route) => route.path === path);
  }

  it('مسیرهای آزمون تعریف شده‌اند', () => {
    expect(pathOf('exams')).toBeDefined();
    expect(pathOf('exams/generate')).toBeDefined();
    expect(pathOf('exams/:id/manage')).toBeDefined();
    expect(pathOf('exams/:id/take')).toBeDefined();
    expect(pathOf('exams/:id/results')).toBeDefined();
  });

  it('صفحه تولید آزمون ExamGeneratePage است', () => {
    const route = pathOf('exams/generate');
    const typeName = (route?.element as { type?: { name?: string } } | undefined)?.type?.name;
    expect(typeName).toBe('ExamGeneratePage');
  });

  it('صفحه مدیریت ExamManagePage است', () => {
    const route = pathOf('exams/:id/manage');
    const typeName = (route?.element as { type?: { name?: string } } | undefined)?.type?.name;
    expect(typeName).toBe('ExamManagePage');
  });
});
