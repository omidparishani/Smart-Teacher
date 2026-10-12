import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { eitaa } from '../eitaa/sdk';
import { deepLinkPath, resolveStartParam } from './start-param';

/**
 * هدایت یک‌باره به مقصد Deep Link.
 *
 * <p>⚠️ فقط <b>یک بار</b> و فقط <b>پس از ورود</b> اجرا می‌شود:
 * <ul>
 *   <li><i>یک بار</i> — چون {@code start_param} تا پایان عمر WebView ثابت
 *       می‌ماند. اگر بدون محافظ باشد، هر بار رندر دوباره کاربر را به آن
 *       صفحه پرت می‌کند و او نمی‌تواند به زبانه دیگری برود.</li>
 *   <li><i>پس از ورود</i> — چون مقصدها صفحه‌های محافظت‌شده‌اند و اگر قبل
 *       از احراز هویت برویم، بلافاصله به صفحه ورود پرت می‌شویم.</li>
 * </ul>
 *
 * <p>🔴 اعتبارسنجی {@code start_param} اینجا هیچ تضمین دسترسی نمی‌دهد؛
 * بک‌اند مستقل بررسی می‌کند که آن منبع به مستجر جاری تعلق دارد.
 */
export function useStartParamRedirect(enabled: boolean): void {
  const navigate = useNavigate();
  const done = useRef(false);

  useEffect(() => {
    if (!enabled || done.current) {
      return;
    }

    done.current = true;

    const target = resolveStartParam(eitaa.startParam());

    if (target) {
      // ⚠️ `void` صریح: در React Router 7 این تابع یک Promise برمی‌گرداند
      // که منتظر ماندن روی آن اینجا معنی ندارد (کامپوننت در همان لحظه
      // دوباره رندر می‌شود)، ولی رها کردنش هم یک Floating Promise است.
      void navigate(deepLinkPath(target), { replace: true });
    }
  }, [enabled, navigate]);
}
