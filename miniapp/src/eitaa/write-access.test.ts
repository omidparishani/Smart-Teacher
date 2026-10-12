import { afterEach, describe, expect, it, vi } from 'vitest';
import { ensureWriteAccess, resetWriteAccessPromptForTest } from './write-access';
import { installFakeEitaa } from '../test/fake-eitaa';

/**
 * آزمون جریان اجازه ارسال پیام.
 *
 * <p>⚠️ سه حالت باید از هم جدا بمانند: «دارد»، «گرفت» و «نشد». اگر «نشد»
 * با «رد کرد» یکی شود، کاربری که SDK قدیمی دارد هر بار دوباره پرسیده
 * می‌شود — یا بدتر، ما فکر می‌کنیم کاربر رد کرده و دیگر هرگز نمی‌پرسیم.
 */
describe('ensureWriteAccess', () => {
  afterEach(() => {
    resetWriteAccessPromptForTest();
    vi.useRealTimers();
  });

  it('بیرون از ایتا «پشتیبانی‌نشده» می‌دهد', async () => {
    await expect(ensureWriteAccess()).resolves.toBe('unsupported');
  });

  it('اگر از قبل اجازه داده شده باشد، نمی‌پرسد', async () => {
    let asked = false;

    installFakeEitaa({
      allowsWriteToPm: true,
      onRequestWriteAccess: () => {
        asked = true;
      },
    });

    await expect(ensureWriteAccess()).resolves.toBe('already-granted');
    expect(asked).toBe(false);
  });

  it('پاسخ مثبت کاربر را «گرفت» می‌خواند', async () => {
    installFakeEitaa({ allowsWriteToPm: false, onRequestWriteAccess: (cb) => cb(true) });

    await expect(ensureWriteAccess()).resolves.toBe('granted');
  });

  it('پاسخ منفی کاربر را «رد کرد» می‌خواند', async () => {
    installFakeEitaa({ allowsWriteToPm: false, onRequestWriteAccess: (cb) => cb(false) });

    await expect(ensureWriteAccess()).resolves.toBe('denied');
  });

  it('🔴 پس از یک رد، هرگز دوباره نمی‌پرسد', async () => {
    const spy = vi.fn<(cb: (granted: boolean) => void) => void>((cb) => cb(false));

    installFakeEitaa({ allowsWriteToPm: false, onRequestWriteAccess: spy });

    await ensureWriteAccess();
    await ensureWriteAccess();
    await ensureWriteAccess();

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('نسخه قدیمی SDK که متد ندارد، خطا نمی‌دهد', async () => {
    installFakeEitaa({ withoutRequestWriteAccess: true });

    await expect(ensureWriteAccess()).resolves.toBe('unsupported');
  });

  it('SDKی که هنگام صدا زدن پرتاب می‌کند، برنامه را نمی‌شکند', async () => {
    installFakeEitaa({
      onRequestWriteAccess: () => {
        throw new Error('SDK قدیمی');
      },
    });

    await expect(ensureWriteAccess()).resolves.toBe('unsupported');
  });

  it('🔴 اگر کال‌بک هرگز نیاید، پس از مهلت «پشتیبانی‌نشده» می‌دهد', async () => {
    // ⚠️ این آزمون همان ریسک راستی‌آزمایی‌نشده دسکتاپ ویندوز را پوشش
    // می‌دهد: اگر `requestWriteAccess` هیچ‌وقت پاسخ ندهد، بدون مهلت
    // Promise تا ابد معلق می‌ماند و صفحه ورود بی‌صدا قفل می‌شود.
    vi.useFakeTimers();

    installFakeEitaa({ onRequestWriteAccess: () => undefined });

    const pending = ensureWriteAccess();

    await vi.advanceTimersByTimeAsync(10_001);

    await expect(pending).resolves.toBe('unsupported');
  });

  it('پاسخ دیرهنگام پس از مهلت، نتیجه را عوض نمی‌کند', async () => {
    vi.useFakeTimers();

    let lateCallback: ((granted: boolean) => void) | undefined;

    installFakeEitaa({
      onRequestWriteAccess: (cb) => {
        lateCallback = cb;
      },
    });

    const pending = ensureWriteAccess();

    await vi.advanceTimersByTimeAsync(10_001);
    await expect(pending).resolves.toBe('unsupported');

    // ⚠️ کاربر چند ثانیه بعد «اجازه می‌دهم» را می‌زند. محافظ
    // دو-بار-حل باید جلوی تغییر نتیجه را بگیرد.
    lateCallback?.(true);
    await vi.advanceTimersByTimeAsync(1);

    await expect(pending).resolves.toBe('unsupported');
  });
});
