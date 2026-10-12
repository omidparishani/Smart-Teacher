import js from '@eslint/js';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import reactHooks from 'eslint-plugin-react-hooks';

/**
 * پیکربندی ESLint — Flat Config.
 *
 * <p>⚠️ قواعد <b>type-aware</b> عمدی هستند
 * ({@code recommendedTypeChecked}). این پروژه داده غیرقابل‌اعتماد را از
 * ایتا می‌گیرد و به بک‌اند می‌فرستد؛ قواعدی مثل
 * {@code no-unsafe-assignment} و {@code no-unsafe-member-access} دقیقاً
 * همان جایی گیر می‌کنند که یک {@code any} ناخواسته وارد مسیر امنیتی شده
 * است. نسخه بدون type-check این‌ها را نمی‌بیند.
 */
export default tseslint.config(
  {
    ignores: [
      'dist/**',
      'node_modules/**',
      'coverage/**',
      'size-limit-report.json',
      // ⚠️ خروجی Playwright — تولیدشده است، نه نوشته‌شده.
      'test-results/**',
      'playwright-report/**',
    ],
  },

  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      ...tseslint.configs.recommendedTypeChecked,
      ...tseslint.configs.stylisticTypeChecked,
    ],
    languageOptions: {
      ecmaVersion: 2023,
      globals: globals.browser,
      parserOptions: {
        // ⚠️ `projectService` و نه `project`: خودش نزدیک‌ترین tsconfig را
        // پیدا می‌کند و لازم نیست فایل‌ها را دستی فهرست کنیم.
        projectService: true,
        tsconfigRootDir: import.meta.dirname,
      },
    },
    plugins: { 'react-hooks': reactHooks },
    rules: {
      ...reactHooks.configs.flat['recommended-latest'].rules,

      // ⚠️ جایگزین `noUnusedLocals` تایپ‌اسکریپت در لینت، تا یک متغیر
      // بلااستفاده هم در `tsc` و هم در ESLint دیده شود.
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' },
      ],

      // ⚠️ Promise بدون await یعنی خطای بی‌صدا. در این پروژه هر جا
      // درخواست شبکه هست، این قاعده جلوی «فراموش کردم await بگذارم» را
      // می‌گیرد — باگی که خودش را به‌صورت داده ناموجود نشان می‌دهد.
      '@typescript-eslint/no-floating-promises': 'error',
    },
  },

  {
    // ⚠️ آزمون‌ها به `describe`/`it` جهانی (globals: true در vitest) و
    // همچنین به APIهای Node دسترسی دارند.
    files: ['**/*.test.{ts,tsx}', 'src/test/**/*.ts'],
    languageOptions: {
      globals: { ...globals.browser, ...globals.node },
    },
  },

  {
    files: ['*.config.{ts,js}'],
    languageOptions: {
      globals: globals.node,
    },
    rules: {
      // ⚠️ فایل‌های پیکربندی در هیچ tsconfigی نیستند، پس اطلاعات نوع
      // ندارند و قواعد type-aware آنجا فقط نویز می‌سازند.
      ...tseslint.configs.disableTypeChecked.rules,
    },
  },

  {
    files: ['eslint.config.js'],
    languageOptions: {
      globals: globals.node,
    },
  },
);
