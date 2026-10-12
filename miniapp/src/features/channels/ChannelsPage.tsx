import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Radio } from 'lucide-react';
import { PageHeader } from '../../components/shared/PageHeader';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { EmptyState } from '../../components/shared/EmptyState';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import {
  createChannel,
  deleteChannel,
  listChannels,
  sendChannelMessage,
  updateChannel,
  verifyChannel,
  type EitaaChannel,
  type EitaaConnectionCheck,
} from '../../lib/eitaa-api';

/**
 * صفحه کانال‌های ایتا — فهرست، ثبت و ارسال.
 */
export function ChannelsPage() {
  const queryClient = useQueryClient();
  const [chatId, setChatId] = useState('');
  const [title, setTitle] = useState('');
  const [username, setUsername] = useState('');
  const [selectedId, setSelectedId] = useState('');
  const [sendText, setSendText] = useState('');
  const [note, setNote] = useState<string | null>(null);

  const channelsQuery = useQuery({
    queryKey: ['eitaa-channels'],
    queryFn: () => listChannels(),
  });

  const createMutation = useMutation({
    mutationFn: () => {
      const body: { eitaaChatId: string; title?: string; username?: string } = {
        eitaaChatId: chatId.trim(),
      };
      const t = title.trim();
      if (t) {
        body.title = t;
      }
      const u = username.trim();
      if (u) {
        body.username = u;
      }
      return createChannel(body);
    },
    onSuccess: async () => {
      setChatId('');
      setTitle('');
      setUsername('');
      setNote('کانال ثبت شد.');
      await queryClient.invalidateQueries({ queryKey: ['eitaa-channels'] });
    },
    onError: (error) => {
      setNote(error instanceof ApiError ? error.message : 'ثبت کانال ناموفق بود.');
    },
  });

  const sendMutation = useMutation({
    mutationFn: () => sendChannelMessage(selectedId, sendText.trim()),
    onSuccess: (result) => {
      setSendText('');
      setNote(result.success ? 'پیام به کانال ارسال شد.' : result.errorMessage ?? 'ارسال ناموفق');
    },
    onError: (error) => {
      setNote(error instanceof ApiError ? error.message : 'ارسال ناموفق بود.');
    },
  });

  // ⚠️ تأیید حذف دو مرحله‌ای است، نه `window.confirm`: داخل WebView ایتا
  // دیالوگ‌های بومی مرورگر قابل‌اعتماد نیستند و ممکن است بی‌صدا بلوک شوند.
  const [confirmingDeleteId, setConfirmingDeleteId] = useState('');

  const [editingId, setEditingId] = useState('');
  const [editChatId, setEditChatId] = useState('');
  const [editTitle, setEditTitle] = useState('');
  const [editUsername, setEditUsername] = useState('');

  const [check, setCheck] = useState<{ id: string; result: EitaaConnectionCheck } | null>(null);

  function startEdit(c: EitaaChannel): void {
    setEditingId(c.id);
    setEditChatId(c.eitaaChatId);
    setEditTitle(c.title ?? '');
    setEditUsername(c.username ?? '');
    setCheck(null);
    setConfirmingDeleteId('');
  }

  const verifyMutation = useMutation({
    mutationFn: (id: string) => verifyChannel(id),
    onSuccess: (result, id) => {
      setCheck({ id, result });
      setNote(null);
    },
    onError: (error) => {
      setCheck(null);
      setNote(error instanceof ApiError ? error.message : 'بررسی اتصال ناموفق بود.');
    },
  });

  const updateMutation = useMutation({
    mutationFn: () => {
      // ⚠️ `chat_id` فقط اگر غیرخالی بود فرستاده می‌شود؛ رشته خالی برای
      // عنوان/نام کاربری یعنی «پاک کن» و عمداً به‌شکل `''` می‌رود — بک‌اند
      // با `trimToNull` آن را به `null` تبدیل می‌کند.
      const body: { eitaaChatId?: string; title?: string; username?: string } = {
        title: editTitle.trim(),
        username: editUsername.trim(),
      };
      const c = editChatId.trim();
      if (c) {
        body.eitaaChatId = c;
      }
      return updateChannel(editingId, body);
    },
    onSuccess: async () => {
      setEditingId('');
      setNote('کانال ویرایش شد.');
      await queryClient.invalidateQueries({ queryKey: ['eitaa-channels'] });
    },
    onError: (error) => {
      setNote(error instanceof ApiError ? error.message : 'ویرایش ناموفق بود.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteChannel(id),
    onSuccess: async (_void, id) => {
      setConfirmingDeleteId('');
      if (selectedId === id) {
        setSelectedId('');
      }
      setCheck(null);
      setNote('کانال حذف شد.');
      await queryClient.invalidateQueries({ queryKey: ['eitaa-channels'] });
    },
    onError: (error) => {
      setNote(error instanceof ApiError ? error.message : 'حذف ناموفق بود.');
    },
  });

  return (
    <>
      <PageHeader title="کانال‌های ایتا" description="ثبت کانال و ارسال پست" />

      {channelsQuery.isPending ? <LoadingSkeleton lines={3} /> : null}

      {channelsQuery.isError ? (
        <ErrorCard
          title="کانال‌ها بارگذاری نشد"
          message={
            channelsQuery.error instanceof ApiError
              ? channelsQuery.error.message
              : 'ارتباط برقرار نشد.'
          }
        />
      ) : null}

      {channelsQuery.isSuccess && channelsQuery.data.length === 0 ? (
        <EmptyState
          icon={<Radio className="size-8" aria-hidden="true" />}
          title="هنوز کانالی نیست"
          description="شناسه چت کانال را ثبت کنید."
        />
      ) : null}

      {channelsQuery.isSuccess && channelsQuery.data.length > 0 ? (
        <ul className="mb-6 space-y-2">
          {channelsQuery.data.map((c) => (
            <li key={c.id} className="rounded-xl border border-border bg-card px-4 py-3">
              <button
                type="button"
                className={[
                  'w-full text-start text-sm',
                  selectedId === c.id ? 'text-primary' : '',
                ].join(' ')}
                onClick={() => setSelectedId(c.id)}
              >
                <div className="font-medium">{c.title ?? c.eitaaChatId}</div>
                <div className="text-muted-foreground">{c.eitaaChatId}</div>
              </button>

              <div className="mt-2 flex flex-wrap items-center gap-3 text-sm">
                <button
                  type="button"
                  className="text-primary"
                  disabled={verifyMutation.isPending}
                  onClick={() => verifyMutation.mutate(c.id)}
                >
                  تست اتصال
                </button>
                <button type="button" className="text-primary" onClick={() => startEdit(c)}>
                  ویرایش
                </button>
                {confirmingDeleteId === c.id ? (
                  <>
                    <span className="text-destructive">مطمئنید؟</span>
                    <button
                      type="button"
                      className="text-destructive"
                      disabled={deleteMutation.isPending}
                      onClick={() => deleteMutation.mutate(c.id)}
                    >
                      تأیید حذف
                    </button>
                    <button
                      type="button"
                      className="text-muted-foreground"
                      onClick={() => setConfirmingDeleteId('')}
                    >
                      انصراف
                    </button>
                  </>
                ) : (
                  <button
                    type="button"
                    className="text-destructive"
                    onClick={() => setConfirmingDeleteId(c.id)}
                  >
                    حذف
                  </button>
                )}
              </div>

              {editingId === c.id ? (
                <div className="mt-3 space-y-2">
                  <input
                    className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
                    placeholder="شناسه چت — بدون @"
                    value={editChatId}
                    onChange={(e) => setEditChatId(e.target.value)}
                  />
                  <input
                    className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
                    placeholder="عنوان (خالی = پاک‌کردن)"
                    value={editTitle}
                    onChange={(e) => setEditTitle(e.target.value)}
                  />
                  <input
                    className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
                    placeholder="نام کاربری (خالی = پاک‌کردن)"
                    value={editUsername}
                    onChange={(e) => setEditUsername(e.target.value)}
                  />
                  <div className="flex gap-2">
                    <Button
                      disabled={updateMutation.isPending}
                      onClick={() => updateMutation.mutate()}
                    >
                      ذخیره
                    </Button>
                    <Button variant="secondary" onClick={() => setEditingId('')}>
                      انصراف
                    </Button>
                  </div>
                </div>
              ) : null}

              {check?.id === c.id ? (
                <div className="mt-3 rounded-xl border border-border bg-background px-3 py-2 text-sm">
                  <p>{check.result.detail}</p>
                  {check.result.mockMode ? (
                    <p className="mt-1 text-destructive">
                      حالت شبیه‌ساز روشن است — این نتیجه اتصال واقعی را تأیید نمی‌کند.
                    </p>
                  ) : null}
                  <p className="mt-1 text-muted-foreground">
                    توکن: {check.result.botTokenConfigured ? 'تنظیم‌شده' : 'تنظیم‌نشده'} · قالب
                    شناسه: {check.result.chatIdFormatValid ? 'معتبر' : 'نامعتبر'}
                  </p>
                  <p className="mt-1 text-muted-foreground">
                    ⚠️ این بررسی فقط توکن را می‌سنجد، نه دسترسی ارسال. تنها تأیید قطعی، فرستادن
                    یک پیام واقعی است.
                  </p>
                </div>
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}

      <section className="mb-6 space-y-3">
        <h2 className="text-sm font-medium">ثبت کانال</h2>
        <input
          className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          placeholder="شناسه چت — بدون @ (مثلاً my_channel یا 11268746)"
          value={chatId}
          onChange={(e) => setChatId(e.target.value)}
        />
        <input
          className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          placeholder="عنوان (اختیاری)"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
        <input
          className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          placeholder="نام کاربری (اختیاری)"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />
        <Button
          disabled={!chatId.trim() || createMutation.isPending}
          onClick={() => createMutation.mutate()}
        >
          ثبت
        </Button>
      </section>

      <section className="space-y-3">
        <h2 className="text-sm font-medium">ارسال به کانال</h2>
        <textarea
          className="min-h-24 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          value={sendText}
          onChange={(e) => setSendText(e.target.value)}
          placeholder="متن پست"
        />
        <Button
          disabled={!selectedId || !sendText.trim() || sendMutation.isPending}
          onClick={() => sendMutation.mutate()}
        >
          ارسال
        </Button>
      </section>

      {note ? <p className="mt-4 text-sm text-muted-foreground">{note}</p> : null}
    </>
  );
}
