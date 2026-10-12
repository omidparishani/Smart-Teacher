#!/usr/bin/env node
/**
 * بررسی باندل تولید — دروازه‌ای که یک نقص واقعی را دائمی می‌کند.
 *
 * <p><b>🔴 چرا این فایل وجود دارد؟</b> چون Vite فایل {@code .env.local} را
 * در بیلد Production هم می‌خواند، پس یک مقدار «فقط توسعه» می‌تواند سوار
 * محصول شود. دروازه امروز در {@code src/eitaa/init-data.ts} بسته است:
 * بررسی {@code import.meta.env.PROD} اول می‌آید و bundler شاخه‌ی توسعه را
 * به‌عنوان کد مرده حذف می‌کند — این با اندازه‌گیری تأیید شد، نه با
 * استدلال (بخش ۱۲ {@code docs/verification-log.md}).
 *
 * <p>اما آن تضمین <i>نتیجه‌ی ضمنی</i> یک بازآرایی است: کافی است کسی ترتیب
 * شاخه‌ها را عوض کند تا مقدار دوباره وارد باندل شود — <b>بی‌صدا</b>. این
 * اسکریپت آن حالت را به یک شکست بلند تبدیل می‌کند.
 *
 * <p>🔴 <b>خودِ این اسکریپت هم آزموده شده.</b> یک «قناری» در باندل کاشته
 * شد و دروازه شکست خورد؛ دروازه‌ای که هرگز شکست نخورده، معلوم نیست کار
 * می‌کند یا نه.
 *
 * <p>⚠️ این یک آزمون واحد نیست و نمی‌تواند باشد: بررسی، خروجی <i>بیلد</i>
 * را می‌خواند، نه رفتار ماژول را. پس در زنجیره بیلد قرار می‌گیرد
 * ({@code npm run build}).
 *
 * <p>🔴 <b>دقتِ لازم در الگو — یک درس از همین اجرا:</b> نسخه اول این
 * اسکریپت دنبال زیررشته {@code dev-data} می‌گشت و <b>مثبت کاذب</b> داد.
 * دلیلش این بود که رشته‌های دائمی و بی‌ضرر هم آن زیررشته را دارند:
 * کلید {@code 'dev-data-missing'} و پیام فارسی‌ای که نام متغیر را توضیح
 * می‌دهد. پس الگو الان فقط <i>مقدار به‌صورت رشته‌ی نقل‌قول‌شده</i> را
 * می‌جوید — یعنی همان شکلی که یک مقدار درون‌ریزی‌شده در باندل پیدا
 * می‌شود. دروازه‌ای که مثبت کاذب می‌دهد، دیر یا زود خاموش می‌شود و دیگر
 * هیچ‌چیز را نمی‌گیرد.
 *
 * <p>اجرا: {@code node scripts/verify-bundle.mjs}
 */

import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

const DIST = 'dist/assets';

/**
 * فایل‌های محیطی‌ای که روی بیلد Production اثر دارند.
 *
 * <p>⚠️ {@code .env.local} مهم‌ترینشان است: Vite آن را در <b>همه</b>
 * حالت‌ها می‌خواند، از جمله {@code build}.
 */
const PRODUCTION_ENV_FILES = ['.env.local', '.env', '.env.production', '.env.production.local'];

const VAR_NAME = 'VITE_DEV_INIT_DATA';

/** خواندن مقدار یک متغیر از یک فایل محیطی. */
function readEnvValue(file, name) {
  if (!existsSync(file)) {
    return null;
  }

  for (const rawLine of readFileSync(file, 'utf8').split('\n')) {
    const line = rawLine.trim();

    if (line.length === 0 || line.startsWith('#')) {
      continue;
    }

    const [key, ...rest] = line.split('=');

    if (key === name) {
      const value = rest.join('=').trim();

      return value.length > 0 ? value : null;
    }
  }

  return null;
}

function fail(lines) {
  console.error('\n❌ بررسی باندل شکست خورد:\n');

  for (const line of lines) {
    console.error(`   • ${line}`);
  }

  console.error(
    '\n   برای رفع: دروازه در src/eitaa/init-data.ts است — شاخه‌ی توسعه\n' +
      '   باید پس از بررسی import.meta.env.PROD بیاید تا bundler بتواند\n' +
      '   آن را به‌عنوان کد مرده حذف کند. اگر بازآرایی‌ای ترتیب را به‌هم\n' +
      '   زده، همین اسکریپت جلوی انتشار را می‌گیرد.\n',
  );

  process.exit(1);
}

if (!existsSync(DIST)) {
  fail([`پوشه ${DIST} وجود ندارد — اول بیلد بگیرید.`]);
}

const files = readdirSync(DIST).filter((name) => name.endsWith('.js'));

if (files.length === 0) {
  fail([`هیچ فایل .js در ${DIST} نیست — بیلد ناقص است.`]);
}

// مقدارهای ممنوع — از پیکربندی واقعی دستگاه، نه از یک رشته ثابت.
const forbidden = new Set(
  PRODUCTION_ENV_FILES.map((file) => readEnvValue(file, VAR_NAME)).filter(
    (value) => value !== null,
  ),
);

if (forbidden.size === 0) {
  console.log(`ℹ️  ⓘ مقدار ${VAR_NAME} در فایل‌های محیطی Production نیست — بررسی ساده‌تر می‌شود.`);
}

const problems = [];

for (const file of files) {
  const content = readFileSync(join(DIST, file), 'utf8');

  for (const value of forbidden) {
    // ⚠️ فقط شکل نقل‌قول‌شده — چون یک مقدار درون‌ریزی‌شده، همیشه یک
    // رشته ثابت در باندل است. جست‌وجوی زیررشته، کلیدهای بی‌ضرر مثل
    // `dev-data-missing` را هم می‌گیرد.
    for (const quote of ['"', "'", '`']) {
      if (content.includes(`${quote}${value}${quote}`)) {
        problems.push(`مقدار ${VAR_NAME} («${value}») داخل ${file} است.`);
      }
    }
  }
}

if (problems.length > 0) {
  fail(problems);
}

console.log(
  `✅ باندل پاک است — ${files.length} فایل بررسی شد؛ ` +
    `${forbidden.size} مقدار ممنوع جست‌وجو شد و هیچ‌کدام نبود.`,
);
