import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchCharts, fetchDashboard, fetchRevenueReport, fetchEitaaHealth } from '../admin/api';
import { Card, CardHeader } from '../../components/ui/Card';
import { StatCard } from '../../components/ui/StatCard';
import { AreaChart, BarChart } from '../../components/ui/Chart';
import { Badge } from '../../components/ui/Badge';

export function DashboardPage() {
  const [period, setPeriod] = useState<'7d' | '30d'>('7d');

  const dashboard = useQuery({
    queryKey: ['admin', 'dashboard'],
    queryFn: fetchDashboard,
  });

  const revenue = useQuery({
    queryKey: ['admin', 'revenue-summary'],
    queryFn: fetchRevenueReport,
  });

  const charts = useQuery({
    queryKey: ['admin', 'dashboard-charts', period],
    queryFn: () => fetchCharts(period),
  });

  const eitaaHealth = useQuery({
    queryKey: ['admin', 'eitaa-health'],
    queryFn: fetchEitaaHealth,
  });

  if (dashboard.isPending || revenue.isPending) {
    return (
      <div className="flex items-center justify-center min-h-[50vh]">
        <div className="flex flex-col items-center gap-3">
          <div className="w-8 h-8 border-3 border-blue-600 border-t-transparent rounded-full animate-spin" />
          <p className="text-sm text-slate-500 font-medium">در حال بارگذاری داشبورد مدیریت...</p>
        </div>
      </div>
    );
  }

  if (dashboard.isError || revenue.isError) {
    return (
      <div className="p-6 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-sm">
        خطا در دریافت اطلاعات داشبورد. لطفاً اتصال به سرور را بررسی کنید.
      </div>
    );
  }

  const data = dashboard.data;
  const revData = revenue.data;

  // Chart series formatting
  const userGrowthData = (charts.data?.newUsers ?? []).map((d) => ({
    label: d.date.slice(5),
    value: d.count,
  }));

  const revenueSeries = (charts.data?.revenue ?? []).map((d) => ({
    label: d.date.slice(5),
    value: Number(d.total),
  }));

  const subGrowthData = (charts.data?.subscriptions ?? []).map((d) => ({
    label: d.date.slice(5),
    value: d.count,
  }));

  return (
    <div className="space-y-8 animate-in fade-in duration-300">
      {/* Top Welcome & Period Filter */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-black text-slate-900 tracking-tight">پیشخوان مدیریت کل</h1>
          <p className="text-xs text-slate-500 mt-1">نمای کلی فعالیت‌ها، فروش، اشتراک‌ها و وضعیت خدمات هوش مصنوعی</p>
        </div>
        <div className="flex items-center gap-2 bg-white p-1 rounded-xl border border-slate-200 shadow-xs">
          <button
            type="button"
            onClick={() => setPeriod('7d')}
            className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-all ${
              period === '7d'
                ? 'bg-blue-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            ۷ روز گذشته
          </button>
          <button
            type="button"
            onClick={() => setPeriod('30d')}
            className={`px-3 py-1.5 text-xs font-semibold rounded-lg transition-all ${
              period === '30d'
                ? 'bg-blue-600 text-white shadow-xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            ۳۰ روز گذشته
          </button>
        </div>
      </div>

      {/* KPI Stat Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="کل کاربران"
          value={data.users.total.toLocaleString('fa-IR')}
          description={`${data.users.active.toLocaleString('fa-IR')} کاربر فعال`}
          color="blue"
          icon={
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
          }
        />
        <StatCard
          title="مؤسسات و مدارس"
          value={data.tenants.total.toLocaleString('fa-IR')}
          description="مستأجرهای فعال در سامانه"
          color="violet"
          icon={
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
          }
        />
        <StatCard
          title="کلاس‌ها و دانش‌آموزان"
          value={data.education.classrooms.toLocaleString('fa-IR')}
          description={`${data.education.students.toLocaleString('fa-IR')} دانش‌آموز ثبت‌شده`}
          color="emerald"
          icon={
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          }
        />
        <StatCard
          title="درآمد کل محقق‌شده"
          value={Number(revData?.paidTotal ?? 0).toLocaleString('fa-IR')}
          description={`${revData?.paidInvoiceCount ?? 0} تراکنش موفق (ریال)`}
          color="amber"
          icon={
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          }
        />
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* User Growth Chart */}
        <Card>
          <CardHeader
            title="روند ثبت‌نام کاربران جدید"
            subtitle={`روند جذب کاربر در دوره ${period === '7d' ? 'هفتگی' : 'ماهانه'}`}
            action={<Badge variant="info">کاربران</Badge>}
          />
          <AreaChart
            data={userGrowthData}
            height={200}
            color="#2563eb"
            fillColor="#3b82f6"
          />
        </Card>

        {/* Revenue / Subscriptions Chart */}
        <Card>
          <CardHeader
            title="درآمد و اشتراک‌ها"
            subtitle="حجم مالی فاکتورهای پرداخت‌شده"
            action={<Badge variant="success">درآمد</Badge>}
          />
          {revenueSeries.length > 0 ? (
            <BarChart
              data={revenueSeries}
              height={200}
              barColor="#10b981"
            />
          ) : (
            <AreaChart
              data={subGrowthData}
              height={200}
              color="#10b981"
              fillColor="#34d399"
            />
          )}
        </Card>
      </div>

      {/* System Status & Eitaa Integration Glance */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Eitaa Card */}
        <Card className="lg:col-span-2">
          <CardHeader
            title="وضعیت اتصال به پیام‌رسان ایتا"
            subtitle="پل ارتباطی اعلان‌ها، ارسال پیام و مینی‌اپلیکیشن معلم"
            action={
              <Badge variant={eitaaHealth.data?.status === 'UP' ? 'success' : 'warning'}>
                {eitaaHealth.data?.status === 'UP' ? 'ارتباط برقرار' : 'نیازمند بررسی'}
              </Badge>
            }
          />
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 bg-slate-50/70 rounded-xl border border-slate-100">
            <div>
              <p className="text-xs text-slate-500 font-medium">ربات بله/ایتا</p>
              <p className="text-sm font-bold text-slate-800 mt-1">
                {eitaaHealth.data?.botConfigured ? 'پیکربندی‌شده' : 'فاقد توکن'}
              </p>
            </div>
            <div>
              <p className="text-xs text-slate-500 font-medium">مینی‌اپلیکیشن</p>
              <p className="text-sm font-bold text-slate-800 mt-1">
                {eitaaHealth.data?.appConfigured ? 'فعال' : 'فاقد تنظیم'}
              </p>
            </div>
            <div>
              <p className="text-xs text-slate-500 font-medium">کانال‌های متصل</p>
              <p className="text-sm font-bold text-slate-800 mt-1 font-mono">
                {eitaaHealth.data?.verifiedChannelsCount ?? 0}
              </p>
            </div>
            <div>
              <p className="text-xs text-slate-500 font-medium">گروه‌های متصل</p>
              <p className="text-sm font-bold text-slate-800 mt-1 font-mono">
                {eitaaHealth.data?.verifiedGroupsCount ?? 0}
              </p>
            </div>
          </div>
        </Card>

        {/* Security / System Summary */}
        <Card>
          <CardHeader
            title="امنیت و پایداری"
            subtitle="بررسی معماری سیستم"
          />
          <ul className="space-y-3 text-xs text-slate-600">
            <li className="flex items-center justify-between">
              <span>محدودیت نرخ (Rate Limit)</span>
              <span className="text-emerald-600 font-semibold">فعال (IP/مسیر)</span>
            </li>
            <li className="flex items-center justify-between">
              <span>نگه‌بان ضد بازپخش</span>
              <span className="text-emerald-600 font-semibold">فعال (ردیس/حافظه)</span>
            </li>
            <li className="flex items-center justify-between">
              <span>جداسازی داده مستأجران</span>
              <span className="text-blue-600 font-semibold">تضمین‌شده (TenantAware)</span>
            </li>
            <li className="flex items-center justify-between">
              <span>قوانین امنیتی نقش‌ها</span>
              <span className="text-blue-600 font-semibold">مجوزهای PreAuthorize</span>
            </li>
          </ul>
        </Card>
      </div>
    </div>
  );
}
