import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Clock } from 'lucide-react';
import { PageHeader } from '../../components/shared/PageHeader';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import { listGroups, scheduleMessage } from '../../lib/eitaa-api';

/**
 * زمان‌بندی پیام گروه — حداقل UI.
 */
export function SchedulePage() {
  const [groupId, setGroupId] = useState('');
  const [text, setText] = useState('');
  const [scheduledAt, setScheduledAt] = useState('');
  const [note, setNote] = useState<string | null>(null);

  const groupsQuery = useQuery({
    queryKey: ['eitaa-groups', 'for-schedule'],
    queryFn: () => listGroups(),
  });

  const scheduleMutation = useMutation({
    mutationFn: () =>
      scheduleMessage({
        groupId,
        text: text.trim(),
        scheduledAt: new Date(scheduledAt).toISOString(),
      }),
    onSuccess: () => {
      setText('');
      setScheduledAt('');
      setNote('پیام زمان‌بندی شد (PENDING تا جاب ارسال کند).');
    },
    onError: (error) => {
      setNote(error instanceof ApiError ? error.message : 'زمان‌بندی ناموفق بود.');
    },
  });

  return (
    <>
      <PageHeader
        title="زمان‌بندی پیام"
        description="پیام در زمان مقرر به گروه ارسال می‌شود"
      />

      {groupsQuery.isPending ? <LoadingSkeleton lines={2} /> : null}

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

      <section className="space-y-3">
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Clock className="size-4" aria-hidden="true" />
          <span>گروه، متن و زمان را انتخاب کنید</span>
        </div>
        <select
          className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          value={groupId}
          onChange={(e) => setGroupId(e.target.value)}
        >
          <option value="">گروه</option>
          {(groupsQuery.data ?? []).map((g) => (
            <option key={g.id} value={g.id}>
              {g.title ?? g.eitaaChatId}
            </option>
          ))}
        </select>
        <textarea
          className="min-h-24 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder="متن پیام"
        />
        <input
          type="datetime-local"
          className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          value={scheduledAt}
          onChange={(e) => setScheduledAt(e.target.value)}
        />
        <Button
          disabled={
            !groupId || !text.trim() || !scheduledAt || scheduleMutation.isPending
          }
          onClick={() => scheduleMutation.mutate()}
        >
          زمان‌بندی
        </Button>
      </section>

      {note ? <p className="mt-4 text-sm text-muted-foreground">{note}</p> : null}
    </>
  );
}
