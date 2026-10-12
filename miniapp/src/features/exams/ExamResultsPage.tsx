import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { PageHeader } from '../../components/shared/PageHeader';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { NotFoundPage } from '../misc/NotFoundPage';
import { ApiError } from '../../lib/api-client';
import { getAttemptReport, getExamResults } from '../../lib/exam-api';

/** کارنامه یک تلاش دانش‌آموز. */
export function ExamAttemptResultsPage() {
  const { attemptId } = useParams<{ attemptId: string }>();
  const valid = Boolean(attemptId && /^[0-9a-fA-F-]{36}$/.test(attemptId));

  const reportQuery = useQuery({
    queryKey: ['exam-report', attemptId],
    queryFn: () => getAttemptReport(attemptId!),
    enabled: valid,
  });

  if (!valid) {
    return <NotFoundPage message="کارنامه پیدا نشد." />;
  }

  if (reportQuery.isPending) {
    return <LoadingSkeleton lines={4} label="بارگذاری کارنامه" />;
  }

  if (reportQuery.isError || !reportQuery.data) {
    return (
      <ErrorCard
        title="کارنامه بارگذاری نشد"
        message={
          reportQuery.error instanceof ApiError
            ? reportQuery.error.message
            : 'ارتباط با سرور برقرار نشد.'
        }
      />
    );
  }

  const report = reportQuery.data;

  return (
    <>
      <PageHeader
        title={`نتیجه: ${report.examTitle}`}
        description={`وضعیت ${report.status}`}
        action={
          <Link
            to={`/exams/${report.examId}/take`}
            className="inline-flex items-center gap-1 text-sm text-primary"
          >
            <ArrowRight className="size-4" aria-hidden="true" />
            آزمون
          </Link>
        }
      />

      <p className="mb-4 text-sm">
        نمره: <strong>{report.score ?? '—'}</strong>
        {report.totalScore != null ? ` از ${report.totalScore}` : null}
      </p>

      <ul className="space-y-3">
        {report.questions.map((q) => {
          const answer = report.answers.find((a) => a.questionId === q.id);
          return (
            <li key={q.id} className="border-b border-border pb-3 text-sm">
              <div className="font-medium">{q.questionText}</div>
              <div className="mt-1 text-muted-foreground">
                پاسخ: {answer?.answerText ?? answer?.selectedOptions ?? '—'}
              </div>
              <div className="mt-1">
                نمره: {answer?.awardedScore ?? '—'}
                {answer?.aiGraded ? ' (AI)' : null}
              </div>
              {answer?.aiFeedback ? (
                <div className="mt-1 text-muted-foreground">{answer.aiFeedback}</div>
              ) : null}
            </li>
          );
        })}
      </ul>
    </>
  );
}

/** نتایج تجمیعی آزمون برای معلم. */
export function ExamResultsPage() {
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
          <Link to={`/exams/${id}/take`} className="inline-flex items-center gap-1 text-sm text-primary">
            <ArrowRight className="size-4" aria-hidden="true" />
            آزمون
          </Link>
        }
      />

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
    </>
  );
}
