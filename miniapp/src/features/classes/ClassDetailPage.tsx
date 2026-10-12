import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ArrowRight, Users } from 'lucide-react';
import { PageHeader } from '../../components/shared/PageHeader';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import { getClassDashboard } from '../../lib/classroom-api';

/**
 * جزئیات و داشبورد یک کلاس — داده واقعی از {@code GET /classes/{id}/dashboard}.
 *
 * <p>⚠️ تکالیف و نمرات عمداً نیستند: بک‌اند هم صفر جعلی برنمی‌گرداند.
 * میان‌بُر دانش‌آموزان همان جایی است که الان داده واقعی دارد.
 */
export function ClassDetailPage() {
  const { id } = useParams<{ id: string }>();

  const dashboardQuery = useQuery({
    queryKey: ['classes', id, 'dashboard'],
    queryFn: () => getClassDashboard(id!),
    enabled: Boolean(id),
  });

  if (!id) {
    return (
      <ErrorCard title="کلاس نامشخص" message="شناسه کلاس در آدرس نیست." />
    );
  }

  if (dashboardQuery.isPending) {
    return <LoadingSkeleton lines={5} label="بارگذاری داشبورد کلاس" />;
  }

  if (dashboardQuery.isError) {
    return (
      <ErrorCard
        title="داشبورد کلاس بارگذاری نشد"
        message={
          dashboardQuery.error instanceof ApiError
            ? dashboardQuery.error.message
            : 'ارتباط با سرور برقرار نشد.'
        }
        action={
          <Button variant="secondary" onClick={() => void dashboardQuery.refetch()}>
            تلاش مجدد
          </Button>
        }
      />
    );
  }

  const dash = dashboardQuery.data;
  const description = [dash.gradeLevel, dash.subjectName, dash.academicYear]
    .filter(Boolean)
    .join(' · ');

  return (
    <>
      <PageHeader
        title={dash.name}
        {...(description ? { description } : {})}
        action={
          <Link
            to="/classes"
            className="inline-flex items-center gap-1 text-sm text-primary"
          >
            <ArrowRight className="size-4" aria-hidden="true" />
            بازگشت
          </Link>
        }
      />

      <div className="mb-4 grid grid-cols-2 gap-3">
        <StatCard label="دانش‌آموزان" value={String(dash.enrolledStudentCount)} />
        <StatCard
          label="ظرفیت"
          value={
            dash.capacity == null
              ? 'بدون سقف'
              : `${dash.enrolledStudentCount} / ${dash.capacity}`
          }
        />
        <StatCard
          label="صندلی خالی"
          value={dash.seatsRemaining == null ? '—' : String(dash.seatsRemaining)}
        />
        <StatCard label="وضعیت" value={dash.status === 'ACTIVE' ? 'فعال' : 'بایگانی'} />
      </div>

      <Link
        to={`/classes/${id}/students`}
        className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3 transition-colors hover:bg-muted/50"
      >
        <div className="flex items-center gap-3">
          <Users className="size-5 text-primary" aria-hidden="true" />
          <div>
            <p className="font-medium">دانش‌آموزان کلاس</p>
            <p className="text-sm text-muted-foreground">
              {dash.enrolledStudentCount} نفر ثبت‌نام شده
            </p>
          </div>
        </div>
        <span className="text-muted-foreground" aria-hidden="true">
          ‹
        </span>
      </Link>
    </>
  );
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-border bg-card px-3 py-3">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 text-lg font-semibold">{value}</p>
    </div>
  );
}
