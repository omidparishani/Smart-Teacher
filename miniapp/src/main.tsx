import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import { eitaa } from './eitaa/sdk';
import { wireAuthBridge } from './stores/auth.store';
// ⚠️ فونت خودمیزبان — لینک به CDN ممنوع است (docs/mini-app.md بخش ۲).
import '@fontsource-variable/vazirmatn';
import './index.css';

const container = document.getElementById('root');

if (!container) {
  // ⚠️ اگر اینجا بیفتد، `index.html` دستکاری شده است. سکوت کردن یعنی
  // کاربر یک صفحه کاملاً سفید می‌بیند بدون هیچ سرنخی.
  throw new Error('عنصر #root در index.html پیدا نشد.');
}

// ⚠️ پیش از رندر: به ایتا می‌گوییم آماده‌ایم و صفحه را تمام‌قد می‌کنیم.
// هر دو بی‌اثر هستند اگر SDK نباشد (مرورگر) — و خطا هم نمی‌دهند.
eitaa.ready();
eitaa.expand();

// ⚠️ پیش از هر درخواست شبکه‌ای: پل احراز هویت باید وصل باشد، وگرنه
// اولین تازه‌سازی توکن به حلقه بی‌پایان ۴۰۱ می‌خورد.
wireAuthBridge();

createRoot(container).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
