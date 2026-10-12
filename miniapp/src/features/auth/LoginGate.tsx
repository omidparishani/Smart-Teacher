import { useEffect, useMemo, type ReactNode } from 'react';
import { LogOut, RefreshCw, ShieldAlert } from 'lucide-react';
import { useAuthStore } from '../../stores/auth.store';
import { detectTheme, useUiStore } from '../../stores/ui.store';
import { eitaa } from '../../eitaa/sdk';
import { isDevInitDataInUse } from '../../eitaa/init-data';
import { checkPlatform } from '../../lib/platform-guard';
import { PlatformBlocked } from '../../components/shared/PlatformBlocked';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { Button } from '../../components/shared/Button';
import { useStartParamRedirect } from '../../router/use-start-param';
import { useSessionResume } from './use-session-resume';

interface LoginGateProps {
  children: ReactNode;
}

/**
 * دروازه ورود — ورود را راه می‌اندازد و تا کامل شدنش محتوا را نشان نمی‌دهد.
 *
 * <p>⚠️ <b>ترتیب بررسی‌ها عمدی است:</b>
 * <ol>
 *   <li>اول پلتفرم. اگر محیط پشتیبانی نمی‌شود، اصلاً درخواست ورود
 *       نمی‌فرستیم — نه اینکه بفرستیم و بعد خطا نشان دهیم.</li>
 *   <li>بعد وضعیت ورود.</li>
 * </ol>
 *
 * <p>سه حالت «مسدود»، «ناموفق» و «در جریان» از هم جدا نگه داشته شده‌اند:
 * فقط حالت «ناموفق» دکمه تلاش مجدد دارد، چون فقط آن حالت با تکرار درست
 * می‌شود.
 *
 * <p>🔴 <b>چرا حالت «مسدود» دکمه «بستن برنامک» دارد و «تلاش مجدد» ندارد؟</b>
 * چون {@code initData} داخل یک WebView <b>ثابت</b> است: تا وقتی برنامک
 * بسته و دوباره باز نشود، SDK همان رشته کهنه را برمی‌گرداند. پس تکرار
 * درخواست داخل برنامه، هر بار همان پاسخ را می‌گیرد — که دقیقاً همان حلقه
 * «دوباره تلاش کنین» بود که کاربر گزارش داد. تنها اقدامی که واقعاً
 * {@code initData} تازه می‌سازد، بستن و باز کردن برنامک است.
 */
export function LoginGate({ children }: LoginGateProps) {
  const status = useAuthStore((state) => state.status);
  const errorMessage = useAuthStore((state) => state.errorMessage);
  const bootstrap = useAuthStore((state) => state.bootstrap);
  const login = useAuthStore((state) => state.login);
  const setTheme = useUiStore((state) => state.setTheme);

  // ⚠️ یک بار در عمر برنامه — تشخیص پلتفرم به SDK وابسته است و آن هم
  // ثابت است. هر رندر دوباره، فقط هزینه دارد.
  const verdict = useMemo(() => checkPlatform(), []);

  /*
   * ⚠️ آیا می‌توان برنامک را بست؟ فقط اگر SDK ایتا واقعاً حاضر باشد.
   *
   * <p>در حالت `VITE_DEV_ALLOW_BROWSER` هیچ SDKای نیست، پس دکمه‌ای که
   * «برنامک را ببند» بگوید، دروغ است — و کاربر را به دکمه‌ای می‌فرستد که
   * هیچ کاری نمی‌کند.
   */
  const canCloseMiniApp = useMemo(() => eitaa.app !== undefined, []);

  // 🔴 تازه‌سازی هنگام بازگشت از پس‌زمینه — وگرنه توکن ۱۵ دقیقه‌ای در
  // پس‌زمینه می‌سوزد و کاربر با یک صفحه خطا برمی‌گردد.
  useSessionResume();

  useEffect(() => {
    // ⚠️ تم پیش از هر چیز اعمال می‌شود تا صفحه با رنگ درست ظاهر شود؛
    // اعمال دیرهنگام یک پرش رنگ می‌سازد که روی موبایل دیده می‌شود.
    //
    // 🔴 `detectTheme()` و نه وضعیت فعلی فروشگاه. نسخه قبلی
    // `useUiStore.getState().theme` را می‌خواند که همیشه `light` است —
    // یعنی `detectTheme` هیچ‌وقت صدا زده نمی‌شد و تنظیم رنگ ایتا
    // (`eitaa.colorScheme()`) کاملاً نادیده می‌ماند، برخلاف چیزی که
    // قرارداد خودِ فروشگاه و `docs/mini-app.md` بخش ۹ می‌گفتند.
    //
    // ⚠️ فقط یک بار در عمر برنامه: این افکت با mount شدن `LoginGate`
    // اجرا می‌شود و آن هم بالای درخت است، پس انتخاب بعدی کاربر (تنظیمات،
    // فاز ۴) را بازنویسی نمی‌کند.
    setTheme(detectTheme());

    if (verdict.ok) {
      void bootstrap();
    }
  }, [bootstrap, setTheme, verdict.ok]);

  useStartParamRedirect(status === 'authenticated');

  if (!verdict.ok) {
    return <PlatformBlocked message={verdict.message} />;
  }

  if (status === 'authenticated') {
    return <>{children}</>;
  }

  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-4 px-6">
      {status === 'unauthenticated' ? (
        <div className="w-full max-w-sm">
          <ErrorCard
            title="ورود انجام نشد"
            message={errorMessage ?? 'ارتباط با سرور برقرار نشد.'}
            action={
              /*
               * ⚠️ این دکمه فقط در حالت «ناموفق» معنا دارد — یعنی خطای
               * گذرا (شبکه، ۵xx، ۴۲۹) یا نشستِ مرده‌ای که با توکن تازه
               * قابل بازیابی است. خطاهای `initData` به حالت «مسدود»
               * می‌روند و اصلاً به اینجا نمی‌رسند.
               */
              <Button
                onClick={() => {
                  void login();
                }}
              >
                <RefreshCw className="size-4" aria-hidden="true" />
                تلاش مجدد
              </Button>
            }
          />
        </div>
      ) : null}

      {status === 'blocked' ? (
        <div className="w-full max-w-sm">
          <div
            role="alert"
            className="flex flex-col items-center gap-3 rounded-xl border border-warning/40 bg-warning/5 p-6 text-center"
          >
            <ShieldAlert className="size-8 text-warning" aria-hidden="true" />
            <h2 className="text-base font-medium">ورود ممکن نیست</h2>
            <p className="text-sm text-muted-foreground">
              {errorMessage ?? 'این برنامه باید از داخل ایتا باز شود.'}
            </p>
            {/*
              🔴 در ایتای واقعی: بستن برنامک تا SDK `initData` تازه بسازد.
              در توسعهٔ مرورگر: بستن بی‌فایده است — باید اسکریپت را دوباره
              اجرا کرد؛ دکمهٔ ایتا عمداً نشان داده نمی‌شود.
            */}
            {canCloseMiniApp && !isDevInitDataInUse() ? (
              <Button variant="secondary" onClick={() => eitaa.close()}>
                <LogOut className="size-4" aria-hidden="true" />
                بستن برنامک و باز کردن دوباره
              </Button>
            ) : null}
          </div>
        </div>
      ) : null}

      {status === 'idle' || status === 'authenticating' ? (
        <div className="w-full max-w-sm">
          <p className="mb-4 text-center text-sm text-muted-foreground">
            در حال ورود به دستیار هوشمند معلم…
          </p>
          <LoadingSkeleton lines={4} label="در حال ورود" />
        </div>
      ) : null}
    </div>
  );
}
