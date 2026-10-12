import { Outlet } from 'react-router-dom';
import { TabBar } from './TabBar';
import { PlatformNotice } from './PlatformNotice';
import { DevDataBanner } from './DevDataBanner';

/**
 * پوسته برنامه — چیدمان مشترک همه صفحه‌های زبانه‌دار.
 *
 * <p>⚠️ <b>چرا {@code min-h-dvh} و نه {@code min-h-screen}؟</b> چون
 * {@code dvh} با نوار آدرس متغیر مرورگر کنار می‌آید؛ {@code 100vh} روی
 * موبایل باعث می‌شود پایین صفحه زیر نوار مرورگر بماند و اسکرول ناخواسته
 * بسازد.
 *
 * <p>⚠️ {@code pb-24} عمدی است: نوار پایین {@code fixed} است و اگر فاصله
 * نگذاریم، آخرین آیتم فهرست زیر آن پنهان می‌شود — باگی که فقط وقتی فهرست
 * به انتها می‌رسد دیده می‌شود.
 */
export function AppShell() {
  return (
    <div className="flex min-h-dvh flex-col bg-background">
      <PlatformNotice />
      <DevDataBanner />

      <main className="mx-auto w-full max-w-lg flex-1 px-4 pb-24 pt-4">
        <Outlet />
      </main>

      <TabBar />
    </div>
  );
}
