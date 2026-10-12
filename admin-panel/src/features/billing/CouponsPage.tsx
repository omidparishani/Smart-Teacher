import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../../lib/api-client';
import type { CouponResponse } from '../../types/api';
import { createCoupon, deleteCoupon, disableCoupon, enableCoupon, fetchCoupons } from '../admin/api';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input, Select } from '../../components/ui/Input';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';

export function CouponsPage() {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<CouponResponse | null>(null);

  const [code, setCode] = useState('');
  const [discountType, setDiscountType] = useState<'PERCENTAGE' | 'FIXED'>('PERCENTAGE');
  const [discountValue, setDiscountValue] = useState('10');
  const [usageLimit, setUsageLimit] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const query = useQuery({
    queryKey: ['admin', 'coupons'],
    queryFn: fetchCoupons,
  });

  const createMutation = useMutation({
    mutationFn: () => {
      const now = new Date();
      const until = new Date(now);
      until.setDate(until.getDate() + 30);
      return createCoupon({
        code: code.trim().toUpperCase(),
        discountType,
        discountValue: Number(discountValue),
        validFrom: now.toISOString(),
        validUntil: until.toISOString(),
        usageLimit: usageLimit ? Number(usageLimit) : undefined,
      });
    },
    onSuccess: () => {
      setFormError(null);
      setShowCreate(false);
      setCode('');
      setDiscountValue('10');
      setUsageLimit('');
      void queryClient.invalidateQueries({ queryKey: ['admin', 'coupons'] });
    },
    onError: (err) => setFormError(err instanceof ApiError ? err.message : 'ایجاد کوپن ناموفق بود.'),
  });

  const enableMutation = useMutation({
    mutationFn: (id: string) => enableCoupon(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'coupons'] });
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'فعال‌سازی ناموفق بود.'),
  });

  const disableMutation = useMutation({
    mutationFn: (id: string) => disableCoupon(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'coupons'] });
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'غیرفعال‌سازی ناموفق بود.'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteCoupon(id),
    onSuccess: () => {
      setDeleteTarget(null);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'coupons'] });
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'حذف کوپن ناموفق بود.'),
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
        {query.error instanceof ApiError ? query.error.message : 'بارگذاری کوپن‌ها ناموفق بود.'}
      </div>
    );
  }

  const coupons: CouponResponse[] = query.data;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">کوپن‌های تخفیف و پروموشن</h1>
          <p className="text-xs text-slate-500 mt-0.5">مدیریت کدهای تخفیف، محدودیت‌های سقف مصرف و تاریخ انقضا</p>
        </div>
        <Button variant="primary" onClick={() => setShowCreate(true)}>
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
          </svg>
          کوپن جدید
        </Button>
      </div>

      <Card className="p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-right text-xs">
            <thead className="bg-slate-50 border-b border-slate-200/80 text-slate-600 font-bold">
              <tr>
                <th className="py-3 px-4">کد تخفیف</th>
                <th className="py-3 px-4">نوع محاسبه</th>
                <th className="py-3 px-4">میزان تخفیف</th>
                <th className="py-3 px-4">تعداد مصرف‌شده</th>
                <th className="py-3 px-4">وضعیت</th>
                <th className="py-3 px-4 text-center">عملیات</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {coupons.map((coupon) => (
                <tr key={coupon.id} className="hover:bg-slate-50/50 transition-colors">
                  <td className="py-3.5 px-4 font-mono font-bold text-slate-800 tracking-wider">
                    {coupon.code}
                  </td>
                  <td className="py-3.5 px-4 text-slate-600">
                    {coupon.discountType === 'PERCENTAGE' ? 'درصدی' : 'مبلغ ثابت'}
                  </td>
                  <td className="py-3.5 px-4 font-mono text-slate-800 font-semibold">
                    {coupon.discountType === 'PERCENTAGE'
                      ? `%${coupon.discountValue}`
                      : `${Number(coupon.discountValue).toLocaleString('fa-IR')} تومان`}
                  </td>
                  <td className="py-3.5 px-4 font-mono text-slate-600">
                    {coupon.usedCount}
                    {coupon.usageLimit != null ? ` / ${coupon.usageLimit}` : ' (نامحدود)'}
                  </td>
                  <td className="py-3.5 px-4">
                    <Badge variant={coupon.active ? 'success' : 'neutral'}>
                      {coupon.active ? 'فعال' : 'غیرفعال'}
                    </Badge>
                  </td>
                  <td className="py-3.5 px-4 text-center">
                    <div className="flex items-center justify-center gap-1.5">
                      {coupon.active ? (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-amber-600 hover:bg-amber-50"
                          onClick={() => disableMutation.mutate(coupon.id)}
                        >
                          غیرفعال
                        </Button>
                      ) : (
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-emerald-600 hover:bg-emerald-50"
                          onClick={() => enableMutation.mutate(coupon.id)}
                        >
                          فعال‌سازی
                        </Button>
                      )}
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-rose-600 hover:bg-rose-50"
                        onClick={() => setDeleteTarget(coupon)}
                      >
                        حذف
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
              {coupons.length === 0 && (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-400">
                    هیچ کوپنی یافت نشد.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Create Coupon Modal */}
      <Modal
        isOpen={showCreate}
        onClose={() => setShowCreate(false)}
        title="تعریف کوپن تخفیف جدید"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate();
          }}
          className="space-y-4"
        >
          <Input
            label="کد تخفیف"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            placeholder="مثال: NOROOZ1405"
            required
          />
          <Select
            label="نوع تخفیف"
            value={discountType}
            onChange={(e) => setDiscountType(e.target.value as 'PERCENTAGE' | 'FIXED')}
            options={[
              { value: 'PERCENTAGE', label: 'درصدی (%)' },
              { value: 'FIXED', label: 'مبلغ ثابت (تومان)' },
            ]}
          />
          <Input
            label={discountType === 'PERCENTAGE' ? 'درصد تخفیف (۱ تا ۱۰۰)' : 'مبلغ تخفیف (تومان)'}
            type="number"
            min="1"
            value={discountValue}
            onChange={(e) => setDiscountValue(e.target.value)}
            required
          />
          <Input
            label="سقف تعداد استفاده (اختیاری)"
            type="number"
            min="1"
            value={usageLimit}
            onChange={(e) => setUsageLimit(e.target.value)}
            placeholder="خالی بگذارید تا نامحدود باشد"
          />
          {formError && <p className="text-xs text-rose-600 font-medium">{formError}</p>}
          <div className="flex justify-end gap-2 pt-3">
            <Button type="button" variant="secondary" onClick={() => setShowCreate(false)}>
              انصراف
            </Button>
            <Button type="submit" variant="primary" isLoading={createMutation.isPending}>
              ثبت کوپن
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Coupon Modal */}
      <Modal
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        title="تأیید حذف کوپن"
        maxWidth="sm"
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            آیا از حذف کوپن تخفیف <strong className="text-slate-900 font-bold">{deleteTarget?.code}</strong> اطمینان
            دارید؟ (در صورتی که قبلاً استفاده شده باشد، پیشنهاد می‌شود صرفاً آن را غیرفعال کنید).
          </p>
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => setDeleteTarget(null)}>
              انصراف
            </Button>
            <Button
              variant="danger"
              isLoading={deleteMutation.isPending}
              onClick={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
            >
              حذف قطعی
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
