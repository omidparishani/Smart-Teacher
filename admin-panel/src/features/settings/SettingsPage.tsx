import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../../lib/api-client';
import type { SystemSettingResponse } from '../../types/api';
import { fetchSettings, updateSetting } from '../admin/api';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input, Textarea } from '../../components/ui/Input';
import { Modal } from '../../components/ui/Modal';

export function SettingsPage() {
  const queryClient = useQueryClient();
  const [editTarget, setEditTarget] = useState<SystemSettingResponse | null>(null);
  const [editValue, setEditValue] = useState('');
  const [editDesc, setEditDesc] = useState('');

  const query = useQuery({
    queryKey: ['admin', 'settings'],
    queryFn: fetchSettings,
  });

  const updateMutation = useMutation({
    mutationFn: () => updateSetting(editTarget!.key, editValue.trim(), editDesc.trim() || undefined),
    onSuccess: () => {
      setEditTarget(null);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'settings'] });
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'به‌روزرسانی تنظیمات ناموفق بود.'),
  });

  function startEdit(setting: SystemSettingResponse) {
    setEditTarget(setting);
    setEditValue(setting.value);
    setEditDesc(setting.description ?? '');
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
        {query.error instanceof ApiError ? query.error.message : 'بارگذاری تنظیمات ناموفق بود.'}
      </div>
    );
  }

  const settings = query.data;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">تنظیمات سراسری سامانه</h1>
        <p className="text-xs text-slate-500 mt-0.5">
          پیکربندی کلید/مقدار سراسری، مقادیر محدودیت‌ها و پارامترهای اجرایی پروداکشن
        </p>
      </div>

      <Card className="p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-right text-xs">
            <thead className="bg-slate-50 border-b border-slate-200/80 text-slate-600 font-bold">
              <tr>
                <th className="py-3 px-4">کلید تنظیم (Key)</th>
                <th className="py-3 px-4">مقدار فعلی (Value)</th>
                <th className="py-3 px-4">توضیحات</th>
                <th className="py-3 px-4">آخرین به‌روزرسانی</th>
                <th className="py-3 px-4 text-center">عملیات</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {settings.map((s) => (
                <tr key={s.id} className="hover:bg-slate-50/50 transition-colors">
                  <td className="py-3.5 px-4 font-mono font-bold text-slate-800">{s.key}</td>
                  <td className="py-3.5 px-4 font-mono text-slate-600 max-w-xs truncate">{s.value}</td>
                  <td className="py-3.5 px-4 text-slate-500">{s.description ?? '—'}</td>
                  <td className="py-3.5 px-4 font-mono text-slate-400">
                    {s.updatedAt ? new Date(s.updatedAt).toLocaleDateString('fa-IR') : '—'}
                  </td>
                  <td className="py-3.5 px-4 text-center">
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-blue-600 hover:bg-blue-50"
                      onClick={() => startEdit(s)}
                    >
                      ویرایش
                    </Button>
                  </td>
                </tr>
              ))}
              {settings.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-slate-400">
                    هیچ تنظیمی یافت نشد.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Edit Setting Modal */}
      <Modal
        isOpen={!!editTarget}
        onClose={() => setEditTarget(null)}
        title={`ویرایش تنظیم: ${editTarget?.key ?? ''}`}
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            updateMutation.mutate();
          }}
          className="space-y-4"
        >
          <Textarea
            label="مقدار تنظیم (Value)"
            value={editValue}
            onChange={(e) => setEditValue(e.target.value)}
            rows={4}
            required
          />
          <Input
            label="توضیحات پارامتر"
            value={editDesc}
            onChange={(e) => setEditDesc(e.target.value)}
          />
          <div className="flex justify-end gap-2 pt-3">
            <Button type="button" variant="secondary" onClick={() => setEditTarget(null)}>
              انصراف
            </Button>
            <Button type="submit" variant="primary" isLoading={updateMutation.isPending}>
              ذخیره تغییرات
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
