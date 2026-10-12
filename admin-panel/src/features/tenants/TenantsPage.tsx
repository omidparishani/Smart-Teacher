import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../../lib/api-client';
import type { AdminTenantResponse } from '../../types/api';
import { deleteTenant, fetchTenants, updateTenant } from '../admin/api';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input, Select } from '../../components/ui/Input';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';

const TYPE_LABEL: Record<AdminTenantResponse['type'], string> = {
  TEACHER: 'معلم',
  SCHOOL: 'مدرسه',
  ORGANIZATION: 'مؤسسه / سازمان',
};

const STATUS_CONFIG: Record<
  AdminTenantResponse['status'],
  { label: string; variant: 'success' | 'warning' | 'neutral' }
> = {
  ACTIVE: { label: 'فعال', variant: 'success' },
  SUSPENDED: { label: 'معلق', variant: 'warning' },
  ARCHIVED: { label: 'بایگانی', variant: 'neutral' },
};

export function TenantsPage() {
  const queryClient = useQueryClient();
  const [editTarget, setEditTarget] = useState<AdminTenantResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminTenantResponse | null>(null);

  const [editName, setEditName] = useState('');
  const [editStatus, setEditStatus] = useState<string>('ACTIVE');

  const query = useQuery({
    queryKey: ['admin', 'tenants'],
    queryFn: () => fetchTenants(0, 50),
  });

  const updateMutation = useMutation({
    mutationFn: () =>
      updateTenant(editTarget!.id, {
        name: editName.trim(),
        status: editStatus,
      }),
    onSuccess: () => {
      setEditTarget(null);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'tenants'] });
    },
    onError: (err) => {
      alert(err instanceof ApiError ? err.message : 'ویرایش مستأجر ناموفق بود.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteTenant(id),
    onSuccess: () => {
      setDeleteTarget(null);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'tenants'] });
    },
    onError: (err) => {
      alert(err instanceof ApiError ? err.message : 'حذف مستأجر ناموفق بود.');
    },
  });

  function startEdit(tenant: AdminTenantResponse) {
    setEditTarget(tenant);
    setEditName(tenant.name);
    setEditStatus(tenant.status);
  }

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
        {query.error instanceof ApiError ? query.error.message : 'بارگذاری مستأجرها ناموفق بود.'}
      </div>
    );
  }

  const tenants = query.data.content;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">مدیریت مستأجرها (Tenants)</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            مجموع {query.data.totalElements.toLocaleString('fa-IR')} مستأجر ثبت‌شده (معلمان، مدارس و سازمان‌ها)
          </p>
        </div>
      </div>

      <Card className="p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-right text-xs">
            <thead className="bg-slate-50 border-b border-slate-200/80 text-slate-600 font-bold">
              <tr>
                <th className="py-3 px-4">نام</th>
                <th className="py-3 px-4">شناسه یکتا (slug)</th>
                <th className="py-3 px-4">نوع مستأجر</th>
                <th className="py-3 px-4">وضعیت</th>
                <th className="py-3 px-4">تاریخ ایجاد</th>
                <th className="py-3 px-4 text-center">عملیات</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {tenants.map((tenant) => {
                const status = STATUS_CONFIG[tenant.status] || {
                  label: tenant.status,
                  variant: 'neutral' as const,
                };
                return (
                  <tr key={tenant.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="py-3.5 px-4 font-semibold text-slate-800">{tenant.name}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-600">{tenant.slug}</td>
                    <td className="py-3.5 px-4 text-slate-700">{TYPE_LABEL[tenant.type] ?? tenant.type}</td>
                    <td className="py-3.5 px-4">
                      <Badge variant={status.variant}>{status.label}</Badge>
                    </td>
                    <td className="py-3.5 px-4 font-mono text-slate-400">{tenant.createdAt?.slice(0, 10)}</td>
                    <td className="py-3.5 px-4 text-center">
                      <div className="flex items-center justify-center gap-1.5">
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-blue-600 hover:text-blue-700 hover:bg-blue-50"
                          onClick={() => startEdit(tenant)}
                        >
                          ویرایش
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-rose-600 hover:text-rose-700 hover:bg-rose-50"
                          onClick={() => setDeleteTarget(tenant)}
                        >
                          حذف
                        </Button>
                      </div>
                    </td>
                  </tr>
                );
              })}
              {tenants.length === 0 && (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-slate-400">
                    هیچ مستأجری یافت نشد.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Edit Tenant Modal */}
      <Modal
        isOpen={!!editTarget}
        onClose={() => setEditTarget(null)}
        title="ویرایش مستأجر"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            updateMutation.mutate();
          }}
          className="space-y-4"
        >
          <Input
            label="نام مستأجر"
            value={editName}
            onChange={(e) => setEditName(e.target.value)}
            required
          />
          <Select
            label="وضعیت فعالیت"
            value={editStatus}
            onChange={(e) => setEditStatus(e.target.value)}
            options={[
              { value: 'ACTIVE', label: 'فعال' },
              { value: 'SUSPENDED', label: 'معلق' },
              { value: 'ARCHIVED', label: 'بایگانی' },
            ]}
          />
          <div className="flex justify-end gap-2 pt-3">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setEditTarget(null)}
            >
              انصراف
            </Button>
            <Button
              type="submit"
              variant="primary"
              isLoading={updateMutation.isPending}
            >
              ذخیره تغییرات
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Tenant Modal */}
      <Modal
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        title="تأیید حذف مستأجر"
        maxWidth="sm"
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            آیا از حذف نرم مستأجر{' '}
            <strong className="text-slate-900 font-bold">{deleteTarget?.name}</strong> اطمینان
            دارید؟ وضعیت به معلق تغییر می‌یابد و تاریخچه حسابرسی ثبت خواهد شد.
          </p>
          <div className="flex justify-end gap-2 pt-2">
            <Button
              variant="secondary"
              onClick={() => setDeleteTarget(null)}
              disabled={deleteMutation.isPending}
            >
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
