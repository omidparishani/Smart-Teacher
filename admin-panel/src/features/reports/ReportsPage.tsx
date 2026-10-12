import { useQuery } from '@tanstack/react-query';
import {
  fetchAiReport,
  fetchCharts,
  fetchRevenueReport,
  fetchSubscriptionsReport,
  fetchUsersReport,
} from '../admin/api';
import { useAuthStore } from '../../stores/auth.store';

const API_BASE: string = import.meta.env.VITE_API_URL ?? '/api/v1';

export function ReportsPage() {
  const users = useQuery({ queryKey: ['admin', 'reports', 'users'], queryFn: fetchUsersReport });
  const subscriptions = useQuery({
    queryKey: ['admin', 'reports', 'subscriptions'],
    queryFn: fetchSubscriptionsReport,
  });
  const ai = useQuery({ queryKey: ['admin', 'reports', 'ai'], queryFn: fetchAiReport });
  const revenue = useQuery({
    queryKey: ['admin', 'reports', 'revenue'],
    queryFn: fetchRevenueReport,
  });
  const charts = useQuery({
    queryKey: ['admin', 'reports', 'charts', '30d'],
    queryFn: () => fetchCharts('30d'),
  });

  const loading =
    users.isPending || subscriptions.isPending || ai.isPending || revenue.isPending || charts.isPending;
  const failed =
    users.isError || subscriptions.isError || ai.isError || revenue.isError || charts.isError;

  if (loading) {
    return <p className="text-muted">در حال بارگذاری گزارش‌ها…</p>;
  }

  if (failed) {
    return (
      <p className="text-danger" role="alert">
        بارگذاری گزارش‌ها ناموفق بود.
      </p>
    );
  }

  return (
    <div>
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">گزارش‌ها</h1>
          <p className="mt-1 text-sm text-muted">
            اعداد واقعی از جدول‌های موجود — بدون صفر جعلی برای دامنه‌های غایب.
          </p>
        </div>
        <div className="flex gap-3">
          <button
            type="button"
            className="text-sm text-primary underline-offset-2 hover:underline"
            onClick={() => void downloadUsersCsv()}
          >
            CSV کاربران
          </button>
          <button
            type="button"
            className="text-sm text-primary underline-offset-2 hover:underline"
            onClick={() => void downloadUsersXlsx()}
          >
            Excel کاربران
          </button>
        </div>
      </div>

      <section className="mt-8">
        <h2 className="text-lg font-medium">کاربران</h2>
        <div className="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <Stat label="کل" value={users.data!.total} />
          {Object.entries(users.data!.byStatus).map(([status, count]) => (
            <Stat key={status} label={status} value={count} />
          ))}
        </div>
      </section>

      <section className="mt-8">
        <h2 className="text-lg font-medium">اشتراک‌ها</h2>
        <div className="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <Stat label="کل" value={subscriptions.data!.total} />
          {Object.entries(subscriptions.data!.byStatus).map(([status, count]) => (
            <Stat key={status} label={status} value={count} />
          ))}
          <Stat
            label="نرخ ریزش تقریبی"
            valueLabel={`${((subscriptions.data!.churnRateApprox ?? 0) * 100).toFixed(1)}٪`}
          />
          <Stat
            label="نرخ تبدیل تقریبی"
            valueLabel={`${((subscriptions.data!.conversionRateApprox ?? 0) * 100).toFixed(1)}٪`}
          />
        </div>
      </section>

      <section className="mt-8">
        <h2 className="text-lg font-medium">مصرف AI</h2>
        <div className="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          <Stat label="درخواست‌ها" value={ai.data!.requestCount} />
          <Stat label="توکن ورودی" value={ai.data!.promptTokens} />
          <Stat label="توکن خروجی" value={ai.data!.completionTokens} />
          <Stat label="هزینه" value={Number(ai.data!.totalCost)} />
        </div>
        {(ai.data!.byModel?.length ?? 0) > 0 ? (
          <div className="mt-4">
            <h3 className="text-sm font-medium text-muted">هزینه به تفکیک مدل</h3>
            <SimpleBars
              items={(ai.data!.byModel ?? []).map((m) => ({
                label: m.model,
                value: Number(m.totalCost),
              }))}
            />
          </div>
        ) : null}
        {(ai.data!.byUser?.length ?? 0) > 0 ? (
          <div className="mt-4 overflow-x-auto border border-line bg-white/80">
            <h3 className="border-b border-line px-3 py-2 text-sm font-medium text-muted">
              برترین کاربران (هزینه AI)
            </h3>
            <table className="min-w-full text-sm">
              <thead className="border-b border-line text-right text-muted">
                <tr>
                  <th className="px-3 py-2 font-medium">کاربر</th>
                  <th className="px-3 py-2 font-medium">درخواست</th>
                  <th className="px-3 py-2 font-medium">هزینه</th>
                </tr>
              </thead>
              <tbody>
                {(ai.data!.byUser ?? []).slice(0, 10).map((u) => (
                  <tr key={u.userId} className="border-b border-line/70 last:border-0">
                    <td className="px-3 py-2">{u.userName}</td>
                    <td className="px-3 py-2 tabular-nums">{u.requestCount}</td>
                    <td className="px-3 py-2 tabular-nums">{Number(u.totalCost).toLocaleString('fa-IR')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>

      <section className="mt-8">
        <h2 className="text-lg font-medium">درآمد (فاکتور PAID)</h2>
        <div className="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          <Stat label="تعداد پرداخت‌شده" value={revenue.data!.paidInvoiceCount} />
          <Stat label="جمع مبلغ" value={Number(revenue.data!.paidTotal)} />
          <Stat label="ارز" valueLabel={revenue.data!.currency ?? '—'} />
        </div>
      </section>

      <section className="mt-8">
        <h2 className="text-lg font-medium">نمودار ۳۰ روز</h2>
        <div className="mt-4 grid gap-6 lg:grid-cols-2 xl:grid-cols-4">
          <SeriesBars title="کاربران جدید" series={charts.data!.newUsers} />
          <SeriesBars title="مستأجر جدید" series={charts.data!.newTenants} />
          <SeriesBars title="اشتراک جدید" series={charts.data!.subscriptions} />
          {(charts.data!.revenue?.length ?? 0) > 0 ? (
            <RevenueBars title="درآمد روزانه" series={charts.data!.revenue!} />
          ) : null}
        </div>
      </section>
    </div>
  );
}

function Stat({
  label,
  value,
  valueLabel,
}: {
  label: string;
  value?: number;
  valueLabel?: string;
}) {
  return (
    <div className="border border-line bg-white/80 px-4 py-5">
      <p className="text-sm text-muted">{label}</p>
      <p className="mt-2 text-3xl font-semibold tabular-nums">
        {valueLabel ?? (value ?? 0).toLocaleString('fa-IR')}
      </p>
    </div>
  );
}

function SeriesBars({
  title,
  series,
}: {
  title: string;
  series: { date: string; count: number }[];
}) {
  const last14 = series.slice(-14);
  return (
    <div>
      <h3 className="mb-2 text-sm font-medium">{title}</h3>
      <SimpleBars items={last14.map((d) => ({ label: d.date.slice(5), value: d.count }))} />
    </div>
  );
}

function RevenueBars({
  title,
  series,
}: {
  title: string;
  series: { date: string; total: number | string }[];
}) {
  const last14 = series.slice(-14);
  return (
    <div>
      <h3 className="mb-2 text-sm font-medium">{title}</h3>
      <SimpleBars
        items={last14.map((d) => ({
          label: d.date.slice(5),
          value: Number(d.total),
        }))}
      />
    </div>
  );
}

function SimpleBars({ items }: { items: { label: string; value: number }[] }) {
  const max = Math.max(1, ...items.map((i) => i.value));
  return (
    <div className="flex h-28 items-end gap-1 border border-line bg-white/60 p-2" role="img">
      {items.map((item) => (
        <div key={item.label} className="flex flex-1 flex-col items-center justify-end gap-1">
          <div
            className="w-full rounded-sm bg-primary/70"
            style={{ height: `${Math.max(2, (item.value / max) * 100)}%` }}
            title={`${item.label}: ${item.value}`}
          />
        </div>
      ))}
    </div>
  );
}

async function downloadUsersCsv() {
  await downloadReport('/admin/reports/users.csv', 'users-report.csv');
}

async function downloadUsersXlsx() {
  await downloadReport('/admin/reports/users.xlsx', 'users-report.xlsx');
}

async function downloadReport(path: string, filename: string) {
  const token = useAuthStore.getState().accessToken;
  const res = await fetch(`${API_BASE}${path}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    return;
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
