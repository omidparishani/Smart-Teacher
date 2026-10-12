import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { PageHeader } from '../../components/shared/PageHeader';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { NotFoundPage } from '../misc/NotFoundPage';
import { ApiError } from '../../lib/api-client';
import { getExamResults } from '../../lib/exam-api';

/** نتایج تجمیعی آزمون برای معلم — {@code GET /exams/{id}/results}. */
export function ExamTeacherResultsPage() {
  const { id } = useParams<{ id: string }>();
  const valid = Boolean(id && /^[0-9a-fA-F-]{36}$/.test(id));

  const resultsQuery = useQuery({
    queryKey: ['exam-results', id],
    queryFn: () => getExamResults(id!),
    enabled: valid,
  });

  if (!valid) {
    return <NotFoundPage message="نتایج پیدا نشد." />;
  }

  if (resultsQuery.isPending) {
    return <LoadingSkeleton lines={4} label="بارگذاری نتایج" />;
  }

  if (resultsQuery.isError || !resultsQuery.data) {
    return (
      <ErrorCard
        title="نتایج بارگذاری نشد"
        message={
          resultsQuery.error instanceof ApiError
            ? resultsQuery.error.message
            : 'ارتباط با سرور برقرار نشد.'
        }
      />
    );
  }

  const results = resultsQuery.data;

  return (
    <>
      <PageHeader
        title={`نتایج: ${results.title}`}
        description={`${results.attemptCount} تلاش · میانگین ${results.averageScore}`}
        action={
          <Link
            to={`/exams/${id}/manage`}
            className="inline-flex items-center gap-1 text-sm text-primary"
          >
            <ArrowRight className="size-4" aria-hidden="true" />
            مدیریت
          </Link>
        }
      />

      {results.attempts.length === 0 ? (
        <p className="text-sm text-muted-foreground">هنوز تلاشی ثبت نشده است.</p>
      ) : (
        <ul className="space-y-2">
          {results.attempts.map((a) => (
            <li key={a.id}>
              <Link
                to={`/exams/attempts/${a.id}/results`}
                className="block border-b border-border py-3 text-sm hover:text-primary"
              >
                تلاش #{a.attemptNumber} — نمره {a.score ?? '—'} ({a.status})
              </Link>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
