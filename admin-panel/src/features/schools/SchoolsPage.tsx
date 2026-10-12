import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../../lib/api-client';
import type { AdminSchoolResponse } from '../../types/api';
import { createSchool, deleteSchool, fetchSchools } from '../admin/api';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';

const STATUS_CONFIG: Record<
  AdminSchoolResponse['status'],
  { label: string; variant: 'success' | 'warning' | 'neutral' }
> = {
  ACTIVE: { label: 'فعال', variant: 'success' },
  SUSPENDED: { label: 'معلق', variant: 'warning' },
  ARCHIVED: { label: 'بایگانی', variant: 'neutral' },
};

export function SchoolsPage() {
  const queryClient = useQueryClient();
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<AdminSchoolResponse | null>(null);

  const [name, setName] = useState('');
  const [slug, setSlug] = useState('');
  const [ownerUserId, setOwnerUserId] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const query = useQuery({
    queryKey: ['admin', 'schools'],
    queryFn: () => fetchSchools(0, 50),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      createSchool({
        name: name.trim(),
        slug: slug.trim(),
        ownerUserId: ownerUserId.trim(),
      }),
    onSuccess: () => {
      setName('');
      setSlug('');
      setOwnerUserId('');
      setFormError(null);
      setIsCreateOpen(false);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'schools'] });
    },
    onError: (err) => {
      setFormError(err instanceof ApiError ? err.message : 'ساخت مدرسه ناموفق بود.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteSchool(id),
    onSuccess: () => {
      setDeleteTarget(null);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'schools'] });
    },
    onError: (err) => {
      alert(err instanceof ApiError ? err.message : 'حذف مدرسه ناموفق بود.');
    },
  });

  function handleCreateSubmit(e: React.FormEvent) {
    e.preventDefault();
    setFormError(null);
    createMutation.mutate();
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
        {query.error instanceof ApiError ? query.error.message : 'بارگذاری مدارس ناموفق بود.'}
      </div>
    );
  }

  const schools = query.data.content;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">مدیریت مدارس</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            مجموع {query.data.totalElements.toLocaleString('fa-IR')} مدرسه ثبت‌شده در سامانه
          </p>
        </div>
        <Button variant="primary" onClick={() => setIsCreateOpen(true)}>
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
          </svg>
          ثبت مدرسه جدید
        </Button>
      </div>

      <Card className="p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-right text-xs">
            <thead className="bg-slate-50 border-b border-slate-200/80 text-slate-600 font-bold">
              <tr>
                <th className="py-3 px-4">نام مدرسه</th>
                <th className="py-3 px-4">شناسه یکتا (slug)</th>
                <th className="py-3 px-4">مالک (شناسه کاربری)</th>
                <th className="py-3 px-4">وضعیت</th>
                <th className="py-3 px-4 text-center">عملیات</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {schools.map((school) => {
                const status = STATUS_CONFIG[school.status] || {
                  label: school.status,
                  variant: 'neutral' as const,
                };
                return (
                  <tr key={school.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="py-3.5 px-4 font-semibold text-slate-800">{school.name}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-600">{school.slug}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-500">{school.ownerUserId}</td>
                    <td className="py-3.5 px-4">
                      <Badge variant={status.variant}>{status.label}</Badge>
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-rose-600 hover:text-rose-700 hover:bg-rose-50"
                        onClick={() => setDeleteTarget(school)}
                      >
                        حذف
                      </Button>
                    </td>
                  </tr>
                );
              })}
              {schools.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-slate-400">
                    هیچ مدرسه‌ای یافت نشد.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Create School Modal */}
      <Modal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        title="ثبت مدرسه جدید"
      >
        <form onSubmit={handleCreateSubmit} className="space-y-4">
          <Input
            label="نام مدرسه"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="مثال: دبیرستان علامه حلی"
            required
          />
          <Input
            label="شناسه انگلیسی یکتا (slug)"
            value={slug}
            onChange={(e) => setSlug(e.target.value)}
            placeholder="مثال: helli-high-school"
            pattern="^[a-z0-9]+(?:-[a-z0-9]+)*$"
            helperText="فقط حروف کوچک انگلیسی، اعداد و خط تیره"
            required
          />
          <Input
            label="شناسه مالک (UUID کاربر)"
            value={ownerUserId}
            onChange={(e) => setOwnerUserId(e.target.value)}
            placeholder="مثال: 0192e4a8-..."
            required
          />
          {formError && <p className="text-xs text-rose-600 font-medium">{formError}</p>}
          <div className="flex justify-end gap-2 pt-3">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setIsCreateOpen(false)}
            >
              انصراف
            </Button>
            <Button
              type="submit"
              variant="primary"
              isLoading={createMutation.isPending}
            >
              ذخیره مدرسه
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        title="تأیید حذف مدرسه"
        maxWidth="sm"
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            آیا از حذف نرم مدرسه{' '}
            <strong className="text-slate-900 font-bold">{deleteTarget?.name}</strong> اطمینان
            دارید؟ وضعیت مدرسه به معلق تغییر یافته و دسترسی آن متوقف خواهد شد.
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
