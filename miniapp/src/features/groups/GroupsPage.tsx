import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { MessageSquare } from 'lucide-react';
import { PageHeader } from '../../components/shared/PageHeader';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { EmptyState } from '../../components/shared/EmptyState';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import {
  connectClass,
  createGroup,
  deleteGroup,
  listGroups,
  sendGroupMessage,
  updateGroup,
  verifyGroup,
  type EitaaConnectionCheck,
  type EitaaGroup,
} from '../../lib/eitaa-api';
import { listClasses } from '../../lib/classroom-api';

/**
 * صفحه گروه‌های ایتا — فهرست، اتصال کلاس، ارسال پیام.
 */
export function GroupsPage() {
  const queryClient = useQueryClient();
  const [chatId, setChatId] = useState('');
  const [title, setTitle] = useState('');
  const [sendText, setSendText] = useState('');
  const [selectedGroupId, setSelectedGroupId] = useState('');
  const [classroomId, setClassroomId] = useState('');
  const [pin, setPin] = useState(false);
  const [viewCountForDelete, setViewCountForDelete] = useState('');
  const [note, setNote] = useState<string | null>(null);

  const groupsQuery = useQuery({
    queryKey: ['eitaa-groups'],
    queryFn: () => listGroups(),
  });

  const classesQuery = useQuery({
    queryKey: ['classes', 'for-groups'],
    queryFn: () => listClasses(0, 50),
  });

  const createMutation = useMutation({
    mutationFn: () => {
      const payload: { eitaaChatId: string; title?: string; type: string } = {
        eitaaChatId: chatId.trim(),
        type: 'GROUP',
      };
      const trimmedTitle = title.trim();
      if (trimmedTitle) {
        payload.title = trimmedTitle;
      }
      return createGroup(payload);
    },
    onSuccess: async () => {
      setChatId('');
      setTitle('');
      setNote('گروه ثبت شد.');
      await queryClient.invalidateQueries({ queryKey: ['eitaa-groups'] });
    },
    onError: (error) => {
      setNote(error instanceof ApiError ? error.message : 'ثبت گروه ناموفق بود.');
    },
  });

  // ⚠️ تأیید حذف دو مرحله‌ای است، نه `window.confirm`: داخل WebView ایتا
  // دیالوگ‌های بومی مرورگر قابل‌اعتماد نیستند و ممکن است بی‌صدا بلوک شوند.
  const [confirmingDeleteId, setConfirmingDeleteId] = useState('');
  const [editingId, setEditingId] = useState('');
  const [editChatId, setEditChatId] = useState('');
  const [editTitle, setEditTitle] = useState('');
  const [check, setCheck] = useState<{ id: string; result: EitaaConnectionCheck } | null>(null);

  function startEdit(g: EitaaGroup): void {
    setEditingId(g.id);
    setEditChatId(g.eitaaChatId);
    setEditTitle(g.title ?? '');
    setCheck(null);
    setConfirmingDeleteId('');
  }

  const verifyMutation = useMutation({
    mutationFn: (id: string) => verifyGroup(id),
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
      // ⚠️ عنوان خالی عمداً به‌شکل `''` می‌رود — بک‌اند با `trimToNull` آن
      // را به `null` تبدیل می‌کند، یعنی «پاک کن».
      const body: { eitaaChatId?: string; title?: string } = { title: editTitle.trim() };
      const c = editChatId.trim();
      if (c) {
        body.eitaaChatId = c;
      }
      return updateGroup(editingId, body);
    },
    onSuccess: async () => {
      setEditingId('');
      setNote('گروه ویرایش شد.');
      await queryClient.invalidateQueries({ queryKey: ['eitaa-groups'] });
    },
    onError: (error) => {
      setNote(error instanceof ApiError ? error.message : 'ویرایش ناموفق بود.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteGroup(id),
    onSuccess: async (_void, id) => {
      setConfirmingDeleteId('');
      if (selectedGroupId === id) {
        setSelectedGroupId('');
      }
      setCheck(null);
      setNote('گروه حذف شد.');
      await queryClient.invalidateQueries({ queryKey: ['eitaa-groups'] });
    },
    onError: (error) => {
      setNote(error instanceof ApiError ? error.message : 'حذف ناموفق بود.');
    },
  });

  const connectMutation = useMutation({
    mutationFn: () => connectClass(selectedGroupId, classroomId),
    onSuccess: async () => {
      setNote('کلاس متصل شد.');
      await queryClient.invalidateQueries({ queryKey: ['eitaa-groups'] });
    },
    onError: (error) => {
      setNote(error instanceof ApiError ? error.message : 'اتصال ناموفق بود.');
    },
  });

  const sendMutation = useMutation({
    mutationFn: () => {
      const opts: { pin: boolean; viewCountForDelete?: number } = { pin };
      const views = viewCountForDelete.trim()
        ? Number.parseInt(viewCountForDelete.trim(), 10)
        : NaN;
      if (Number.isFinite(views) && views > 0) {
        opts.viewCountForDelete = views;
      }
      return sendGroupMessage(selectedGroupId, sendText.trim(), opts);
    },
    onSuccess: (result) => {
      setSendText('');
      setNote(result.success ? 'پیام ارسال شد.' : result.errorMessage ?? 'ارسال ناموفق');
    },
    onError: (error) => {
      setNote(error instanceof ApiError ? error.message : 'ارسال ناموفق بود.');
    },
  });

  return (
    <>
      <PageHeader title="گروه‌های ایتا" description="اتصال کلاس و ارسال پیام خروجی" />

      {groupsQuery.isPending ? <LoadingSkeleton lines={3} /> : null}

      {groupsQuery.isError ? (
        <ErrorCard
          title="گروه‌ها بارگذاری نشد"
          message={
            groupsQuery.error instanceof ApiError
              ? groupsQuery.error.message
              : 'ارتباط برقرار نشد.'
          }
        />
      ) : null}

      {groupsQuery.isSuccess && groupsQuery.data.length === 0 ? (
        <EmptyState
          icon={<MessageSquare className="size-8" aria-hidden="true" />}
          title="هنوز گروهی نیست"
          description="شناسه چت ایتا را ثبت کنید."
        />
      ) : null}

      {groupsQuery.isSuccess && groupsQuery.data.length > 0 ? (
        <ul className="mb-6 space-y-2">
          {groupsQuery.data.map((g) => (
            <li key={g.id} className="rounded-xl border border-border bg-card px-4 py-3">
              <button
                type="button"
                className={[
                  'w-full text-start text-sm',
                  selectedGroupId === g.id ? 'text-primary' : '',
                ].join(' ')}
                onClick={() => setSelectedGroupId(g.id)}
              >
                <div className="font-medium">{g.title ?? g.eitaaChatId}</div>
                <div className="text-muted-foreground">{g.eitaaChatId}</div>
              </button>

              <div className="mt-2 flex flex-wrap items-center gap-3 text-sm">
                <button
                  type="button"
                  className="text-primary"
                  disabled={verifyMutation.isPending}
                  onClick={() => verifyMutation.mutate(g.id)}
                >
                  تست اتصال
                </button>
                <button type="button" className="text-primary" onClick={() => startEdit(g)}>
                  ویرایش
                </button>
                {confirmingDeleteId === g.id ? (
                  <>
                    <span className="text-destructive">مطمئنید؟</span>
                    <button
                      type="button"
                      className="text-destructive"
                      disabled={deleteMutation.isPending}
                      onClick={() => deleteMutation.mutate(g.id)}
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
                    onClick={() => setConfirmingDeleteId(g.id)}
                  >
                    حذف
                  </button>
                )}
              </div>

              {editingId === g.id ? (
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

              {check?.id === g.id ? (
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
        <h2 className="text-sm font-medium">ثبت گروه</h2>
        <input
          className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          placeholder="شناسه چت — بدون @ (مثلاً my_group یا 11268746)"
          value={chatId}
          onChange={(e) => setChatId(e.target.value)}
        />
        <input
          className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          placeholder="عنوان (اختیاری)"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
        <Button
          disabled={!chatId.trim() || createMutation.isPending}
          onClick={() => createMutation.mutate()}
        >
          ثبت
        </Button>
      </section>

      <section className="mb-6 space-y-3">
        <h2 className="text-sm font-medium">اتصال به کلاس</h2>
        <select
          className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          value={classroomId}
          onChange={(e) => setClassroomId(e.target.value)}
        >
          <option value="">کلاس</option>
          {(classesQuery.data?.content ?? []).map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        <Button
          disabled={!selectedGroupId || !classroomId || connectMutation.isPending}
          onClick={() => connectMutation.mutate()}
        >
          اتصال
        </Button>
      </section>

      <section className="space-y-3">
        <h2 className="text-sm font-medium">ارسال پیام</h2>
        <textarea
          className="min-h-24 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          value={sendText}
          onChange={(e) => setSendText(e.target.value)}
          placeholder="متن پیام"
        />
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={pin} onChange={(e) => setPin(e.target.checked)} />
          سنجاق پیام (pin)
        </label>
        <input
          type="number"
          min={1}
          className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          placeholder="حذف پس از N بازدید (viewCountForDelete)"
          value={viewCountForDelete}
          onChange={(e) => setViewCountForDelete(e.target.value)}
        />
        <Button
          disabled={!selectedGroupId || !sendText.trim() || sendMutation.isPending}
          onClick={() => sendMutation.mutate()}
        >
          ارسال
        </Button>
      </section>

      {note ? <p className="mt-4 text-sm text-muted-foreground">{note}</p> : null}
    </>
  );
}
