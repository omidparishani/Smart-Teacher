import type { EitaaUnsafeData, EitaaWebAppApi } from '../eitaa/types';

export interface FakeEitaaOptions {
  /** رشته خام `initData`. پیش‌فرض یک مقدار غیرخالی. */
  initData?: string;
  /** پارامتر Deep Link. */
  startParam?: string;
  platform?: string;
  colorScheme?: 'light' | 'dark';
  /** همان `allows_write_to_pm` در داده تأییدنشده. */
  allowsWriteToPm?: boolean;
  version?: string;
  /**
   * شبیه‌سازی نبودن متد در نسخه‌های قدیمی SDK.
   *
   * <p>⚠️ لازم است چون تفاوت نسخه‌ها یکی از سه ریسک واقعی این لایه است و
   * بدون این گزینه، مسیر «نسخه قدیمی» هیچ‌وقت آزموده نمی‌شود.
   */
  withoutRequestWriteAccess?: boolean;
  /** رفتار دلخواه {@code requestWriteAccess} — مثلاً هرگز کال‌بک نزدن. */
  onRequestWriteAccess?: (cb: (granted: boolean) => void) => void;
}

/**
 * نصب یک SDK جعلی روی {@code window.Eitaa}.
 *
 * <p>⚠️ این جعل «داخل ایتا بودن» را شبیه‌سازی می‌کند، ولی هیچ‌چیز را
 * <i>تأیید</i> نمی‌کند — دقیقاً مثل خود SDK واقعی. امضای
 * {@code initData} فقط در بک‌اند بررسی می‌شود.
 *
 * @returns همان شیء نصب‌شده، برای بررسی فراخوانی‌ها در آزمون
 */
export function installFakeEitaa(options: FakeEitaaOptions = {}): EitaaWebAppApi {
  const unsafe: EitaaUnsafeData = {
    auth_date: 1_700_000_000,
    hash: 'fake-hash',
    ...(options.startParam === undefined ? {} : { start_param: options.startParam }),
    user: {
      id: 12345,
      first_name: 'آزمون',
      ...(options.allowsWriteToPm === undefined
        ? {}
        : { allows_write_to_pm: options.allowsWriteToPm }),
    },
  };

  const app = {
    initData: options.initData ?? 'query_id=fake&hash=fake-hash',
    initDataUnsafe: unsafe,
    version: options.version ?? '7.0',
    platform: options.platform ?? 'android',
    colorScheme: options.colorScheme ?? 'light',
    themeParams: {},
    isExpanded: true,
    MainButton: undefined,
    SecondaryButton: undefined,
    BackButton: undefined,
    HapticFeedback: { selectionChanged: () => undefined },
    ready: () => undefined,
    expand: () => undefined,
    close: () => undefined,
    isVersionAtLeast: () => true,
    onEvent: () => undefined,
    offEvent: () => undefined,
    showAlert: () => undefined,
    showConfirm: () => undefined,
    openLink: () => undefined,
    openEitaaLink: () => undefined,
    requestContact: () => undefined,
  } as unknown as EitaaWebAppApi;

  if (!options.withoutRequestWriteAccess) {
    (app as { requestWriteAccess?: unknown }).requestWriteAccess =
      options.onRequestWriteAccess ?? ((cb: (granted: boolean) => void) => cb(true));
  }

  window.Eitaa = { WebApp: app };

  return app;
}
