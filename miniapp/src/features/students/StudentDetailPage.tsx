import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { PageHeader } from '../../components/shared/PageHeader';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import { getStudent } from '../../lib/student-api';

/**
 * پرونده یک دانش‌آموز — {@code GET /students/{id}}.
 *
 * <p>⚠️ عملکرد و تکالیف عمداً نیستند؛ بک‌اند هم آن Endpointها را هنوز
 * ندارد. نشان‌دادن «۰ تکلیف» دروغ است.
 */
export function StudentDetailPage() {
  const { id } = useParams<{ id: string }>();

  const studentQuery = useQuery({
    queryKey: ['students', id],
    queryFn: () => getStudent(id!),
    enabled: Boolean(id),
  });

  if (!id) {
    return <ErrorCard title="دانش‌آموز نامشخص" message="شناسه در آدرس نیست." />;
  }

  if (studentQuery.isPending) {
    return <LoadingSkeleton lines={5} label="بارگذاری پرونده" />;
  }

  if (studentQuery.isError) {
    return (
      <ErrorCard
        title="پرونده بارگذاری نشد"
        message={
          studentQuery.error instanceof ApiError
            ? studentQuery.error.message
            : 'ارتباط با سرور برقرار نشد.'
        }
        action={
          <Button variant="secondary" onClick={() => void studentQuery.refetch()}>
            تلاش مجدد
          </Button>
        }
      />
    );
  }

  const student = studentQuery.data;

  return (
    <>
      <PageHeader
        title={student.fullName}
        description={statusLabel(student.status)}
        action={
          <Link to="/students" className="inline-flex items-center gap-1 text-sm text-primary">
            <ArrowRight className="size-4" aria-hidden="true" />
            بازگشت
          </Link>
        }
      />

      <dl className="flex flex-col gap-3 rounded-xl border border-border bg-card p-4 text-sm">
        <Row label="کد ملی" value={student.nationalCode} />
        <Row label="نام ولی" value={student.guardianName} />
        <Row label="موبایل ولی" value={student.guardianMobile} />
        <Row label="تاریخ تولد" value={student.birthDate} />
        <Row
          label="جنسیت"
          value={
            student.gender === 'MALE'
              ? 'پسر'
              : student.gender === 'FEMALE'
                ? 'دختر'
                : null
          }
        />
      </dl>
    </>
  );
}

function Row({ label, value }: { label: string; value: string | null }) {
  return (
    <div className="flex justify-between gap-4 border-b border-border pb-2 last:border-0 last:pb-0">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="font-medium">{value && value.length > 0 ? value : '—'}</dd>
    </div>
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
