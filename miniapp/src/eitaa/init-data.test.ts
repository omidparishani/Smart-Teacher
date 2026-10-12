import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  getInitData,
  INIT_DATA_MESSAGES,
  InitDataUnavailableError,
  isDevInitDataInUse,
} from './init-data';
import { installFakeEitaa } from '../test/fake-eitaa';

/**
 * آزمون استخراج `initData`.
 *
 * <p>🔴 حیاتی‌ترین آزمون این فایل، «حالت Production» است: اگر روزی کسی
 * شرط {@code import.meta.env.PROD} را بردارد یا جابه‌جا کند، برنامه می‌شود
 * یک دروازه ورود جعلی — هر کسی با یک {@code initData} ساختگی وارد می‌شود.
 */
describe('getInitData', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it('داده ایتا را بر داده توسعه مقدم می‌داند', () => {
    vi.stubEnv('VITE_DEV_INIT_DATA', 'dev-data');
    installFakeEitaa({ initData: 'real-from-eitaa' });

    expect(getInitData()).toBe('real-from-eitaa');
    expect(isDevInitDataInUse()).toBe(false);
  });

  it('در حالت توسعه به داده جعلی برمی‌گردد و آن را اعلام می‌کند', () => {
    vi.stubEnv('VITE_DEV_INIT_DATA', 'dev-data');

    expect(getInitData()).toBe('dev-data');
    // ⚠️ اعلام شدن مهم است: بنر «حالت توسعه» روی همین پرچم سوار است.
    expect(isDevInitDataInUse()).toBe(true);
  });

  it('در حالت توسعه بدون داده جعلی، خطای قابل‌تشخیص می‌دهد', () => {
    vi.stubEnv('VITE_DEV_INIT_DATA', '');

    expect(() => getInitData()).toThrowError(InitDataUnavailableError);

    try {
      getInitData();
    } catch (error) {
      expect((error as InitDataUnavailableError).reason).toBe('dev-data-missing');
      expect((error as Error).message).toBe(INIT_DATA_MESSAGES['dev-data-missing']);
    }
  });

  it('🔴 در Production حتی با داده جعلی موجود، آن را نمی‌پذیرد', () => {
    vi.stubEnv('PROD', true);
    vi.stubEnv('VITE_DEV_INIT_DATA', 'dev-data');

    expect(() => getInitData()).toThrowError(InitDataUnavailableError);

    try {
      getInitData();
    } catch (error) {
      expect((error as InitDataUnavailableError).reason).toBe('not-in-eitaa');
    }

    expect(isDevInitDataInUse()).toBe(false);
  });

  it('در Production داخل ایتا، داده واقعی را می‌دهد', () => {
    vi.stubEnv('PROD', true);
    installFakeEitaa({ initData: 'real-from-eitaa' });

    expect(getInitData()).toBe('real-from-eitaa');
  });

  it('پرچم حالت توسعه بین فراخوانی‌ها پاک می‌شود', () => {
    vi.stubEnv('VITE_DEV_INIT_DATA', 'dev-data');
    getInitData();
    expect(isDevInitDataInUse()).toBe(true);

    installFakeEitaa({ initData: 'real' });
    getInitData();

    // ⚠️ بدون پاک‌سازی، بنر پس از ورود واقعی هم می‌ماند و دروغ می‌گوید.
    expect(isDevInitDataInUse()).toBe(false);
  });
});
