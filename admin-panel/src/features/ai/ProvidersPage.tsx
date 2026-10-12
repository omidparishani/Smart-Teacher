import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../../lib/api-client';
import type { AiModelResponse, AiProviderResponse, ProviderResolveTraceResponse } from '../../types/api';
import {
  createAiModel,
  createAiProvider,
  deleteAiModel,
  deleteAiProvider,
  fetchAiModels,
  fetchAiProviders,
  fetchResolveTrace,
} from './api';
import { Card, CardHeader } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input, Select } from '../../components/ui/Input';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';

export function ProvidersPage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'providers' | 'models' | 'trace'>('providers');

  // Trace state
  const [feature, setFeature] = useState('TEACHER_CHAT');
  const [tenantId, setTenantId] = useState('');
  const [trace, setTrace] = useState<ProviderResolveTraceResponse | null>(null);
  const [traceError, setTraceError] = useState<string | null>(null);
  const [loadingTrace, setLoadingTrace] = useState(false);

  // Provider modals
  const [showCreateProvider, setShowCreateProvider] = useState(false);
  const [pCode, setPCode] = useState('');
  const [pName, setPName] = useState('');
  const [pType, setPType] = useState('OPENAI_COMPATIBLE');
  const [pBaseUrl, setPBaseUrl] = useState('');
  const [pApiKey, setPApiKey] = useState('');

  // Model modals
  const [showCreateModel, setShowCreateModel] = useState(false);
  const [mProviderId, setMProviderId] = useState('');
  const [mCode, setMCode] = useState('');
  const [mName, setMName] = useState('');
  const [mInputCost, setMInputCost] = useState('0');
  const [mOutputCost, setMOutputCost] = useState('0');

  const providersQuery = useQuery({
    queryKey: ['admin', 'ai', 'providers'],
    queryFn: fetchAiProviders,
  });

  const modelsQuery = useQuery({
    queryKey: ['admin', 'ai', 'models'],
    queryFn: () => fetchAiModels(),
  });

  const createProviderMutation = useMutation({
    mutationFn: () =>
      createAiProvider({
        code: pCode.trim(),
        name: pName.trim(),
        type: pType,
        baseUrl: pBaseUrl.trim() || undefined,
        apiKey: pApiKey.trim() || undefined,
        enabled: true,
      }),
    onSuccess: () => {
      setShowCreateProvider(false);
      setPCode('');
      setPName('');
      setPBaseUrl('');
      setPApiKey('');
      void queryClient.invalidateQueries({ queryKey: ['admin', 'ai', 'providers'] });
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'ثبت سرویس‌دهنده ناموفق بود.'),
  });

  const deleteProviderMutation = useMutation({
    mutationFn: (id: string) => deleteAiProvider(id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['admin', 'ai', 'providers'] }),
    onError: (err) => alert(err instanceof ApiError ? err.message : 'حذف سرویس‌دهنده ناموفق بود.'),
  });

  const createModelMutation = useMutation({
    mutationFn: () =>
      createAiModel({
        providerId: mProviderId,
        code: mCode.trim(),
        displayName: mName.trim(),
        enabled: true,
        inputCostPerMillion: Number(mInputCost),
        outputCostPerMillion: Number(mOutputCost),
      }),
    onSuccess: () => {
      setShowCreateModel(false);
      setMCode('');
      setMName('');
      void queryClient.invalidateQueries({ queryKey: ['admin', 'ai', 'models'] });
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'ثبت مدل ناموفق بود.'),
  });

  const deleteModelMutation = useMutation({
    mutationFn: (id: string) => deleteAiModel(id),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['admin', 'ai', 'models'] }),
    onError: (err) => alert(err instanceof ApiError ? err.message : 'حذف مدل ناموفق بود.'),
  });

  async function runTrace() {
    setLoadingTrace(true);
    setTraceError(null);
    try {
      const result = await fetchResolveTrace(
        tenantId.trim()
          ? { feature: feature.trim(), tenantId: tenantId.trim() }
          : { feature: feature.trim() },
      );
      setTrace(result);
    } catch {
      setTraceError('اجرای ردیابی ناموفق بود.');
      setTrace(null);
    } finally {
      setLoadingTrace(false);
    }
  }

  const providers: AiProviderResponse[] = providersQuery.data ?? [];
  const models: AiModelResponse[] = modelsQuery.data ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">سرویس‌دهنده‌ها و مدل‌های هوش مصنوعی</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            پیکربندی ارائه‌دهندگان LLM، مدل‌های پردازش و سلسله‌مراتب Resolve
          </p>
        </div>
        <div className="flex items-center gap-2">
          {activeTab === 'providers' && (
            <Button variant="primary" onClick={() => setShowCreateProvider(true)}>
              سرویس‌دهنده جدید
            </Button>
          )}
          {activeTab === 'models' && (
            <Button
              variant="primary"
              disabled={providers.length === 0}
              onClick={() => {
                if (providers[0]) setMProviderId(providers[0].id);
                setShowCreateModel(true);
              }}
            >
              افزودن مدل هوش مصنوعی
            </Button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-slate-200">
        <button
          type="button"
          onClick={() => setActiveTab('providers')}
          className={`px-4 py-2 text-xs font-bold transition-all border-b-2 -mb-px ${
            activeTab === 'providers'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          سرویس‌دهنده‌ها (Providers)
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('models')}
          className={`px-4 py-2 text-xs font-bold transition-all border-b-2 -mb-px ${
            activeTab === 'models'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          مدل‌های پردازشی (Models)
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('trace')}
          className={`px-4 py-2 text-xs font-bold transition-all border-b-2 -mb-px ${
            activeTab === 'trace'
              ? 'border-blue-600 text-blue-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          ردیابی انتخاب مدل (Resolve Trace)
        </button>
      </div>

      {/* Tab: Providers */}
      {activeTab === 'providers' && (
        <Card className="p-0 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-right text-xs">
              <thead className="bg-slate-50 border-b border-slate-200/80 text-slate-600 font-bold">
                <tr>
                  <th className="py-3 px-4">کد یکتا</th>
                  <th className="py-3 px-4">نام سرویس‌دهنده</th>
                  <th className="py-3 px-4">نوع درایور</th>
                  <th className="py-3 px-4">نشانی Base URL</th>
                  <th className="py-3 px-4">وضعیت</th>
                  <th className="py-3 px-4 text-center">عملیات</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {providers.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="py-3.5 px-4 font-mono font-semibold text-slate-700">{p.code}</td>
                    <td className="py-3.5 px-4 font-bold text-slate-800">{p.name}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-500">{p.type}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-500 truncate max-w-xs">{p.baseUrl || 'پیش‌فرض ابری'}</td>
                    <td className="py-3.5 px-4">
                      <Badge variant={p.enabled ? 'success' : 'neutral'}>
                        {p.enabled ? 'فعال' : 'غیرفعال'}
                      </Badge>
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-rose-600 hover:bg-rose-50"
                        onClick={() => {
                          if (confirm(`آیا از حذف سرویس‌دهنده ${p.name} اطمینان دارید؟`)) {
                            deleteProviderMutation.mutate(p.id);
                          }
                        }}
                      >
                        حذف
                      </Button>
                    </td>
                  </tr>
                ))}
                {providers.length === 0 && (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-slate-400">
                      هیچ سرویس‌دهنده‌ای ثبت نشده است.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* Tab: Models */}
      {activeTab === 'models' && (
        <Card className="p-0 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-right text-xs">
              <thead className="bg-slate-50 border-b border-slate-200/80 text-slate-600 font-bold">
                <tr>
                  <th className="py-3 px-4">کد مدل</th>
                  <th className="py-3 px-4">نام نمایشی</th>
                  <th className="py-3 px-4">سرویس‌دهنده</th>
                  <th className="py-3 px-4">هزینه ورودی / ۱M توکن</th>
                  <th className="py-3 px-4">هزینه خروجی / ۱M توکن</th>
                  <th className="py-3 px-4">وضعیت</th>
                  <th className="py-3 px-4 text-center">عملیات</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {models.map((m) => {
                  const p = providers.find((x) => x.id === m.providerId);
                  return (
                    <tr key={m.id} className="hover:bg-slate-50/50 transition-colors">
                      <td className="py-3.5 px-4 font-mono font-semibold text-slate-700">{m.code}</td>
                      <td className="py-3.5 px-4 font-bold text-slate-800">{m.displayName}</td>
                      <td className="py-3.5 px-4 text-slate-600">{p?.name || '—'}</td>
                      <td className="py-3.5 px-4 font-mono text-slate-600">{String(m.inputCostPerMillion ?? 0)}</td>
                      <td className="py-3.5 px-4 font-mono text-slate-600">{String(m.outputCostPerMillion ?? 0)}</td>
                      <td className="py-3.5 px-4">
                        <Badge variant={m.enabled ? 'success' : 'neutral'}>
                          {m.enabled ? 'فعال' : 'غیرفعال'}
                        </Badge>
                      </td>
                      <td className="py-3.5 px-4 text-center">
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-rose-600 hover:bg-rose-50"
                          onClick={() => {
                            if (confirm(`آیا از حذف مدل ${m.displayName} اطمینان دارید؟`)) {
                              deleteModelMutation.mutate(m.id);
                            }
                          }}
                        >
                          حذف
                        </Button>
                      </td>
                    </tr>
                  );
                })}
                {models.length === 0 && (
                  <tr>
                    <td colSpan={7} className="py-8 text-center text-slate-400">
                      هیچ مدلی یافت نشد.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* Tab: Resolve Trace */}
      {activeTab === 'trace' && (
        <Card className="space-y-4">
          <CardHeader
            title="شبیه‌ساز و ردیابی حل سرویس‌دهنده (Resolve Trace)"
            subtitle="بررسی این‌که یک قابلیت هوش مصنوعی برای یک مستأجر خاص از چه پرووایدر و کلیدی استفاده می‌کند"
          />
          <div className="flex flex-col sm:flex-row gap-3">
            <Input
              label="کد قابلیت (Feature Code)"
              value={feature}
              onChange={(e) => setFeature(e.target.value)}
              placeholder="مثال: TEACHER_CHAT"
            />
            <Input
              label="شناسه مستأجر (UUID اختیاری)"
              value={tenantId}
              onChange={(e) => setTenantId(e.target.value)}
              placeholder="خالی بگذارید برای تنظیمات سراسری"
            />
            <div className="self-end">
              <Button
                variant="primary"
                isLoading={loadingTrace}
                disabled={!feature.trim()}
                onClick={() => void runTrace()}
              >
                اجرای ردیابی
              </Button>
            </div>
          </div>

          {traceError && (
            <div className="p-3 bg-rose-50 text-rose-700 text-xs rounded-lg">{traceError}</div>
          )}

          {trace && (
            <div className="mt-4 p-4 bg-slate-50 rounded-xl border border-slate-200 space-y-3">
              <div className="flex items-center gap-2">
                <span className="text-xs text-slate-500 font-semibold">پرووایدر برگزیده نهایی:</span>
                <span className="text-sm font-mono font-bold text-blue-600">
                  {trace.selectedProviderCode ?? 'یافت نشد'}
                </span>
              </div>
              <div className="space-y-2 pt-2">
                <p className="text-xs font-bold text-slate-700">مراحل جستجو در سلسله‌مراتب:</p>
                <div className="space-y-1.5">
                  {trace.steps.map((step, idx) => (
                    <div
                      key={idx}
                      className={`flex items-center justify-between p-2.5 rounded-lg text-xs font-mono border ${
                        step.found
                          ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
                          : 'bg-white border-slate-200 text-slate-600'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <span>{step.found ? '✓' : '✗'}</span>
                        <span className="font-bold">{step.level}</span>
                        {step.providerCode && (
                          <span className="text-slate-700 font-normal">({step.providerCode})</span>
                        )}
                      </div>
                      {step.detail && <span className="text-slate-500 text-[11px]">{step.detail}</span>}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </Card>
      )}

      {/* Create Provider Modal */}
      <Modal
        isOpen={showCreateProvider}
        onClose={() => setShowCreateProvider(false)}
        title="ثبت سرویس‌دهنده جدید AI"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createProviderMutation.mutate();
          }}
          className="space-y-4"
        >
          <Input
            label="کد انگلیسی یکتا"
            value={pCode}
            onChange={(e) => setPCode(e.target.value)}
            placeholder="مثال: openai_main"
            required
          />
          <Input
            label="نام نمایشی"
            value={pName}
            onChange={(e) => setPName(e.target.value)}
            placeholder="مثال: OpenAI Official"
            required
          />
          <Select
            label="نوع پروتکل"
            value={pType}
            onChange={(e) => setPType(e.target.value)}
            options={[
              { value: 'OPENAI_COMPATIBLE', label: 'OpenAI Compatible (سازگار با OpenAI)' },
              { value: 'ANTHROPIC', label: 'Anthropic Claude' },
              { value: 'CUSTOM', label: 'سفارشی / محلی' },
            ]}
          />
          <Input
            label="Base URL (اختیاری)"
            value={pBaseUrl}
            onChange={(e) => setPBaseUrl(e.target.value)}
            placeholder="مثال: https://api.openai.com/v1"
          />
          <Input
            label="API Key (اختیاری و محرمانه)"
            type="password"
            value={pApiKey}
            onChange={(e) => setPApiKey(e.target.value)}
            placeholder="sk-..."
          />
          <div className="flex justify-end gap-2 pt-3">
            <Button type="button" variant="secondary" onClick={() => setShowCreateProvider(false)}>
              انصراف
            </Button>
            <Button type="submit" variant="primary" isLoading={createProviderMutation.isPending}>
              ثبت سرویس‌دهنده
            </Button>
          </div>
        </form>
      </Modal>

      {/* Create Model Modal */}
      <Modal
        isOpen={showCreateModel}
        onClose={() => setShowCreateModel(false)}
        title="تعریف مدل پردازشی جدید"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createModelMutation.mutate();
          }}
          className="space-y-4"
        >
          <Select
            label="سرویس‌دهنده والد"
            value={mProviderId}
            onChange={(e) => setMProviderId(e.target.value)}
            options={providers.map((p) => ({ value: p.id, label: `${p.name} (${p.code})` }))}
          />
          <Input
            label="کد سیستمی مدل"
            value={mCode}
            onChange={(e) => setMCode(e.target.value)}
            placeholder="مثال: gpt-4o-mini"
            required
          />
          <Input
            label="نام نمایشی مدل"
            value={mName}
            onChange={(e) => setMName(e.target.value)}
            placeholder="مثال: GPT-4o Mini (سریع)"
            required
          />
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="هزینه ورودی / ۱M توکن"
              type="number"
              value={mInputCost}
              onChange={(e) => setMInputCost(e.target.value)}
            />
            <Input
              label="هزینه خروجی / ۱M توکن"
              type="number"
              value={mOutputCost}
              onChange={(e) => setMOutputCost(e.target.value)}
            />
          </div>
          <div className="flex justify-end gap-2 pt-3">
            <Button type="button" variant="secondary" onClick={() => setShowCreateModel(false)}>
              انصراف
            </Button>
            <Button type="submit" variant="primary" isLoading={createModelMutation.isPending}>
              ذخیره مدل
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
