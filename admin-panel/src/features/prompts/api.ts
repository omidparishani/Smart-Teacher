import { api } from '../../lib/api-client';
import type { PromptResponse, PromptVersionResponse } from '../../types/api';

export function fetchPrompts(): Promise<PromptResponse[]> {
  return api.get<PromptResponse[]>('/admin/prompts');
}

export function fetchPromptVersions(promptId: string): Promise<PromptVersionResponse[]> {
  return api.get<PromptVersionResponse[]>(`/admin/prompts/${promptId}/versions`);
}

export function publishPromptVersion(versionId: string): Promise<PromptVersionResponse> {
  return api.post<PromptVersionResponse>(`/admin/prompts/versions/${versionId}/publish`);
}

export function rollbackPromptVersion(versionId: string): Promise<PromptVersionResponse> {
  return api.post<PromptVersionResponse>(`/admin/prompts/versions/${versionId}/rollback`);
}
