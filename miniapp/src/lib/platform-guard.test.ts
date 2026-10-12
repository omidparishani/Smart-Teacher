import { describe, expect, it, vi } from 'vitest';
import { checkPlatform, PLATFORM_MESSAGES } from './platform-guard';
import { installFakeEitaa } from '../test/fake-eitaa';

/**
 * آزمون گارد پلتفرم.
 *
 * <p>⚠️ این یک واقعیت <b>خارجی</b> را کد می‌کند: ایتا برنامک‌ها را فقط روی
 * اندروید و دسکتاپ ویندوز اجرا می‌کند. پس هدف آزمون این است که مطمئن شویم
 * کاربر iOS و کاربر مرورگر <i>صفحه سفید نمی‌بینند</i>.
 */
describe('checkPlatform', () => {
  it('بیرون از ایتا مسدود می‌کند', () => {
    const verdict = checkPlatform();

    expect(verdict.ok).toBe(false);

    if (verdict.ok) {
      throw new Error('unreachable');
    }

    expect(verdict.reason).toBe('not-in-eitaa');
    expect(verdict.message).toBe(PLATFORM_MESSAGES.notInEitaa);
  });

  it('روی iOS مسدود می‌کند و پیام اندروید/ویندوز می‌دهد', () => {
    installFakeEitaa({ platform: 'ios' });

    const verdict = checkPlatform();

    expect(verdict.ok).toBe(false);

    if (verdict.ok) {
      throw new Error('unreachable');
    }

    expect(verdict.reason).toBe('ios');
    expect(verdict.message).toContain('اندروید');
    expect(verdict.message).toContain('ویندوز');
  });

  it('روی اندروید عبور می‌دهد', () => {
    installFakeEitaa({ platform: 'android' });

    expect(checkPlatform().ok).toBe(true);
  });

  it.each(['tdesktop', 'web', 'macos', 'unknown'])(
    'پلتفرم %s را مسدود نمی‌کند',
    (platform) => {
      // 🔴 تصمیم عمدی: پلتفرم ناشناخته رد نمی‌شود. هزینه رد کردن یک
      // کاربر واقعی بیشتر از اجازه دادن به کاربری است که در هر صورت در
      // لایه‌های بعدی به مشکل می‌خورد.
      installFakeEitaa({ platform });

      expect(checkPlatform().ok).toBe(true);
    },
  );

  it('iOS را با زیررشته تشخیص می‌دهد، نه برابری دقیق', () => {
    // ⚠️ فهرست مقادیر `platform` در مستندات بسته نیست؛ اگر ایتا روزی
    // `ios-arm64` بدهد، ما نباید آن را از دست بدهیم.
    installFakeEitaa({ platform: 'iOS-WebKit' });

    expect(checkPlatform().ok).toBe(false);
  });
});

/**
 * آزمون دروازه توسعه.
 *
 * <p>⚠️ این دروازه عمداً <b>Opt-in صریح</b> است: روی شبکه‌ای که
 * {@code developer.eitaa.com} در دسترس نیست، {@code window.Eitaa} ساخته
 * نمی‌شود و بدون این دروازه کار روی برنامک در مرورگر ممکن نیست. ولی
 * اگر پیش‌فرض روشن باشد، یک کنترل امنیتی بی‌صدا باز می‌ماند.
 */
describe('دروازه توسعه — VITE_DEV_ALLOW_BROWSER', () => {
  it('وقتی روشن باشد، بیرون از ایتا عبور می‌دهد', () => {
    vi.stubEnv('DEV', true);
    vi.stubEnv('PROD', false);
    vi.stubEnv('VITE_DEV_ALLOW_BROWSER', 'true');

    const verdict = checkPlatform();

    expect(verdict.ok).toBe(true);
    expect(verdict.platform).toBe('dev-browser');
  });

  it('پیش‌فرض خاموش است — بدون متغیر، مسدود می‌ماند', () => {
    vi.stubEnv('DEV', true);
    vi.stubEnv('PROD', false);
    vi.stubEnv('VITE_DEV_ALLOW_BROWSER', '');

    expect(checkPlatform().ok).toBe(false);
  });

  it.each(['1', 'TRUE', 'yes'])(
    'مقدار %s به‌عنوان «روشن» پذیرفته نمی‌شود — فقط true',
    (value) => {
      // ⚠️ مقایسه دقیق عمدی است: یک متغیر با مقدار «۱» یا «بله» باید
      // خاموش بماند، نه اینکه با تفسیر سست روشن شود.
      vi.stubEnv('DEV', true);
      vi.stubEnv('PROD', false);
      vi.stubEnv('VITE_DEV_ALLOW_BROWSER', value);

      expect(checkPlatform().ok).toBe(false);
    },
  );

  it('🔴 در بیلد Production هرگز عبور نمی‌دهد', () => {
    // 🔴 مهم‌ترین آزمون این بلوک: حتی اگر متغیر در محیط تنظیم شده باشد،
    // شاخه Production باید بسته بماند. نگهبانِ باندل،
    // `scripts/verify-bundle.mjs` است؛ این آزمون نگهبانِ منطق.
    vi.stubEnv('DEV', false);
    vi.stubEnv('PROD', true);
    vi.stubEnv('VITE_DEV_ALLOW_BROWSER', 'true');

    expect(checkPlatform().ok).toBe(false);
  });

  it('داخل ایتا، مقدار متغیر بی‌اثر است', () => {
    // ⚠️ دروازه فقط مسیر «نبود SDK» را عوض می‌کند. کاربر iOS با این
    // متغیر نباید به برنامه راه پیدا کند.
    installFakeEitaa({ platform: 'ios' });
    vi.stubEnv('DEV', true);
    vi.stubEnv('PROD', false);
    vi.stubEnv('VITE_DEV_ALLOW_BROWSER', 'true');

    expect(checkPlatform().ok).toBe(false);
  });
});
