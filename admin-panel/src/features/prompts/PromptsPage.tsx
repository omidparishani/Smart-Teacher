import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../../lib/api-client';
import type { PromptResponse, PromptVersionResponse } from '../../types/api';
import { deletePrompt } from '../admin/api';
import { fetchPrompts, fetchPromptVersions, publishPromptVersion, rollbackPromptVersion } from './api';
import { Card, CardHeader } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';

export function PromptsPage() {
  const queryClient = useQueryClient();
  const [selectedPromptId, setSelectedPromptId] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<PromptResponse | null>(null);

  const promptsQuery = useQuery({
    queryKey: ['admin', 'prompts'],
    queryFn: fetchPrompts,
  });

  const versionsQuery = useQuery({
    queryKey: ['admin', 'prompts', selectedPromptId, 'versions'],
    queryFn: () => fetchPromptVersions(selectedPromptId!),
    enabled: selectedPromptId != null,
  });

  const publishMutation = useMutation({
    mutationFn: publishPromptVersion,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'prompts'] });
      void queryClient.invalidateQueries({ queryKey: ['admin', 'prompts', selectedPromptId, 'versions'] });
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'انتشار ناموفق بود.'),
  });

  const rollbackMutation = useMutation({
    mutationFn: rollbackPromptVersion,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'prompts'] });
      void queryClient.invalidateQueries({ queryKey: ['admin', 'prompts', selectedPromptId, 'versions'] });
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'بازگردانی نسخه ناموفق بود.'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deletePrompt(id),
    onSuccess: () => {
      setDeleteTarget(null);
      if (selectedPromptId === deleteTarget?.id) setSelectedPromptId(null);
      void queryClient.invalidateQueries({ queryKey: ['admin', 'prompts'] });
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'حذف پرامپت ناموفق بود.'),
  });

  if (promptsQuery.isPending) {
    return (
      <div className="flex items-center justify-center min-h-[40vh]">
        <div className="w-8 h-8 border-3 border-blue-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (promptsQuery.isError) {
    return (
      <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-sm">
        بارگذاری پرامپت‌ها ناموفق بود.
      </div>
    );
  }

  const prompts: PromptResponse[] = promptsQuery.data;
  const versions: PromptVersionResponse[] = versionsQuery.data ?? [];
  const selectedPrompt = prompts.find((p) => p.id === selectedPromptId);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900">مدیریت پرامپت‌های هوش مصنوعی</h1>
        <p className="text-xs text-slate-500 mt-0.5">
          قالب‌های سیستم، نسخه‌بندی متن پرامپت، انتشار بلادرنگ و قابلیت Rollback
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Prompts Catalog List */}
        <div className="lg:col-span-5">
          <Card className="p-0 overflow-hidden">
            <div className="p-3.5 bg-slate-50 border-b border-slate-200/80 font-bold text-xs text-slate-700">
              قالب‌های تعریف‌شده
            </div>
            <div className="divide-y divide-slate-100 max-h-[600px] overflow-y-auto">
              {prompts.map((p) => {
                const isSelected = selectedPromptId === p.id;
                return (
                  <div
                    key={p.id}
                    onClick={() => setSelectedPromptId(p.id)}
                    className={`p-3.5 cursor-pointer transition-colors flex items-center justify-between ${
                      isSelected ? 'bg-blue-50/70 border-r-4 border-blue-600' : 'hover:bg-slate-50'
                    }`}
                  >
                    <div>
                      <p className="text-xs font-bold text-slate-800">{p.name}</p>
                      <p className="text-[11px] font-mono text-slate-500 mt-0.5">{p.code}</p>
                      <span className="inline-block mt-1 text-[10px] font-mono px-1.5 py-0.5 rounded-sm bg-slate-100 text-slate-600">
                        {p.featureCode}
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-rose-600 hover:bg-rose-50 p-1 h-auto"
                        onClick={(e) => {
                          e.stopPropagation();
                          setDeleteTarget(p);
                        }}
                      >
                        حذف
                      </Button>
                    </div>
                  </div>
                );
              })}
              {prompts.length === 0 && (
                <div className="p-6 text-center text-xs text-slate-400">هیچ قالبی ثبت نشده است.</div>
              )}
            </div>
          </Card>
        </div>

        {/* Selected Prompt Versions & Details */}
        <div className="lg:col-span-7">
          {!selectedPrompt ? (
            <Card className="h-64 flex items-center justify-center text-xs text-slate-400">
              یک پرامپت را از لیست سمت راست انتخاب کنید تا تاریخچه نسخه‌های آن نمایش داده شود.
            </Card>
          ) : (
            <Card className="space-y-4">
              <CardHeader
                title={`نسخه‌های: ${selectedPrompt.name}`}
                subtitle={`کد سیستمی: ${selectedPrompt.code} | قابلیت: ${selectedPrompt.featureCode}`}
              />

              {versionsQuery.isPending ? (
                <div className="py-8 flex justify-center">
                  <div className="w-6 h-6 border-2 border-blue-600 border-t-transparent rounded-full animate-spin" />
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-right text-xs">
                    <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 font-bold">
                      <tr>
                        <th className="py-2.5 px-3">نسخه</th>
                        <th className="py-2.5 px-3">وضعیت</th>
                        <th className="py-2.5 px-3">متن سیستم / کاربر</th>
                        <th className="py-2.5 px-3 text-center">عملیات</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {versions.map((v) => (
                        <tr key={v.id} className="hover:bg-slate-50/50">
                          <td className="py-3 px-3 font-mono font-bold text-slate-800">v{v.versionNumber}</td>
                          <td className="py-3 px-3">
                            <Badge
                              variant={
                                v.status === 'PUBLISHED'
                                  ? 'success'
                                  : v.status === 'DRAFT'
                                  ? 'warning'
                                  : 'neutral'
                              }
                            >
                              {v.status === 'PUBLISHED'
                                ? 'منتشرشده'
                                : v.status === 'DRAFT'
                                ? 'پیش‌نویس'
                                : 'آرشیو'}
                            </Badge>
                          </td>
                          <td className="py-3 px-3 max-w-xs truncate font-mono text-[11px] text-slate-500">
                            {v.systemPrompt || v.userPrompt}
                          </td>
                          <td className="py-3 px-3 text-center">
                            {v.status === 'DRAFT' && (
                              <Button
                                variant="primary"
                                size="sm"
                                isLoading={publishMutation.isPending}
                                onClick={() => publishMutation.mutate(v.id)}
                              >
                                انتشار
                              </Button>
                            )}
                            {v.status === 'ARCHIVED' && (
                              <Button
                                variant="secondary"
                                size="sm"
                                isLoading={rollbackMutation.isPending}
                                onClick={() => rollbackMutation.mutate(v.id)}
                              >
                                بازگردانی
                              </Button>
                            )}
                          </td>
                        </tr>
                      ))}
                      {versions.length === 0 && (
                        <tr>
                          <td colSpan={4} className="py-6 text-center text-slate-400">
                            هیچ نسخه‌ای یافت نشد.
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              )}
            </Card>
          )}
        </div>
      </div>

      {/* Delete Prompt Modal */}
      <Modal
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        title="تأیید حذف قالب پرامپت"
        maxWidth="sm"
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            آیا از حذف کامل پرامپت <strong className="text-slate-900 font-bold">{deleteTarget?.name}</strong> اطمینان
            دارید؟ تمام نسخه‌ها و انتساب‌های آن حذف خواهند شد.
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
