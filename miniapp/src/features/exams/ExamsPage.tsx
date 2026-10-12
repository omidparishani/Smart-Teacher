import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { ClipboardCheck, Plus } from 'lucide-react';
import { PageHeader } from '../../components/shared/PageHeader';
import { EmptyState } from '../../components/shared/EmptyState';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import { listExams } from '../../lib/exam-api';

/**
 * فهرست آزمون‌ها — فاز ۸.
 */
export function ExamsPage() {
  const examsQuery = useQuery({
    queryKey: ['exams'],
    queryFn: () => listExams(),
  });

  return (
    <>
      <PageHeader
        title="آزمون‌ها"
        description="آزمون‌های ساخته‌شده و وضعیت برگزاری"
        action={
          <Link to="/exams/generate">
            <Button variant="secondary">
              <Plus className="size-4" aria-hidden="true" />
              ساخت با AI
            </Button>
          </Link>
        }
      />

      {examsQuery.isPending ? <LoadingSkeleton lines={4} /> : null}

      {examsQuery.isError ? (
        <ErrorCard
          title="آزمون‌ها بارگذاری نشد"
          message={
            examsQuery.error instanceof ApiError
              ? examsQuery.error.message
              : 'بارگذاری آزمون‌ها ناموفق بود.'
          }
        />
      ) : null}

      {examsQuery.isSuccess && examsQuery.data.content.length === 0 ? (
        <EmptyState
          icon={<ClipboardCheck className="size-8" aria-hidden="true" />}
          title="هنوز آزمونی نیست"
          description="با «ساخت با AI» اولین آزمون را بسازید."
          action={
            <Link to="/exams/generate">
              <Button variant="secondary">ساخت آزمون</Button>
            </Link>
          }
        />
      ) : null}

      {examsQuery.isSuccess && examsQuery.data.content.length > 0 ? (
        <ul className="space-y-3">
          {examsQuery.data.content.map((exam) => (
            <li key={exam.id} className="border-b border-border py-3">
              <div className="font-medium">{exam.title}</div>
              <div className="mt-1 text-sm text-muted-foreground">{exam.status}</div>
              <div className="mt-2 flex flex-wrap gap-2 text-sm">
                <Link to="/exams/generate" className="text-primary">
                  تولید
                </Link>
                <Link to={`/exams/${exam.id}/manage`} className="text-primary">
                  مدیریت
                </Link>
                <Link to={`/exams/${exam.id}/results`} className="text-primary">
                  نتایج
                </Link>
                <Link to={`/exams/${exam.id}/take`} className="text-primary">
                  شرکت
                </Link>
              </div>
            </li>
          ))}
        </ul>
      ) : null}
    </>
  );
}
