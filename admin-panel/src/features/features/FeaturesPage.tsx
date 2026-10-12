import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../../lib/api-client';
import type { FeatureResponse } from '../../types/api';
import { createFeature, fetchFeatures, updateFeature } from '../admin/api';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';

export function FeaturesPage() {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [newCode, setNewCode] = useState('');
  const [newName, setNewName] = useState('');
  const [newCategory, setNewCategory] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const query = useQuery({
    queryKey: ['admin', 'features'],
    queryFn: fetchFeatures,
  });

  const toggleMutation = useMutation({
    mutationFn: ({ code, enabled }: { code: string; enabled: boolean }) =>
      updateFeature(code, { defaultEnabled: enabled }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'features'] });
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'تغییر وضعیت ناموفق بود.'),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      createFeature({
        code: newCode.trim(),
        name: newName.trim(),
        category: newCategory.trim() || undefined,
        description: newDesc.trim() || undefined,
        defaultEnabled: true,
      }),
    onSuccess: () => {
      setFormError(null);
      setShowCreate(false);
      setNewCode('');
      setNewName('');
      setNewCategory('');
      setNewDesc('');
      void queryClient.invalidateQueries({ queryKey: ['admin', 'features'] });
    },
    onError: (err) => setFormError(err instanceof ApiError ? err.message : 'ایجاد قابلیت ناموفق بود.'),
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
        بارگذاری امکانات ناموفق بود.
      </div>
    );
  }

  const features: FeatureResponse[] = query.data;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">مدیریت امکانات و Feature Flags</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            کنترل پرچم‌های امکانات سراسری و وضعیت فعال بودن پیش‌فرض در برنامک و ربات
          </p>
        </div>
        <Button variant="primary" onClick={() => setShowCreate(true)}>
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
          </svg>
          تعریف قابلیت جدید
        </Button>
      </div>

      <Card className="p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-right text-xs">
            <thead className="bg-slate-50 border-b border-slate-200/80 text-slate-600 font-bold">
              <tr>
                <th className="py-3 px-4">کد یکتای قابلیت</th>
                <th className="py-3 px-4">نام فارسی</th>
                <th className="py-3 px-4">دسته‌بندی</th>
                <th className="py-3 px-4">وضعیت پیش‌فرض</th>
                <th className="py-3 px-4 text-center">عملیات</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {features.map((feature) => (
                <tr key={feature.id} className="hover:bg-slate-50/50 transition-colors">
                  <td className="py-3.5 px-4 font-mono font-bold text-slate-800">{feature.code}</td>
                  <td className="py-3.5 px-4 font-semibold text-slate-800">{feature.name}</td>
                  <td className="py-3.5 px-4 text-slate-500">{feature.category ?? 'عمومی'}</td>
                  <td className="py-3.5 px-4">
                    <Badge variant={feature.defaultEnabled ? 'success' : 'neutral'}>
                      {feature.defaultEnabled ? 'فعال پیش‌فرض' : 'غیرفعال'}
                    </Badge>
                  </td>
                  <td className="py-3.5 px-4 text-center">
                    <Button
                      variant={feature.defaultEnabled ? 'outline' : 'primary'}
                      size="sm"
                      isLoading={toggleMutation.isPending}
                      onClick={() =>
                        toggleMutation.mutate({
                          code: feature.code,
                          enabled: !feature.defaultEnabled,
                        })
                      }
                    >
                      {feature.defaultEnabled ? 'غیرفعال‌سازی' : 'فعال‌سازی'}
                    </Button>
                  </td>
                </tr>
              ))}
              {features.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-slate-400">
                    هیچ قابلیتی یافت نشد.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Create Feature Modal */}
      <Modal
        isOpen={showCreate}
        onClose={() => setShowCreate(false)}
        title="تعریف قابلیت (Feature Flag) جدید"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate();
          }}
          className="space-y-4"
        >
          <Input
            label="کد انگلیسی قابلیت"
            value={newCode}
            onChange={(e) => setNewCode(e.target.value)}
            placeholder="مثال: AI_LESSON_PLAN"
            required
          />
          <Input
            label="نام فارسی قابلیت"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder="مثال: طرح درس هوشمند معلم"
            required
          />
          <Input
            label="دسته‌بندی (اختیاری)"
            value={newCategory}
            onChange={(e) => setNewCategory(e.target.value)}
            placeholder="مثال: TEACHING / AI"
          />
          <Input
            label="توضیحات مختصر"
            value={newDesc}
            onChange={(e) => setNewDesc(e.target.value)}
            placeholder="توضیح هدف قابلیت"
          />
          {formError && <p className="text-xs text-rose-600 font-medium">{formError}</p>}
          <div className="flex justify-end gap-2 pt-3">
            <Button type="button" variant="secondary" onClick={() => setShowCreate(false)}>
              انصراف
            </Button>
            <Button type="submit" variant="primary" isLoading={createMutation.isPending}>
              ثبت قابلیت
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
