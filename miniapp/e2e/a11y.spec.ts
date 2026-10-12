import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { blockEitaaSdk, installFakeEitaa, stubBackend } from './support/eitaa';

/**
 * بررسی دسترس‌پذیری — {@code docs/mini-app.md} بخش ۱۲ («هر صفحه»).
 *
 * <p><b>🔴 چرا این آزمون با «صفر تخلف» سنجیده می‌شود و نه «صفر تخلف جدی»؟</b>
 * چون آستانه‌ی نسبی، یک دروازه‌ی نرم است: هر بار که یک تخلف اضافه شود کسی
 * می‌تواند سطح را پایین‌تر بیاورد تا سبز شود. تخلف صفر، ادعای مطلقی است که
 * یا درست است یا نه.
 *
 * <p>⚠️ axe همه‌چیز را نمی‌گیرد. این ابزار «کنتراست کم، برچسب نبودن،
 * ساختار نادرست» را می‌گیرد؛ «آیا این متن مفهوم است» را نمی‌گیرد. پس سبز
 * بودن این آزمون یعنی «موانع ماشین‌خوان نیست»، نه «برنامه برای همه عالی
 * است».
 */

/** صفحه‌هایی که پوشش داده می‌شوند — هر پنج زبانه، صفحه مسدود، و ۴۰۴. */
const PAGES: readonly { path: string; label: string }[] = [
  { path: '/', label: 'خانه' },
  { path: '/books', label: 'کتاب‌ها' },
  { path: '/assignments', label: 'تکالیف' },
  { path: '/assistant', label: 'دستیار' },
  { path: '/settings', label: 'تنظیمات' },
  { path: '/this-route-does-not-exist', label: 'صفحه پیدا نشد' },
];

/**
 * اجرای axe و شکست با یک پیام فارسی خوانا.
 *
 * <p>⚠️ پیام پیش‌فرض Playwright یک شیء JSON غول‌آسا است. اینجا فقط
 * «قاعده → گره → توضیح» چاپ می‌شود تا رفع اشکال سریع باشد.
 */
async function expectNoViolations(page: Page, label: string): Promise<void> {
  const results = await new AxeBuilder({ page })
    // ⚠️ فقط قواعد WCAG سطح A و AA. قواعد «بهترین‌روش» (مثل ترتیب سرتیتر)
    // سلیقه‌ای‌اند و دروازه‌ای که روی سلیقه بسته شود، خاموش می‌شود.
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze();

  const report = results.violations.map(
    (violation) =>
      `- [${violation.id}] (${violation.impact ?? 'نامشخص'}) ${violation.help}\n` +
      violation.nodes.map((node) => `    ← ${node.target.join(' ')}`).join('\n'),
  );

  expect(report, `تخلف دسترس‌پذیری در صفحه «${label}»:\n${report.join('\n')}`).toEqual([]);
}

for (const { path, label } of PAGES) {
  test(`دسترس‌پذیری — ${label}`, async ({ page }) => {
    await installFakeEitaa(page);
    await stubBackend(page);

    await page.goto(path);

    // ⚠️ پیش از اسکن باید برنامه واقعاً رندر شده باشد. بدون این انتظار،
    // axe یک صفحه خالی را اسکن می‌کند و همیشه سبز است — یعنی آزمونی که
    // هیچ‌چیز را نمی‌سنجد.
    await expect(page.getByRole('navigation', { name: 'زبانه‌های اصلی' })).toBeVisible();

    await expectNoViolations(page, label);
  });
}

test('دسترس‌پذیری — صفحه «پلتفرم پشتیبانی نمی‌شود»', async ({ page }) => {
  await blockEitaaSdk(page);
  await stubBackend(page);

  await page.goto('/');

  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();

  await expectNoViolations(page, 'پلتفرم پشتیبانی نمی‌شود');
});

test('دسترس‌پذیری — صفحه «ورود انجام نشد»', async ({ page }) => {
  await installFakeEitaa(page);
  await page.route('**/api/v1/**', (route) =>
    route.fulfill({
      status: 503,
      json: { status: 503, code: 'INTERNAL_ERROR', message: 'سرور در دسترس نیست.' },
    }),
  );

  await page.goto('/');

  await expect(page.getByRole('heading', { name: 'ورود انجام نشد' })).toBeVisible();

  await expectNoViolations(page, 'ورود انجام نشد');
});

/**
 * 🔴 حالت تاریک هم اسکن می‌شود و نه فقط حالت روشن.
 *
 * <p>دلیلش یک تجربه واقعی در همین فاز است: تخلف کنتراستِ عنوان خطا
 * <i>فقط</i> در حالت روشن دیده می‌شد، چون در حالت تاریک زمینه تیره است و
 * همان قرمز روشن نسبت خوبی می‌دهد. اگر فقط یک تم آزموده شود، نصف پالت
 * هرگز سنجیده نمی‌شود.
 *
 * <p>⚠️ تم از {@code eitaa.colorScheme()} می‌آید و نه از
 * {@code prefers-color-scheme} — پس همان‌جا که منبع حقیقت است، جعل می‌شود.
 */
test('دسترس‌پذیری — حالت تاریک', async ({ page }) => {
  await installFakeEitaa(page, { colorScheme: 'dark' });
  await stubBackend(page);

  await page.goto('/');

  await expect(page.getByRole('navigation', { name: 'زبانه‌های اصلی' })).toBeVisible();
  // ⚠️ اگر تم اعمال نشده باشد، این ادعا می‌افتد و آزمون به‌جای اسکن تم
  // روشن، بی‌صدا چیز دیگری را می‌سنجید.
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');

  await expectNoViolations(page, 'خانه — حالت تاریک');
});

test('دسترس‌پذیری — خطای ورود در حالت تاریک', async ({ page }) => {
  await installFakeEitaa(page, { colorScheme: 'dark' });
  await page.route('**/api/v1/**', (route) =>
    route.fulfill({
      status: 503,
      json: { status: 503, code: 'INTERNAL_ERROR', message: 'سرور در دسترس نیست.' },
    }),
  );

  await page.goto('/');

  await expect(page.getByRole('heading', { name: 'ورود انجام نشد' })).toBeVisible();
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');

  await expectNoViolations(page, 'ورود انجام نشد — حالت تاریک');
});
