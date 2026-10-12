import { apiFetch } from './api-client';
import type { AiChatBody, AiChatResponse } from '../types/api';

/**
 * تماس‌های HTTP دستیار — {@code POST /ai/chat}.
 */
export function sendChatMessage(body: AiChatBody): Promise<AiChatResponse> {
  return apiFetch<AiChatResponse>('/ai/chat', { method: 'POST', body });
}
