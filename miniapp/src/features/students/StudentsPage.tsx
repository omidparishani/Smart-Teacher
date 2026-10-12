import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Plus, User } from 'lucide-react';
import { PageHeader } from '../../components/shared/PageHeader';
import { EmptyState } from '../../components/shared/EmptyState';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import { listStudents } from '../../lib/student-api';

/**
 * فهرست دانش‌آموزان مدرسه — {@code GET /students}.
 */
export function StudentsPage() {
  const studentsQuery = useQuery({
    queryKey: ['students'],
    queryFn: () => listStudents(),
  });

  return (
    <>
      <PageHeader
        title="دانش‌آموزان"
        description="پرونده‌های مدرسه"
        action={
          <Link to="/students/new">
            <Button variant="secondary">
              <Plus className="size-4" aria-hidden="true" />
              جدید
            </Button>
          </Link>
        }
      />

      {studentsQuery.isPending ? <LoadingSkeleton lines={4} label="بارگذاری دانش‌آموزان" /> : null}

      {studentsQuery.isError ? (
        <ErrorCard
          title="فهرست بارگذاری نشد"
          message={
            studentsQuery.error instanceof ApiError
              ? studentsQuery.error.message
              : 'ارتباط با سرور برقرار نشد.'
          }
          action={
            <Button variant="secondary" onClick={() => void studentsQuery.refetch()}>
              تلاش مجدد
            </Button>
          }
        />
      ) : null}

      {studentsQuery.isSuccess && studentsQuery.data.totalElements === 0 ? (
        <EmptyState
          title="هنوز دانش‌آموزی نیست"
          description="اولین پرونده دانش‌آموز را بسازید، بعد او را به کلاس اضافه کنید."
          icon={<User className="size-8" aria-hidden="true" />}
          action={
            <Link to="/students/new">
              <Button>
                <Plus className="size-4" aria-hidden="true" />
                ساخت دانش‌آموز
              </Button>
            </Link>
          }
        />
      ) : null}

      {studentsQuery.isSuccess && studentsQuery.data.content.length > 0 ? (
        <ul className="flex flex-col gap-2">
          {studentsQuery.data.content.map((student) => (
            <li key={student.id}>
              <Link
                to={`/students/${student.id}`}
                className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3 transition-colors hover:bg-muted/50"
              >
                <div>
                  <p className="font-medium">{student.fullName}</p>
                  <p className="text-sm text-muted-foreground">
                    {student.nationalCode ?? 'بدون کد ملی'}
                  </p>
                </div>
                <span className="text-xs text-muted-foreground">
                  {statusLabel(student.status)}
                </span>
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
    case 'ACTIVE':
      return 'فعال';
    case 'INACTIVE':
      return 'غیرفعال';
    case 'GRADUATED':
      return 'فارغ‌التحصیل';
    default:
      return status;
  }
}
