import { useQuery } from '@tanstack/react-query';
import { ApiError } from '../../lib/api-client';
import { fetchEitaaHealth } from '../admin/api';
import { Card, CardHeader } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';

export function EitaaPage() {
  const query = useQuery({
    queryKey: ['admin', 'eitaa-health'],
    queryFn: fetchEitaaHealth,
  });

  if (query.isPending) {
    return (
      <div className="flex items-center justify-center min-h-[40vh]">
        <div className="w-8 h-8 border-3 border-blue-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (query.isError) {
    return (
      <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-sm">
        {query.error instanceof ApiError ? query.error.message : 'دریافت وضعیت ایتا ناموفق بود.'}
      </div>
    );
  }

  const data = query.data;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">وضعیت اتصال به پیام‌رسان ایتا (Eitaa Integration)</h1>
        <p className="text-xs text-slate-500 mt-0.5">
          مدیریت یکپارچه‌سازی با ربات ایتا، وب‌هوک‌ها، مینی‌اپلیکیشن معلم و کانال‌های اطلاع‌رسانی
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-4 bg-white rounded-xl border border-slate-200 shadow-xs">
          <p className="text-xs text-slate-500 font-semibold">وضعیت کلی سرویس</p>
          <div className="mt-2 flex items-center justify-between">
            <span className="text-base font-bold text-slate-800 font-mono">{data.status}</span>
            <Badge variant={data.status === 'UP' ? 'success' : 'warning'}>
              {data.status === 'UP' ? 'سالم و فعال' : 'نیازمند بررسی'}
            </Badge>
          </div>
        </div>
        <div className="p-4 bg-white rounded-xl border border-slate-200 shadow-xs">
          <p className="text-xs text-slate-500 font-semibold">توکن ربات (Bot Token)</p>
          <div className="mt-2 flex items-center justify-between">
            <span className="text-sm font-bold text-slate-800">
              {data.botConfigured ? 'پیکربندی‌شده' : 'فاقد کلید'}
            </span>
            <Badge variant={data.botConfigured ? 'success' : 'danger'}>
              {data.botConfigured ? 'متصل' : 'قطع'}
            </Badge>
          </div>
        </div>
        <div className="p-4 bg-white rounded-xl border border-slate-200 shadow-xs">
          <p className="text-xs text-slate-500 font-semibold">توکن مینی‌اپلیکیشن (MiniApp)</p>
          <div className="mt-2 flex items-center justify-between">
            <span className="text-sm font-bold text-slate-800">
              {data.appConfigured ? 'پیکربندی‌شده' : 'فاقد کلید'}
            </span>
            <Badge variant={data.appConfigured ? 'success' : 'warning'}>
              {data.appConfigured ? 'فعال' : 'غیرفعال'}
            </Badge>
          </div>
        </div>
        <div className="p-4 bg-white rounded-xl border border-slate-200 shadow-xs">
          <p className="text-xs text-slate-500 font-semibold">حالت شبیه‌سازی (Mock Mode)</p>
          <div className="mt-2 flex items-center justify-between">
            <span className="text-sm font-bold text-slate-800">
              {data.mockMode ? 'حالت Mock فعال' : 'ارتباط واقعی فعال'}
            </span>
            <Badge variant={data.mockMode ? 'warning' : 'success'}>
              {data.mockMode ? 'شبیه‌ساز' : 'زنده (Production)'}
            </Badge>
          </div>
        </div>
      </div>

      <Card className="space-y-4">
        <CardHeader
          title="راهنمای راه‌اندازی اتصال واقعی ربات به کانال و گروه ایتا"
          subtitle="مراحل گام‌به‌گام اتصال طبق حقایق قطعی ایتا"
        />
        <div className="space-y-3 text-xs text-slate-700 leading-relaxed">
          <div className="p-3 bg-blue-50 border border-blue-200 rounded-lg text-blue-900">
            <strong>نکته مهم امنیتی و معماری:</strong> طبق مستندات و زیرساخت رسمی eitaayar.ir، ربات ایتا فاقد متدهای
            inbound، webhook و متدهای دریافت اطلاعات مانند getChatMember است. بنابراین تنها راه قطعی راستی‌آزمایی
            دسترسی ربات، ارسال یک پیام آزمایشی کنترل‌شده است.
          </div>
          <ol className="list-decimal list-inside space-y-2 pr-2">
            <li>
              <strong>عضویت و ادمین کردن ربات در کانال یا گروه:</strong> ربات ساخته‌شده در بات‌فادر ایتا را به کانال یا
              گروه مدرسه اضافه کرده و دسترسی ارسال پیام (Admin) را به آن بدهید.
            </li>
            <li>
              <strong>یافتن Chat ID:</strong> شناسه عددی گروه یا کانال (یا آیدی با پیشوند @ مانند{' '}
              <code className="bg-slate-100 px-1 py-0.5 rounded font-mono">@my_school_channel</code>) را در تنظیمات
              مدرسه ثبت کنید.
            </li>
            <li>
              <strong>تنظیم متغیر محیطی:</strong> کلید <code className="bg-slate-100 px-1 py-0.5 rounded font-mono">APP_EITAA_BOT_TOKEN</code>{' '}
              را در فایل پیکربندی سرور درج نمایید.
            </li>
            <li>
              <strong>ارسال آزمایشی:</strong> با تأیید نهایی، پیام راستی‌آزمایی از پنل ارسال می‌شود تا تیک سبز اتصال
              ثبت گردد.
            </li>
          </ol>
        </div>
      </Card>
    </div>
  );
}
