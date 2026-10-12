import { apiFetch } from './api-client';

export interface EitaaGroup {
  id: string;
  eitaaChatId: string;
  title?: string | null;
  type: string;
  classroomId?: string | null;
  status: string;
  canSendMessages: boolean;
  createdAt: string;
}

export interface EitaaChannel {
  id: string;
  eitaaChatId: string;
  title?: string | null;
  username?: string | null;
  status: string;
  canPost: boolean;
  createdAt: string;
}

export interface AutomationRule {
  id: string;
  name: string;
  triggerType: string;
  triggerConfig?: string | null;
  actionType: string;
  actionConfig?: string | null;
  enabled: boolean;
  executionCount: number;
  lastExecutedAt?: string | null;
  createdAt: string;
}

export interface ScheduledMessage {
  id: string;
  targetType: string;
  targetId: string;
  contentText: string;
  scheduledAt: string;
  status: string;
  eitaaMessageId?: string | null;
}

export interface SendMessageResult {
  success: boolean;
  messageId?: string | null;
  errorCode?: string | null;
  errorMessage?: string | null;
}

/**
 * نتیجه بررسی اتصال ایتا — آینه‌ی `EitaaConnectionCheckResponse` بک‌اند.
 *
 * <p>🔴 این پاسخ دقیقاً می‌گوید چه چیزی تأیید شد و چه چیزی نه. ایتا متدی
 * برای پرسیدن «آیا این گفت‌وگو وجود دارد و ربات به آن دسترسی دارد؟» ندارد
 * (نه `getChat`، نه `getChatMember`). پس {@code chatIdFormatValid} یعنی
 * «رشته شبیه یک شناسه است»، نه «این گفت‌وگو وجود دارد».
 *
 * <p>⚠️ در حالت شبیه‌ساز، {@code getMe} همیشه موفق می‌شود؛ پس وقتی
 * {@code mockMode} روشن است، {@code botHealthy} هیچ چیز واقعی را تأیید
 * نمی‌کند. تنها تأیید قطعی، فرستادن یک پیام واقعی است.
 */
export interface EitaaConnectionCheck {
  botTokenConfigured: boolean;
  mockMode: boolean;
  botHealthy: boolean;
  botUsername?: string | null;
  botDisplayName?: string | null;
  chatId?: string | null;
  chatIdFormatValid: boolean;
  detail: string;
  checkedAt: string;
}

export function listGroups(): Promise<EitaaGroup[]> {
  return apiFetch<EitaaGroup[]>('/eitaa/groups');
}

export function createGroup(payload: {
  eitaaChatId: string;
  title?: string;
  type?: string;
}): Promise<EitaaGroup> {
  return apiFetch<EitaaGroup>('/eitaa/groups', {
    method: 'POST',
    body: payload,
  });
}

export function connectClass(groupId: string, classroomId: string): Promise<EitaaGroup> {
  return apiFetch<EitaaGroup>(`/eitaa/groups/${groupId}/connect-class`, {
    method: 'POST',
    body: { classroomId },
  });
}

export function sendGroupMessage(
  groupId: string,
  text: string,
  options?: { pin?: boolean; viewCountForDelete?: number },
): Promise<SendMessageResult> {
  return apiFetch<SendMessageResult>(`/eitaa/groups/${groupId}/send-message`, {
    method: 'POST',
    body: {
      text,
      pin: options?.pin ?? false,
      viewCountForDelete: options?.viewCountForDelete ?? null,
    },
  });
}

export function listChannels(): Promise<EitaaChannel[]> {
  return apiFetch<EitaaChannel[]>('/eitaa/channels');
}

export function createChannel(payload: {
  eitaaChatId: string;
  title?: string;
  username?: string;
}): Promise<EitaaChannel> {
  return apiFetch<EitaaChannel>('/eitaa/channels', {
    method: 'POST',
    body: payload,
  });
}

export function sendChannelMessage(
  channelId: string,
  text: string,
  options?: { pin?: boolean; viewCountForDelete?: number },
): Promise<SendMessageResult> {
  return apiFetch<SendMessageResult>(`/eitaa/channels/${channelId}/send-message`, {
    method: 'POST',
    body: {
      text,
      pin: options?.pin ?? false,
      viewCountForDelete: options?.viewCountForDelete ?? null,
    },
  });
}

export function listAutomationRules(): Promise<AutomationRule[]> {
  return apiFetch<AutomationRule[]>('/eitaa/automation-rules');
}

export function createAutomationRule(payload: {
  name: string;
  triggerType: string;
  triggerConfig?: string;
  actionType: string;
  actionConfig?: string;
  enabled?: boolean;
}): Promise<AutomationRule> {
  return apiFetch<AutomationRule>('/eitaa/automation-rules', {
    method: 'POST',
    body: payload,
  });
}

export function scheduleMessage(payload: {
  groupId: string;
  text: string;
  scheduledAt: string;
}): Promise<ScheduledMessage> {
  return apiFetch<ScheduledMessage>('/eitaa/scheduled-messages', {
    method: 'POST',
    body: payload,
  });
}

// ─────────────────────────────────────────────────────────────
//  بررسی اتصال (A2) — تشخیصی، نه تأیید دسترسی
// ─────────────────────────────────────────────────────────────

/** سلامت ربات با `getMe` — هیچ پیامی فرستاده نمی‌شود. */
export function checkEitaaHealth(): Promise<EitaaConnectionCheck> {
  return apiFetch<EitaaConnectionCheck>('/eitaa/health');
}

/**
 * بررسی اتصال گروه.
 *
 * <p>⚠️ اعتبار توکن و قالب `chat_id` را می‌سنجد — <b>نه دسترسی ارسال</b>.
 * برای اطمینان قطعی یک پیام آزمایشی بفرستید.
 */
export function verifyGroup(groupId: string): Promise<EitaaConnectionCheck> {
  return apiFetch<EitaaConnectionCheck>(`/eitaa/groups/${groupId}/verify`, {
    method: 'POST',
  });
}

/** بررسی اتصال کانال — همان محدودیت `verifyGroup`. */
export function verifyChannel(channelId: string): Promise<EitaaConnectionCheck> {
  return apiFetch<EitaaConnectionCheck>(`/eitaa/channels/${channelId}/verify`, {
    method: 'POST',
  });
}

// ─────────────────────────────────────────────────────────────
//  ویرایش و حذف مقصد (A2)
// ─────────────────────────────────────────────────────────────

export function updateGroup(
  groupId: string,
  payload: { eitaaChatId?: string; title?: string; type?: string },
): Promise<EitaaGroup> {
  return apiFetch<EitaaGroup>(`/eitaa/groups/${groupId}`, {
    method: 'PUT',
    body: payload,
  });
}

export function deleteGroup(groupId: string): Promise<void> {
  return apiFetch<void>(`/eitaa/groups/${groupId}`, { method: 'DELETE' });
}

export function updateChannel(
  channelId: string,
  payload: { eitaaChatId?: string; title?: string; username?: string },
): Promise<EitaaChannel> {
  return apiFetch<EitaaChannel>(`/eitaa/channels/${channelId}`, {
    method: 'PUT',
    body: payload,
  });
}

export function deleteChannel(channelId: string): Promise<void> {
  return apiFetch<void>(`/eitaa/channels/${channelId}`, { method: 'DELETE' });
}
