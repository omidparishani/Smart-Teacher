import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ArrowRight, UserPlus } from 'lucide-react';
import { useState } from 'react';
import { PageHeader } from '../../components/shared/PageHeader';
import { EmptyState } from '../../components/shared/EmptyState';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import { enrollStudent, listClassStudents, unenrollStudent } from '../../lib/classroom-api';
import { listStudents } from '../../lib/student-api';

/**
 * دانش‌آموزان یک کلاس — ثبت‌نام و لغو ثبت‌نام واقعی.
 */
export function ClassStudentsPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const [enrolling, setEnrolling] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const enrolledQuery = useQuery({
    queryKey: ['classes', id, 'students'],
    queryFn: () => listClassStudents(id!),
    enabled: Boolean(id),
  });

  const schoolStudentsQuery = useQuery({
    queryKey: ['students'],
    queryFn: () => listStudents(0, 100),
    enabled: enrolling,
  });

  const enrollMutation = useMutation({
    mutationFn: (studentId: string) => enrollStudent(id!, { studentId }),
    onSuccess: async () => {
      setActionError(null);
      setEnrolling(false);
      await queryClient.invalidateQueries({ queryKey: ['classes', id] });
    },
    onError: (error) => {
      setActionError(error instanceof ApiError ? error.message : 'ثبت‌نام انجام نشد.');
    },
  });

  const unenrollMutation = useMutation({
    mutationFn: (studentId: string) => unenrollStudent(id!, studentId),
    onSuccess: async () => {
      setActionError(null);
      await queryClient.invalidateQueries({ queryKey: ['classes', id] });
    },
    onError: (error) => {
      setActionError(error instanceof ApiError ? error.message : 'لغو ثبت‌نام انجام نشد.');
    },
  });

  if (!id) {
    return <ErrorCard title="کلاس نامشخص" message="شناسه کلاس در آدرس نیست." />;
  }

  const enrolledIds = new Set(
    enrolledQuery.data?.content.map((row) => row.studentId) ?? [],
  );

  return (
    <>
      <PageHeader
        title="دانش‌آموزان کلاس"
        action={
          <Link to={`/classes/${id}`} className="inline-flex items-center gap-1 text-sm text-primary">
            <ArrowRight className="size-4" aria-hidden="true" />
            بازگشت
          </Link>
        }
      />

      <div className="mb-4 flex gap-2">
        <Button
          variant="secondary"
          onClick={() => {
            setEnrolling((open) => !open);
            setActionError(null);
          }}
        >
          <UserPlus className="size-4" aria-hidden="true" />
          افزودن از مدرسه
        </Button>
        <Link to="/students/new">
          <Button variant="ghost">دانش‌آموز جدید</Button>
        </Link>
      </div>

      {actionError ? <p className="mb-3 text-sm text-danger-strong">{actionError}</p> : null}

      {enrolling ? (
        <div className="mb-4 rounded-xl border border-border bg-card p-3">
          <p className="mb-2 text-sm font-medium">انتخاب از فهرست مدرسه</p>
          {schoolStudentsQuery.isPending ? (
            <LoadingSkeleton lines={3} label="بارگذاری دانش‌آموزان" />
          ) : null}
          {schoolStudentsQuery.isSuccess && schoolStudentsQuery.data.content.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              هنوز دانش‌آموزی در مدرسه نیست.{' '}
              <Link to="/students/new" className="text-primary">
                یکی بسازید
              </Link>
            </p>
          ) : null}
          {schoolStudentsQuery.isSuccess ? (
            <ul className="flex max-h-64 flex-col gap-1 overflow-y-auto">
              {schoolStudentsQuery.data.content
                .filter((student) => !enrolledIds.has(student.id))
                .map((student) => (
                  <li key={student.id} className="flex items-center justify-between gap-2 py-1">
                    <span className="text-sm">{student.fullName}</span>
                    <Button
                      variant="secondary"
                      disabled={enrollMutation.isPending}
                      onClick={() => enrollMutation.mutate(student.id)}
                    >
                      افزودن
                    </Button>
                  </li>
                ))}
            </ul>
          ) : null}
        </div>
      ) : null}

      {enrolledQuery.isPending ? <LoadingSkeleton lines={4} label="بارگذاری دانش‌آموزان کلاس" /> : null}

      {enrolledQuery.isError ? (
        <ErrorCard
          title="فهرست بارگذاری نشد"
          message={
            enrolledQuery.error instanceof ApiError
              ? enrolledQuery.error.message
              : 'ارتباط با سرور برقرار نشد.'
          }
          action={
            <Button variant="secondary" onClick={() => void enrolledQuery.refetch()}>
              تلاش مجدد
            </Button>
          }
        />
      ) : null}

      {enrolledQuery.isSuccess && enrolledQuery.data.totalElements === 0 ? (
        <EmptyState
          title="کسی در این کلاس نیست"
          description="دانش‌آموز را از فهرست مدرسه اضافه کنید یا اول پرونده‌اش را بسازید."
        />
      ) : null}

      {enrolledQuery.isSuccess && enrolledQuery.data.content.length > 0 ? (
        <ul className="flex flex-col gap-2">
          {enrolledQuery.data.content.map((row) => (
            <li
              key={row.studentId}
              className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3"
            >
              <Link to={`/students/${row.studentId}`} className="min-w-0 flex-1">
                <p className="font-medium">{row.fullName}</p>
                <p className="text-sm text-muted-foreground">
                  {[row.rollNumber ? `شماره ${row.rollNumber}` : null, row.nationalCode]
                    .filter(Boolean)
                    .join(' · ') || 'بدون جزئیات'}
                </p>
              </Link>
              <Button
                variant="ghost"
                disabled={unenrollMutation.isPending}
                onClick={() => unenrollMutation.mutate(row.studentId)}
              >
                حذف از کلاس
              </Button>
            </li>
          ))}
        </ul>
      ) : null}
    </>
  );
}
