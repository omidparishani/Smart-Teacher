import { defineConfig, devices } from '@playwright/test';

/**
 * پیکربندی E2E — {@code docs/mini-app.md} بخش ۱۲.
 *
 * <p><b>🔴 نکته‌ای که باید ثبت شود: مرورگر از روی سیستم استفاده می‌شود.</b>
 * دانلود Chromium با {@code npx playwright install} روی این شبکه شکست خورد
 * ({@code UNABLE_TO_VERIFY_LEAF_SIGNATURE} — گواهی پروکسی سازمانی). به‌جای
 * تکیه بر یک دانلود نامطمئن، از کانال {@code chrome} استفاده می‌شود که به
 * Chrome نصب‌شده روی سیستم وصل می‌شود. نتیجه: <b>هیچ باینری‌ای دانلود
 * نمی‌شود</b> و آزمون روی همان موتور Chromium اجرا می‌شود.
 *
 * <p>⚠️ هزینه‌اش این است که نسخه مرورگر روی هر دستگاه می‌تواند فرق کند. این
 * محدودیت در {@code docs/verification-log.md} بخش ۱۲ ثبت شده است.
 */
export default defineConfig({
  testDir: './e2e',

  // ⚠️ هر فایل آزمون مستقل از دیگری اجرا می‌شود: هیچ آزمونی به وضعیت
  // باقی‌مانده از آزمون قبلی تکیه نمی‌کند.
  fullyParallel: true,

  // 🔴 اگر `test.only` اشتباهی در کد بماند، CI باید بایستد — نه اینکه
  // خاموش و سبز شود.
  forbidOnly: Boolean(process.env.CI),

  retries: process.env.CI === undefined ? 0 : 1,
  reporter: [['list']],

  use: {
    baseURL: 'http://127.0.0.1:5173',

    // ⚠️ اثر شکست برای اشکال‌زدایی. `on-first-retry` عمدی است: ذخیره‌ی
    // هر اجرا حجم بی‌دلیل می‌سازد.
    trace: 'on-first-retry',

    // ⚠️ اندازه‌ی گوشی مرجع است، نه دسکتاپ: این برنامک در WebView ایتا
    // روی موبایل اجرا می‌شود. آزمون دسکتاپ چیدمانی را می‌سنجید که کاربر
    // هرگز نمی‌بیند.
    viewport: { width: 390, height: 844 },
    deviceScaleFactor: 2,
    isMobile: false,
    hasTouch: true,
  },

  projects: [
    {
      name: 'chromium',

      use: {
        ...devices['Desktop Chrome'],
        channel: 'chrome',
        viewport: { width: 390, height: 844 },
        hasTouch: true,
      },
    },
  ],

  webServer: {
    command: 'npm run dev -- --port 5173 --strictPort',

    // 🔴 دروازه توسعه در آزمون خاموش می‌شود.
    //
    // `.env.development.local` مقدار `VITE_DEV_ALLOW_BROWSER=true` را دارد
    // (تا کار روی برنامک در مرورگر ممکن باشد). اگر همین مقدار به سرور
    // آزمون برسد، آزمون «بیرون از ایتا» دیگر صفحه گارد را نمی‌بیند و
    // سبزِ دروغ می‌شود. Vite مقدار `process.env` را بر فایل `.env` مقدم
    // می‌داند، پس یک مقدار خالی اینجا کافی است.
    env: {
      VITE_DEV_ALLOW_BROWSER: '',
    },

    // ⚠️ `127.0.0.1` و نه `localhost`: روی ویندوز، `localhost` ممکن است
    // به `::1` حل شود و Vite روی آن گوش ندهد؛ نتیجه یک مهلت تمام‌شده‌ی
    // گمراه‌کننده است، نه یک خطای روشن.
    url: 'http://127.0.0.1:5173',

    // ⚠️ `false` و نه `reuseExistingServer`: با استفاده از سرور موجود،
    // مقدار `VITE_DEV_ALLOW_BROWSER` بالا اعمال نمی‌شود و نتیجه به این
    // بستگی پیدا می‌کند که کسی سرور توسعه را باز گذاشته باشد یا نه.
    // شکست صریح («پورت اشغال است») بهتر از سبز شدن تصادفی است.
    reuseExistingServer: false,
    timeout: 120_000,
    stdout: 'ignore',
    stderr: 'pipe',
  },
});
