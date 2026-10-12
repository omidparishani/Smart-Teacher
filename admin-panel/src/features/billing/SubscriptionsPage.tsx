import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../../lib/api-client';
import type { AdminSubscriptionResponse } from '../../types/api';
import { cancelSubscription, extendSubscription, fetchSubscriptions } from '../admin/api';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';

const STATUS_CONFIG: Record<
  string,
  { label: string; variant: 'success' | 'warning' | 'danger' | 'neutral' }
> = {
  ACTIVE: { label: 'فعال', variant: 'success' },
  PENDING: { label: 'در انتظار پرداخت', variant: 'warning' },
  EXPIRED: { label: 'منقضی شده', variant: 'neutral' },
  CANCELLED: { label: 'لغو شده', variant: 'danger' },
};

export function SubscriptionsPage() {
  const queryClient = useQueryClient();
  const [cancelTarget, setCancelTarget] = useState<AdminSubscriptionResponse | null>(null);
  const [extendTarget, setExtendTarget] = useState<AdminSubscriptionResponse | null>(null);
  const [cancelReason, setCancelReason] = useState('');
  const [extendDays, setExtendDays] = useState('30');

  const query = useQuery({
    queryKey: ['admin', 'subscriptions'],
    queryFn: () => fetchSubscriptions(0, 100),
  });

  const cancelMutation = useMutation({
    mutationFn: () => cancelSubscription(cancelTarget!.id, cancelReason.trim()),
    onSuccess: () => {
      setCancelTarget(null);
      setCancelReason('');
      void queryClient.invalidateQueries({ queryKey: ['admin', 'subscriptions'] });
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'لغو اشتراک ناموفق بود.'),
  });

  const extendMutation = useMutation({
    mutationFn: () => extendSubscription(extendTarget!.id, Number(extendDays)),
    onSuccess: () => {
      setExtendTarget(null);
      setExtendDays('30');
      void queryClient.invalidateQueries({ queryKey: ['admin', 'subscriptions'] });
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'تمدید اشتراک ناموفق بود.'),
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
        {query.error instanceof ApiError ? query.error.message : 'بارگذاری اشتراک‌ها ناموفق بود.'}
      </div>
    );
  }

  const subscriptions: AdminSubscriptionResponse[] = query.data;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">مدیریت اشتراک‌های کاربران و مدارس</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            {subscriptions.length.toLocaleString('fa-IR')} اشتراک ثبت‌شده — امکان لغو مدیریتی یا تمدید دوره
          </p>
        </div>
      </div>

      <Card className="p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-right text-xs">
            <thead className="bg-slate-50 border-b border-slate-200/80 text-slate-600 font-bold">
              <tr>
                <th className="py-3 px-4">پلن اشتراک</th>
                <th className="py-3 px-4">وضعیت</th>
                <th className="py-3 px-4">تاریخ شروع</th>
                <th className="py-3 px-4">تاریخ پایان</th>
                <th className="py-3 px-4">مبلغ پرداختی (تومان)</th>
                <th className="py-3 px-4 text-center">عملیات مدیریتی</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {subscriptions.map((sub) => {
                const status = STATUS_CONFIG[sub.status] || {
                  label: sub.status,
                  variant: 'neutral' as const,
                };
                return (
                  <tr key={sub.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="py-3.5 px-4">
                      <p className="font-semibold text-slate-800">{sub.planName}</p>
                      <p className="font-mono text-[11px] text-slate-400 mt-0.5">{sub.planCode}</p>
                    </td>
                    <td className="py-3.5 px-4">
                      <Badge variant={status.variant}>{status.label}</Badge>
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-600">
                      {sub.startDate ? new Date(sub.startDate).toLocaleDateString('fa-IR') : '—'}
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-600">
                      {sub.endDate ? new Date(sub.endDate).toLocaleDateString('fa-IR') : '—'}
                    </td>
                    <td className="py-3.5 px-4 font-mono font-medium text-slate-800">
                      {sub.pricePaid != null ? Number(sub.pricePaid).toLocaleString('fa-IR') : '—'}
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <div className="flex items-center justify-center gap-1.5">
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-blue-600 hover:bg-blue-50"
                          onClick={() => setExtendTarget(sub)}
                        >
                          تمدید
                        </Button>
                        {sub.status === 'ACTIVE' && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-rose-600 hover:bg-rose-50"
                            onClick={() => setCancelTarget(sub)}
                          >
                            لغو
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
              {subscriptions.length === 0 && (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-400">
                    هیچ اشتراکی یافت نشد.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Cancel Modal */}
      <Modal
        isOpen={!!cancelTarget}
        onClose={() => setCancelTarget(null)}
        title="لغو اشتراک مدیریتی"
        maxWidth="sm"
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            آیا از لغو اشتراک <strong className="text-slate-900 font-bold">{cancelTarget?.planName}</strong> اطمینان
            دارید؟ دسترسی کاربر بلافاصله متوقف می‌شود.
          </p>
          <Input
            label="دلیل لغو (اختیاری جهت درج در لاگ)"
            value={cancelReason}
            onChange={(e) => setCancelReason(e.target.value)}
            placeholder="مثال: درخواست عودت وجه یا تخلف"
          />
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => setCancelTarget(null)}>
              انصراف
            </Button>
            <Button
              variant="danger"
              isLoading={cancelMutation.isPending}
              onClick={() => cancelMutation.mutate()}
            >
              تأیید و لغو اشتراک
            </Button>
          </div>
        </div>
      </Modal>

      {/* Extend Modal */}
      <Modal
        isOpen={!!extendTarget}
        onClose={() => setExtendTarget(null)}
        title="تمدید اشتراک"
        maxWidth="sm"
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            تمدید اشتراک <strong className="text-slate-900 font-bold">{extendTarget?.planName}</strong> به صورت هدیه یا
            توافق پشتیبانی.
          </p>
          <Input
            label="تعداد روزهای اضافه (روز)"
            type="number"
            min="1"
            value={extendDays}
            onChange={(e) => setExtendDays(e.target.value)}
            required
          />
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => setExtendTarget(null)}>
              انصراف
            </Button>
            <Button
              variant="primary"
              isLoading={extendMutation.isPending}
              onClick={() => extendMutation.mutate()}
            >
              اعمال تمدید
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
