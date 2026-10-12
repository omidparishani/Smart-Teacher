import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { ClipboardList, Plus } from 'lucide-react';
import { useState } from 'react';
import { PageHeader } from '../../components/shared/PageHeader';
import { EmptyState } from '../../components/shared/EmptyState';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import { createAssignment, listAssignments } from '../../lib/assignment-api';
import { listClasses } from '../../lib/classroom-api';

/**
 * فهرست تکالیف + فرم ساخت — داده واقعی از {@code GET/POST /assignments}.
 */
export function AssignmentsPage() {
  const queryClient = useQueryClient();
  const [creating, setCreating] = useState(false);
  const [title, setTitle] = useState('');
  const [classroomId, setClassroomId] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const assignmentsQuery = useQuery({
    queryKey: ['assignments'],
    queryFn: () => listAssignments(),
  });

  const classesQuery = useQuery({
    queryKey: ['classes'],
    queryFn: () => listClasses(),
    enabled: creating,
  });

  const createMutation = useMutation({
    mutationFn: () =>
      createAssignment({
        title: title.trim(),
        classroomId,
        type: 'HOMEWORK',
      }),
    onSuccess: async () => {
      setTitle('');
      setClassroomId('');
      setCreating(false);
      setFormError(null);
      await queryClient.invalidateQueries({ queryKey: ['assignments'] });
    },
    onError: (error) => {
      setFormError(error instanceof ApiError ? error.message : 'ساخت تکلیف انجام نشد.');
    },
  });

  return (
    <>
      <PageHeader
        title="تکالیف"
        description="تکالیف ساخته‌شده و وضعیت تحویل"
        action={
          <Button
            variant="secondary"
            onClick={() => {
              setCreating((open) => !open);
              setFormError(null);
            }}
          >
            <Plus className="size-4" aria-hidden="true" />
            تکلیف جدید
          </Button>
        }
      />

      {creating ? (
        <form
          className="mb-4 flex flex-col gap-3 rounded-xl border border-border bg-card p-4"
          onSubmit={(event) => {
            event.preventDefault();
            if (title.trim().length === 0) {
              setFormError('عنوان تکلیف اجباری است.');
              return;
            }
            if (!classroomId) {
              setFormError('کلاس را انتخاب کنید.');
              return;
            }
            createMutation.mutate();
          }}
        >
          <label className="flex flex-col gap-1 text-sm">
            <span className="font-medium">عنوان</span>
            <input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              className="rounded-xl border border-border bg-background px-3 py-2"
              placeholder="مثلاً تمرین فصل سوم"
              maxLength={250}
              autoFocus
            />
          </label>
          <label className="flex flex-col gap-1 text-sm">
            <span className="font-medium">کلاس</span>
            <select
              value={classroomId}
              onChange={(event) => setClassroomId(event.target.value)}
              className="rounded-xl border border-border bg-background px-3 py-2"
            >
              <option value="">انتخاب کلاس…</option>
              {(classesQuery.data?.content ?? []).map((room) => (
                <option key={room.id} value={room.id}>
                  {room.name}
                </option>
              ))}
            </select>
          </label>
          {formError ? <p className="text-sm text-danger-strong">{formError}</p> : null}
          <div className="flex gap-2">
            <Button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? 'در حال ساخت…' : 'ساخت'}
            </Button>
            <Button
              type="button"
              variant="ghost"
              onClick={() => {
                setCreating(false);
                setFormError(null);
              }}
            >
              انصراف
            </Button>
          </div>
        </form>
      ) : null}

      {assignmentsQuery.isPending ? (
        <LoadingSkeleton lines={4} label="بارگذاری تکالیف" />
      ) : null}

      {assignmentsQuery.isError ? (
        <ErrorCard
          title="تکالیف بارگذاری نشد"
          message={
            assignmentsQuery.error instanceof ApiError
              ? assignmentsQuery.error.message
              : 'ارتباط با سرور برقرار نشد.'
          }
          action={
            <Button variant="secondary" onClick={() => void assignmentsQuery.refetch()}>
              تلاش مجدد
            </Button>
          }
        />
      ) : null}

      {assignmentsQuery.isSuccess && assignmentsQuery.data.totalElements === 0 ? (
        <EmptyState
          title="هنوز تکلیفی ندارید"
          description="اولین تکلیف را بسازید و برای کلاس منتشر کنید."
          icon={<ClipboardList className="size-8" aria-hidden="true" />}
          action={
            <Button onClick={() => setCreating(true)}>
              <Plus className="size-4" aria-hidden="true" />
              ساخت تکلیف
            </Button>
          }
        />
      ) : null}

      {assignmentsQuery.isSuccess && assignmentsQuery.data.content.length > 0 ? (
        <ul className="flex flex-col gap-2">
          {assignmentsQuery.data.content.map((item) => (
            <li key={item.id}>
              <Link
                to={`/assignments/${item.id}`}
                className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3 transition-colors hover:bg-muted/50"
              >
                <div>
                  <p className="font-medium">{item.title}</p>
                  <p className="text-sm text-muted-foreground">
                    {[item.classroomName, statusLabel(item.status), `${item.submissionCount} ارسال`]
                      .filter(Boolean)
                      .join(' · ')}
                  </p>
                </div>
                <span className="text-xs text-muted-foreground">{statusLabel(item.status)}</span>
              </Link>
            </li>
          ))}
        </ul>
      ) : null}
    </>
  );
}

function statusLabel(status: string): string {
  switch (status) {
    case 'PUBLISHED':
      return 'منتشرشده';
    case 'CLOSED':
      return 'بسته';
    case 'ARCHIVED':
      return 'بایگانی';
    default:
      return 'پیش‌نویس';
  }
}
