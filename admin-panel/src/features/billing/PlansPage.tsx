import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../../lib/api-client';
import type { PlanResponse } from '../../types/api';
import { createPlan, deletePlan, disablePlan, fetchPlans, updatePlan } from '../admin/api';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';

export function PlansPage() {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<PlanResponse | null>(null);

  const [newCode, setNewCode] = useState('');
  const [newName, setNewName] = useState('');
  const [newPrice, setNewPrice] = useState('0');
  const [formError, setFormError] = useState<string | null>(null);

  const query = useQuery({
    queryKey: ['admin', 'plans'],
    queryFn: fetchPlans,
  });

  const createMutation = useMutation({
    mutationFn: () =>
      createPlan({
        code: newCode.trim(),
        name: newName.trim(),
        price: Number(newPrice),
        currency: 'IRT',
        billingPeriod: 'MONTHLY',
        isPublic: true,
      }),
    onSuccess: () => {
      setFormError(null);
      setShowCreate(false);
      setNewCode('');
      setNewName('');
      setNewPrice('0');
      void queryClient.invalidateQueries({ queryKey: ['admin', 'plans'] });
    },
    onError: (err) => setFormError(err instanceof ApiError ? err.message : 'ایجاد پلن ناموفق بود.'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, name, price }: { id: string; name: string; price: number }) =>
      updatePlan(id, { name, price }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'plans'] });
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'به‌روزرسانی ناموفق بود.'),
  });

  const disableMutation = useMutation({
    mutationFn: (id: string) => disablePlan(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'plans'] });
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'غیرفعال‌سازی ناموفق بود.'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deletePlan(id),
    onSuccess: () => {
      setDeleteTarget(null);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'plans'] });
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'حذف پلن ناموفق بود.'),
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
        {query.error instanceof ApiError ? query.error.message : 'بارگذاری پلن‌ها ناموفق بود.'}
      </div>
    );
  }

  const plans: PlanResponse[] = query.data;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">مدیریت پلن‌های مالی و اشتراک</h1>
          <p className="text-xs text-slate-500 mt-0.5">کاتالوگ پلن‌ها، مبالغ ماهانه و دسترسی به قابلیت‌ها</p>
        </div>
        <Button variant="primary" onClick={() => setShowCreate(true)}>
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
          </svg>
          تعریف پلن جدید
        </Button>
      </div>

      <Card className="p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-right text-xs">
            <thead className="bg-slate-50 border-b border-slate-200/80 text-slate-600 font-bold">
              <tr>
                <th className="py-3 px-4">کد سیستمی</th>
                <th className="py-3 px-4">عنوان پلن</th>
                <th className="py-3 px-4">قیمت ماهانه (تومان)</th>
                <th className="py-3 px-4">وضعیت</th>
                <th className="py-3 px-4 text-center">عملیات</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {plans.map((plan) => (
                <PlanRow
                  key={plan.id}
                  plan={plan}
                  onSave={(name, price) => updateMutation.mutate({ id: plan.id, name, price })}
                  onDisable={() => disableMutation.mutate(plan.id)}
                  onDelete={() => setDeleteTarget(plan)}
                  saving={updateMutation.isPending}
                />
              ))}
              {plans.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-slate-400">
                    هیچ پلنی یافت نشد.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Create Plan Modal */}
      <Modal
        isOpen={showCreate}
        onClose={() => setShowCreate(false)}
        title="تعریف پلن اشتراک جدید"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate();
          }}
          className="space-y-4"
        >
          <Input
            label="کد انگلیسی پلن"
            value={newCode}
            onChange={(e) => setNewCode(e.target.value)}
            placeholder="مثال: pro_monthly"
            required
          />
          <Input
            label="عنوان نمایشی پلن"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder="مثال: اشتراک حرفه‌ای دبیران"
            required
          />
          <Input
            label="قیمت (تومان)"
            type="number"
            min="0"
            value={newPrice}
            onChange={(e) => setNewPrice(e.target.value)}
            required
          />
          {formError && <p className="text-xs text-rose-600 font-medium">{formError}</p>}
          <div className="flex justify-end gap-2 pt-3">
            <Button type="button" variant="secondary" onClick={() => setShowCreate(false)}>
              انصراف
            </Button>
            <Button type="submit" variant="primary" isLoading={createMutation.isPending}>
              ایجاد پلن
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Plan Modal */}
      <Modal
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        title="تأیید حذف پلن"
        maxWidth="sm"
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            آیا از حذف کامل پلن <strong className="text-slate-900 font-bold">{deleteTarget?.name}</strong> اطمینان
            دارید؟ (در صورتی که پلن اشتراک فعال داشته باشد حذف نخواهد شد).
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

function PlanRow({
  plan,
  onSave,
  onDisable,
  onDelete,
  saving,
}: {
  plan: PlanResponse;
  onSave: (name: string, price: number) => void;
  onDisable: () => void;
  onDelete: () => void;
  saving: boolean;
}) {
  const [name, setName] = useState(plan.name);
  const [price, setPrice] = useState(String(plan.price));
  const isChanged = name !== plan.name || price !== String(plan.price);

  return (
    <tr className="hover:bg-slate-50/50 transition-colors">
      <td className="py-3.5 px-4 font-mono font-medium text-slate-700">{plan.code}</td>
      <td className="py-3.5 px-4">
        <input
          className="w-full max-w-xs px-2.5 py-1 text-xs bg-white border border-slate-200 rounded-md focus:border-blue-500 focus:outline-none"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
      </td>
      <td className="py-3.5 px-4 font-mono">
        <input
          type="number"
          min="0"
          className="w-32 px-2.5 py-1 text-xs bg-white border border-slate-200 rounded-md focus:border-blue-500 focus:outline-none"
          value={price}
          onChange={(e) => setPrice(e.target.value)}
        />
      </td>
      <td className="py-3.5 px-4">
        <Badge variant={plan.active ? 'success' : 'neutral'}>
          {plan.active ? 'فعال' : 'غیرفعال'}
        </Badge>
      </td>
      <td className="py-3.5 px-4 text-center">
        <div className="flex items-center justify-center gap-1.5">
          {isChanged && (
            <Button
              variant="primary"
              size="sm"
              disabled={saving}
              onClick={() => onSave(name.trim(), Number(price))}
            >
              ذخیره
            </Button>
          )}
          {plan.active && (
            <Button variant="ghost" size="sm" className="text-amber-600 hover:bg-amber-50" onClick={onDisable}>
              غیرفعال
            </Button>
          )}
          <Button variant="ghost" size="sm" className="text-rose-600 hover:bg-rose-50" onClick={onDelete}>
            حذف
          </Button>
        </div>
      </td>
    </tr>
  );
}
