/**
 * سطح SDK ایتا — همان‌طور که راستی‌آزمایی شده است.
 *
 * <p><b>منبع:</b> {@code developer.eitaa.com/docs/Develop/JsSDK/} — فهرست کامل
 * در {@code docs/eitaa.md} بخش ۳ ثبت شده.
 *
 * <p><b>⚠️ چرا این فایل وجود دارد و کامپوننت‌ها مستقیم با SDK حرف نمی‌زنند؟</b>
 * چون SDK بیرونی است و سه چیز می‌تواند غافلگیر کند: نام شیء Global، نبود
 * متد در نسخه‌های قدیمی کلاینت، و تفاوت بین نسخه اندروید و دسکتاپ. با یک
 * لایه Adapter، اگر ایتا نامی را عوض کند، <b>فقط همین پوشه</b> اصلاح می‌شود.
 *
 * <p><b>⚠️ و یک نکته امنیتی که نباید گم شود:</b> هرچه از SDK می‌آید
 * <b>داده غیرقابل‌اعتماد</b> است. مخصوصاً {@code initData} — که شامل
 * {@code initDataUnsafe} هم می‌شود. نامش خودش همین را می‌گوید. اعتبار آن
 * فقط و فقط در بک‌اند و با HMAC-SHA256 تأیید می‌شود
 * ({@code docs/eitaa.md} بخش ۷). هیچ تصمیم امنیتی در این لایه گرفته نمی‌شود.
 */

/** دکمه پایین صفحه — ساختار مستندشده. */
export interface BottomButton {
  text: string;
  isVisible: boolean;
  isActive: boolean;
  show(): void;
  hide(): void;
  enable(): void;
  disable(): void;
  onClick(cb: () => void): void;
  setText(text: string): void;
}

/**
 * کاربر داخل {@code initDataUnsafe}.
 *
 * <p>⚠️ {@code id} در مستندات تا **۵۲ بیت** اهمیت دارد. در TypeScript
 * {@code number} یک double است و تا ۲^۵۳ را دقیق نگه می‌دارد، پس امن است —
 * <b>ولی به شرطی که هیچ‌جا از {@code | 0} یا {@code parseInt} بیت‌به‌بیت
 * استفاده نشود.</b> در جاوا همین عدد باید {@code Long} باشد، نه {@code int}.
 */
export interface EitaaUnsafeUser {
  id: number;
  first_name: string;
  last_name?: string;
  language_code?: string;
  allows_write_to_pm?: boolean;
}

/**
 * محتوای {@code initDataUnsafe}.
 *
 * <p>🔴 <b>«Unsafe» یعنی تأییدنشده، نه «بی‌خطر».</b> این مقادیر از سمت
 * کلاینت می‌آیند و هر کاربری می‌تواند دست‌کاری‌شان کند. فقط برای
 * <i>راحتی UI</i> استفاده می‌شوند (مثلاً نمایش نام تا رسیدن پاسخ سرور).
 * هیچ‌گاه مبنای تصمیم دسترسی نیستند.
 */
export interface EitaaUnsafeData {
  user?: EitaaUnsafeUser;
  auth_date: number;
  hash: string;
  start_param?: string;
  chat_type?: 'private' | 'group' | 'channelOrSupergroup';
}

/** سطح کامل SDK — دقیقاً همان چیزی که مستندات تأیید کرده. */
export interface EitaaWebAppApi {
  readonly initData: string;
  readonly initDataUnsafe: EitaaUnsafeData;
  readonly version: string;
  readonly platform: string;
  readonly colorScheme: 'light' | 'dark';
  readonly themeParams: Record<string, string>;
  readonly isExpanded: boolean;
  readonly MainButton: BottomButton;
  readonly SecondaryButton: BottomButton;
  readonly BackButton: {
    show(): void;
    hide(): void;
    onClick(cb: () => void): void;
  };
  readonly HapticFeedback: {
    impactOccurred(style: 'light' | 'medium' | 'heavy' | 'rigid' | 'soft'): void;
    notificationOccurred(type: 'error' | 'success' | 'warning'): void;
    selectionChanged(): void;
  };

  ready(): void;
  expand(): void;
  close(): void;
  isVersionAtLeast(version: string): boolean;
  onEvent(type: string, handler: () => void): void;
  offEvent(type: string, handler: () => void): void;
  showAlert(message: string, cb?: () => void): void;
  showConfirm(message: string, cb?: (ok: boolean) => void): void;
  openLink(url: string, options?: { try_browser?: boolean }): void;
  openEitaaLink(url: string): void;
  requestWriteAccess(cb?: (granted: boolean) => void): void;
  requestContact(cb?: (shared: boolean, data?: string) => void): void;
}

declare global {
  interface Window {
    Eitaa?: { WebApp?: EitaaWebAppApi };
  }
}
