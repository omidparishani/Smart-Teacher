import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Plus, Users } from 'lucide-react';
import { useState } from 'react';
import { PageHeader } from '../../components/shared/PageHeader';
import { EmptyState } from '../../components/shared/EmptyState';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import { createClass, listClasses } from '../../lib/classroom-api';

/**
 * فهرست کلاس‌های معلم — داده واقعی از {@code GET /classes}.
 *
 * <p>⚠️ هیچ آمار ساختگی نمایش داده نمی‌شود. اگر فهرست خالی است، واقعاً
 * کلاسی نیست — نه اینکه «هنوز ساخته نشده».
 */
export function ClassesPage() {
  const queryClient = useQueryClient();
  const [creating, setCreating] = useState(false);
  const [name, setName] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const classesQuery = useQuery({
    queryKey: ['classes'],
    queryFn: () => listClasses(),
  });

  const createMutation = useMutation({
    mutationFn: () => createClass({ name: name.trim(), academicYear: '1404-1405' }),
    onSuccess: async () => {
      setName('');
      setCreating(false);
      setFormError(null);
      await queryClient.invalidateQueries({ queryKey: ['classes'] });
    },
    onError: (error) => {
      setFormError(error instanceof ApiError ? error.message : 'ساخت کلاس انجام نشد.');
    },
  });

  return (
    <>
      <PageHeader
        title="کلاس‌های من"
        description="کلاس‌هایی که سرکلاسشان هستید"
        action={
          <Button
            variant="secondary"
            onClick={() => {
              setCreating((open) => !open);
              setFormError(null);
            }}
          >
            <Plus className="size-4" aria-hidden="true" />
            کلاس جدید
          </Button>
        }
      />

      {creating ? (
        <form
          className="mb-4 flex flex-col gap-3 rounded-xl border border-border bg-card p-4"
          onSubmit={(event) => {
            event.preventDefault();
            if (name.trim().length === 0) {
              setFormError('نام کلاس اجباری است.');
              return;
            }
            createMutation.mutate();
          }}
        >
          <label className="flex flex-col gap-1 text-sm">
            <span className="font-medium">نام کلاس</span>
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              className="rounded-xl border border-border bg-background px-3 py-2"
              placeholder="مثلاً پنجم الف"
              maxLength={150}
              autoFocus
            />
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

      {classesQuery.isPending ? <LoadingSkeleton lines={4} label="بارگذاری کلاس‌ها" /> : null}

      {classesQuery.isError ? (
        <ErrorCard
          title="کلاس‌ها بارگذاری نشد"
          message={
            classesQuery.error instanceof ApiError
              ? classesQuery.error.message
              : 'ارتباط با سرور برقرار نشد.'
          }
          action={
            <Button variant="secondary" onClick={() => void classesQuery.refetch()}>
              تلاش مجدد
            </Button>
          }
        />
      ) : null}

      {classesQuery.isSuccess && classesQuery.data.totalElements === 0 ? (
        <EmptyState
          title="هنوز کلاسی ندارید"
          description="اولین کلاس را بسازید تا بتوانید دانش‌آموز اضافه کنید."
          icon={<Users className="size-8" aria-hidden="true" />}
          action={
            <Button onClick={() => setCreating(true)}>
              <Plus className="size-4" aria-hidden="true" />
              ساخت کلاس
            </Button>
          }
        />
      ) : null}

      {classesQuery.isSuccess && classesQuery.data.content.length > 0 ? (
        <ul className="flex flex-col gap-2">
          {classesQuery.data.content.map((room) => (
            <li key={room.id}>
              <Link
                to={`/classes/${room.id}`}
                className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3 transition-colors hover:bg-muted/50"
              >
                <div>
                  <p className="font-medium">{room.name}</p>
                  <p className="text-sm text-muted-foreground">
                    {[room.gradeLevel, room.subjectName, room.academicYear]
                      .filter(Boolean)
                      .join(' · ') || 'بدون جزئیات'}
                  </p>
                </div>
                <span className="text-xs text-muted-foreground">
                  {room.status === 'ACTIVE' ? 'فعال' : 'بایگانی'}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      ) : null}
    </>
  );
}
