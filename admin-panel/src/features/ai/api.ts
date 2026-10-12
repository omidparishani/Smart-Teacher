import { api } from '../../lib/api-client';
import type { AiProviderResponse, ProviderResolveTraceResponse } from '../../types/api';

export function fetchAiProviders(): Promise<AiProviderResponse[]> {
  return api.get<AiProviderResponse[]>('/admin/ai/providers');
}

export function createAiProvider(body: {
  code: string;
  name: string;
  type: string;
  baseUrl?: string | undefined;
  apiKey?: string | undefined;
  enabled?: boolean | undefined;
}): Promise<AiProviderResponse> {
  return api.post<AiProviderResponse>('/admin/ai/providers', body);
}

export function updateAiProvider(
  id: string,
  body: {
    name?: string | undefined;
    baseUrl?: string | undefined;
    apiKey?: string | undefined;
    enabled?: boolean | undefined;
  },
): Promise<AiProviderResponse> {
  return api.put<AiProviderResponse>(`/admin/ai/providers/${id}`, body);
}

export function deleteAiProvider(id: string): Promise<void> {
  return api.delete<void>(`/admin/ai/providers/${id}`);
}

export function fetchAiModels(providerId?: string): Promise<import('../../types/api').AiModelResponse[]> {
  const url = providerId ? `/admin/ai/models?providerId=${providerId}` : '/admin/ai/models';
  return api.get<import('../../types/api').AiModelResponse[]>(url);
}

export function createAiModel(body: {
  providerId: string;
  code: string;
  displayName: string;
  enabled?: boolean | undefined;
  inputCostPerMillion?: number | undefined;
  outputCostPerMillion?: number | undefined;
  currency?: string | undefined;
}): Promise<import('../../types/api').AiModelResponse> {
  return api.post<import('../../types/api').AiModelResponse>('/admin/ai/models', body);
}

export function updateAiModel(
  id: string,
  body: {
    displayName?: string | undefined;
    enabled?: boolean | undefined;
    inputCostPerMillion?: number | undefined;
    outputCostPerMillion?: number | undefined;
    currency?: string | undefined;
  },
): Promise<import('../../types/api').AiModelResponse> {
  return api.put<import('../../types/api').AiModelResponse>(`/admin/ai/models/${id}`, body);
}

export function deleteAiModel(id: string): Promise<void> {
  return api.delete<void>(`/admin/ai/models/${id}`);
}

export function fetchResolveTrace(params: {
  feature: string;
  tenantId?: string;
}): Promise<ProviderResolveTraceResponse> {
  const qs = new URLSearchParams({ feature: params.feature });
  if (params.tenantId) {
    qs.set('tenantId', params.tenantId);
  }
  return api.get<ProviderResolveTraceResponse>(`/admin/ai/resolve-trace?${qs.toString()}`);
}
