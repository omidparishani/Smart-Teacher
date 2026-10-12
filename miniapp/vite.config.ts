/// <reference types="vitest/config" />
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

/**
 * 🔴 <b>چرا اینجا پلاگینی برای حذف {@code VITE_DEV_INIT_DATA} نیست؟</b>
 *
 * <p>چون یکی نوشته و <b>آزموده</b> شد و لازم نبود. سه اندازه‌گیری:
 * <ol>
 *   <li>بیلد Production، <b>بدون</b> پلاگین، با minifier: هیچ اثری از
 *       مقدار نبود.</li>
 *   <li>یک بازآرایی عمداً «شکننده» (بردن مقدار به یک ثابت سطح ماژول، تا
 *       تاش‌پذیری از بین برود)، <b>بدون</b> پلاگین و با
 *       {@code --minify false}: باز هم هیچ اثری نبود. کل شاخه‌ی توسعه و
 *       حتی خودِ ثابت از باندل حذف شده بودند.</li>
 *   <li>پس حذف، کار bundler است نه کار minifier: Vite مقدار
 *       {@code import.meta.env.PROD} را به {@code true} تبدیل می‌کند و
 *       bundler شاخه‌ی مرده را کنار می‌گذارد.</li>
 * </ol>
 *
 * <p>پس دروازه، در {@code src/eitaa/init-data.ts} بسته است و نه در
 * پیکربندی. آنچه تضمین می‌کند بسته بماند
 * {@code scripts/verify-bundle.mjs} است — و آن اسکریپت با یک
 * «قناری» واقعی آزموده شده: مقدار ممنوع در باندل کاشته شد و دروازه
 * شکست خورد.
 *
 * <p>⚠️ <b>چرا پلاگینِ اضافی نگه داشته نشد؟</b> چون در بدترین حالت
 * <i>مضر</i> بود: اگر روزی مقدار به مسیری برسد که واقعاً در Production
 * اجرا می‌شود، آن پلاگین مقدار را بی‌صدا به {@code undefined} تبدیل
 * می‌کرد و بیلد سبز می‌ماند — یعنی یک نقص واقعیِ دروازه را پنهان
 * می‌کرد. شکستِ بلند و واضح در {@code verify-bundle.mjs} رفتار درست
 * است.
 *
 * <p>⚠️ آزمون‌های ناوبری که با {@code define} هم انجام شد، شکست خورد:
 * جایگزینی متغیرهای محیطی در Vite <i>پس از</i> {@code define} اجرا
 * می‌شود، پس {@code define} در این کار بازنده است. این هم یکی از
 * دلایلی است که راه‌حل در کد است و نه در پیکربندی.
 */

export default defineConfig(({ command, mode }) => {
  const isProductionBuild = command === 'build';

  /*
   * ⚠️ هشدار بلند، چون این حالت روی دستگاه توسعه پیش می‌آید و اگر کسی
   * نداند، فکر می‌کند همه‌چیز مرتب است.
   *
   * <p>دلیل ریشه‌ای اینکه این حالت پیش می‌آید: Vite فایل
   * {@code .env.local} را در <b>همه</b> حالت‌ها می‌خواند — از جمله
   * {@code build}. یعنی هر مقدار «فقط توسعه» که آنجا بگذارید به باندل
   * محصول راه پیدا می‌کند. راه درست، گذاشتنش در
   * {@code .env.development.local} است.
   *
   * <p>⚠️ متن هشدار می‌گوید مقدار به باندل <i>نمی‌رسد</i>. این ادعا
   * اندازه‌گیری شده (بالا) و {@code verify-bundle.mjs} هم پایش می‌کند.
   */
  if (isProductionBuild) {
    const env = loadEnv(mode, process.cwd(), 'VITE_');
    const devInitData: string = env.VITE_DEV_INIT_DATA ?? '';

    if (devInitData.length > 0) {
      console.warn(
        '\n⚠️  VITE_DEV_INIT_DATA در حال بیلد Production مقدار دارد.\n' +
          '   مقدارش به باندل نمی‌رسد (شرط import.meta.env.PROD در\n' +
          '   getInitData و حذف کد مرده)، ولی جای این متغیر\n' +
          '   .env.development.local است، نه .env.local.\n',
      );
    }
  }

  return {
    plugins: [react(), tailwindcss()],

    server: {
      port: 5173,
      // ⚠️ ایتا Mini App را در WebView باز می‌کند. برای آزمون دستی روی
      // دستگاه واقعی، باید از شبکه محلی هم قابل دسترسی باشد.
      host: true,

      /*
       * ⚠️ پروکسی فقط یک Fallback است، نه مسیر اصلی.
       *
       * مسیر اصلی همان `VITE_API_URL` مطلق است (`http://localhost:8080`)
       * که با CORS بک‌اند کار می‌کند — و روی دستگاه واقعی هم همان چیزی
       * است که لازم می‌شود، چون آنجا «localhost» یعنی خودِ گوشی.
       *
       * این پروکسی وقتی به کار می‌آید که `VITE_API_URL` تنظیم نشده باشد:
       * آن وقت `BASE_URL` به `/api/v1` نسبی برمی‌گردد و بدون پروکسی،
       * درخواست به خودِ سرور Vite می‌خورد و ۴۰۴ می‌گیرد.
       */
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },

    build: {
      /*
       * ⚠️ سقف هشدار Vite روی حجم <b>خام</b> است، نه gzip.
       *
       * این عدد پیش‌تر ۱۵۰ بود — یعنی همان بودجه‌ی مستند که <b>gzip</b>
       * است. نتیجه: این هشدار همیشه روشن می‌ماند (باندل خام ~۳۶۳kB است)
       * و هشداری که همیشه روشن باشد، دیر یا زود نادیده گرفته می‌شود.
       *
       * ۴۰۰kB خام ≈ ۱۲۶kB gzip است، پس همچنان <i>پیش از</i> رسیدن به
       * بودجه ۱۵۰kB هشدار می‌دهد. دروازه‌ی واقعی `size-check` است.
       */
      chunkSizeWarningLimit: 400,
      sourcemap: false,
    },

    test: {
      environment: 'jsdom',
      globals: true,
      setupFiles: ['./src/test/setup.ts'],
      include: ['src/**/*.test.{ts,tsx}'],
      // ⚠️ تست‌های E2E در `e2e/` جدا هستند و Playwright اجرا می‌کند،
      // نه Vitest. بدون این، Vitest سعی می‌کرد آن‌ها را هم بردارد.
      exclude: ['node_modules', 'dist', 'e2e'],
    },
  };
});
