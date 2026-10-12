import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ApiError } from '../../lib/api-client';
import { fetchAuditLogs } from '../admin/api';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';

export function AuditLogsPage() {
  const [page, setPage] = useState(0);
  const [tenantFilter, setTenantFilter] = useState('');

  const query = useQuery({
    queryKey: ['admin', 'audit-logs', page, tenantFilter],
    queryFn: () => fetchAuditLogs(page, 20, tenantFilter.trim() || undefined),
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
        {query.error instanceof ApiError ? query.error.message : 'بارگذاری لاگ‌ها ناموفق بود.'}
      </div>
    );
  }

  const logs = query.data.content;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">لاگ حسابرسی و رویدادهای امنیتی (Audit Logs)</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            ثبت غیرقابل‌دستکاری تمامی تغییرات، ورودها، اقدامات حساس و حذف‌ها در پایگاه داده
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Input
            placeholder="فیلتر بر اساس شناسه مستأجر..."
            value={tenantFilter}
            onChange={(e) => {
              setTenantFilter(e.target.value);
              setPage(0);
            }}
            className="w-64 text-xs"
          />
        </div>
      </div>

      <Card className="p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-right text-xs">
            <thead className="bg-slate-50 border-b border-slate-200/80 text-slate-600 font-bold">
              <tr>
                <th className="py-3 px-4">نوع رویداد</th>
                <th className="py-3 px-4">موجودیت</th>
                <th className="py-3 px-4">کاربر اقدام‌کننده (Actor)</th>
                <th className="py-3 px-4">مستأجر</th>
                <th className="py-3 px-4">آدرس IP</th>
                <th className="py-3 px-4">تاریخ و زمان</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {logs.map((log) => (
                <tr key={log.id} className="hover:bg-slate-50/50 transition-colors">
                  <td className="py-3.5 px-4">
                    <span className="font-mono font-bold text-slate-800 px-2 py-0.5 rounded-sm bg-slate-100 border border-slate-200 text-[11px]">
                      {log.eventType}
                    </span>
                  </td>
                  <td className="py-3.5 px-4 font-mono text-slate-700">
                    {log.entityType ? `${log.entityType}:${log.entityId?.slice(0, 8) ?? ''}` : '—'}
                  </td>
                  <td className="py-3.5 px-4 font-mono text-slate-500">
                    {log.actorId ? log.actorId.slice(0, 8) + '...' : 'سیستم'}
                  </td>
                  <td className="py-3.5 px-4 font-mono text-slate-500">
                    {log.tenantId ? log.tenantId.slice(0, 8) + '...' : 'سراسری'}
                  </td>
                  <td className="py-3.5 px-4 font-mono text-slate-500">{log.clientIp || '—'}</td>
                  <td className="py-3.5 px-4 font-mono text-slate-600">
                    {new Date(log.createdAt).toLocaleString('fa-IR')}
                  </td>
                </tr>
              ))}
              {logs.length === 0 && (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-400">
                    هیچ لاگی یافت نشد.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="flex items-center justify-between p-4 border-t border-slate-100 bg-slate-50/50 text-xs">
          <span className="text-slate-500">
            صفحه {page + 1} از {Math.max(1, query.data.totalPages)} ({query.data.totalElements} لاگ)
          </span>
          <div className="flex items-center gap-2">
            <Button
              variant="secondary"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              صفحه قبل
            </Button>
            <Button
              variant="secondary"
              size="sm"
              disabled={query.data.last}
              onClick={() => setPage((p) => p + 1)}
            >
              صفحه بعد
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
}
