import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ArrowRight, Send, Sparkles } from 'lucide-react';
import { useState } from 'react';
import { PageHeader } from '../../components/shared/PageHeader';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { EmptyState } from '../../components/shared/EmptyState';
import { Button } from '../../components/shared/Button';
import { NotFoundPage } from '../misc/NotFoundPage';
import { ApiError } from '../../lib/api-client';
import {
  evaluateSubmission,
  getAssignment,
  listReviews,
  listSubmissions,
  publishAssignment,
  reevaluateAll,
  reviewSubmission,
  submitAnswer,
} from '../../lib/assignment-api';
import { listClassStudents } from '../../lib/classroom-api';
import type { Evaluation, ReviewAction, TeacherReview } from '../../types/api';

/**
 * جزئیات تکلیف: انتشار، پاسخ، تصحیح AI و بازبینی معلم.
 *
 * <p>⚠️ پاسخ فقط از همین فرم Mini App ثبت می‌شود — نه از چت ایتا.
 */
export function AssignmentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const [studentId, setStudentId] = useState('');
  const [answerText, setAnswerText] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [publishNote, setPublishNote] = useState<string | null>(null);
  const [evaluations, setEvaluations] = useState<Record<string, Evaluation>>({});
  const [reviews, setReviews] = useState<Record<string, TeacherReview[]>>({});
  const [reviewScore, setReviewScore] = useState<Record<string, string>>({});
  const [reviewComment, setReviewComment] = useState<Record<string, string>>({});

  const idValid = Boolean(id && /^[0-9a-fA-F-]{36}$/.test(id));

  const assignmentQuery = useQuery({
    queryKey: ['assignments', id],
    queryFn: () => getAssignment(id!),
    enabled: idValid,
  });

  const submissionsQuery = useQuery({
    queryKey: ['assignments', id, 'submissions'],
    queryFn: () => listSubmissions(id!),
    enabled: idValid,
  });

  const studentsQuery = useQuery({
    queryKey: ['classes', assignmentQuery.data?.classroomId, 'students'],
    queryFn: () => listClassStudents(assignmentQuery.data!.classroomId),
    enabled: Boolean(assignmentQuery.data?.classroomId),
  });

  const publishMutation = useMutation({
    mutationFn: () => publishAssignment(id!),
    onSuccess: async (result) => {
      setPublishNote(
        result.eitaaMessageStatus === 'SKIPPED'
          ? 'منتشر شد (ارسال ایتا رد شد — بدون chatId).'
          : result.eitaaMessageStatus === 'FAILED'
            ? 'منتشر شد؛ ارسال به ایتا ناموفق بود.'
            : 'منتشر شد.',
      );
      await queryClient.invalidateQueries({ queryKey: ['assignments', id] });
      await queryClient.invalidateQueries({ queryKey: ['assignments'] });
    },
    onError: (error) => {
      setPublishNote(error instanceof ApiError ? error.message : 'انتشار انجام نشد.');
    },
  });

  const submitMutation = useMutation({
    mutationFn: () =>
      submitAnswer(id!, { studentId, answerText: answerText.trim() }),
    onSuccess: async () => {
      setAnswerText('');
      setFormError(null);
      await queryClient.invalidateQueries({ queryKey: ['assignments', id, 'submissions'] });
      await queryClient.invalidateQueries({ queryKey: ['assignments', id] });
    },
    onError: (error) => {
      setFormError(error instanceof ApiError ? error.message : 'ثبت پاسخ انجام نشد.');
    },
  });

  const evaluateMutation = useMutation({
    mutationFn: (submissionId: string) => evaluateSubmission(id!, submissionId),
    onSuccess: (result) => {
      setEvaluations((prev) => ({ ...prev, [result.submissionId]: result }));
    },
  });

  const reevaluateMutation = useMutation({
    mutationFn: () => reevaluateAll(id!),
    onSuccess: (results) => {
      const map: Record<string, Evaluation> = {};
      for (const item of results) {
        map[item.submissionId] = item;
      }
      setEvaluations(map);
    },
  });

  const reviewMutation = useMutation({
    mutationFn: (params: {
      submissionId: string;
      action: ReviewAction;
      newScore?: number | null;
      comment?: string | null;
    }) => {
      const body: {
        action: ReviewAction;
        newScore?: number | null;
        comment?: string | null;
      } = { action: params.action };
      if (params.newScore !== undefined) {
        body.newScore = params.newScore;
      }
      if (params.comment !== undefined) {
        body.comment = params.comment;
      }
      return reviewSubmission(id!, params.submissionId, body);
    },
    onSuccess: async (_review, vars) => {
      const list = await listReviews(id!, vars.submissionId);
      setReviews((prev) => ({ ...prev, [vars.submissionId]: list }));
      await queryClient.invalidateQueries({ queryKey: ['assignments', id, 'submissions'] });
    },
  });

  if (!idValid) {
    return <NotFoundPage message="تکلیف خواسته‌شده پیدا نشد." />;
  }

  if (assignmentQuery.isPending) {
    return <LoadingSkeleton lines={5} label="بارگذاری تکلیف" />;
  }

  if (assignmentQuery.isError) {
    return (
      <ErrorCard
        title="تکلیف بارگذاری نشد"
        message={
          assignmentQuery.error instanceof ApiError
            ? assignmentQuery.error.message
            : 'ارتباط با سرور برقرار نشد.'
        }
        action={
          <Button variant="secondary" onClick={() => void assignmentQuery.refetch()}>
            تلاش مجدد
          </Button>
        }
      />
    );
  }

  const assignment = assignmentQuery.data;
  const description = assignment.classroomName ?? '';

  return (
    <>
      <PageHeader
        title={assignment.title}
        {...(description ? { description } : {})}
        action={
          <Link to="/assignments" className="inline-flex items-center gap-1 text-sm text-primary">
            <ArrowRight className="size-4" aria-hidden="true" />
            بازگشت
          </Link>
        }
      />

      <div className="mb-4 flex flex-col gap-2 rounded-xl border border-border bg-card p-4 text-sm">
        <p>
          <span className="text-muted-foreground">وضعیت: </span>
          {statusLabel(assignment.status)}
        </p>
        {assignment.description ? <p>{assignment.description}</p> : null}
        <p>
          <span className="text-muted-foreground">ارسال‌ها: </span>
          {assignment.submissionCount}
        </p>
        <div className="flex flex-wrap gap-2">
          {assignment.status === 'DRAFT' ? (
            <Button
              onClick={() => publishMutation.mutate()}
              disabled={publishMutation.isPending}
            >
              <Send className="size-4" aria-hidden="true" />
              {publishMutation.isPending ? 'در حال انتشار…' : 'انتشار'}
            </Button>
          ) : null}
          <Button
            variant="secondary"
            onClick={() => reevaluateMutation.mutate()}
            disabled={reevaluateMutation.isPending}
          >
            <Sparkles className="size-4" aria-hidden="true" />
            {reevaluateMutation.isPending ? 'تصحیح همه…' : 'تصحیح همه'}
          </Button>
        </div>
        {publishNote ? <p className="text-muted-foreground">{publishNote}</p> : null}
      </div>

      <h2 className="mb-2 text-sm font-medium text-muted-foreground">ثبت پاسخ (Mini App)</h2>
      <form
        className="mb-6 flex flex-col gap-3 rounded-xl border border-border bg-card p-4"
        onSubmit={(event) => {
          event.preventDefault();
          if (!studentId) {
            setFormError('دانش‌آموز را انتخاب کنید.');
            return;
          }
          if (answerText.trim().length === 0) {
            setFormError('متن پاسخ اجباری است.');
            return;
          }
          submitMutation.mutate();
        }}
      >
        <label className="flex flex-col gap-1 text-sm">
          <span className="font-medium">دانش‌آموز</span>
          <select
            value={studentId}
            onChange={(event) => setStudentId(event.target.value)}
            className="rounded-xl border border-border bg-background px-3 py-2"
          >
            <option value="">انتخاب…</option>
            {(studentsQuery.data?.content ?? []).map((student) => (
              <option key={student.studentId} value={student.studentId}>
                {student.fullName}
              </option>
            ))}
          </select>
        </label>
        <label className="flex flex-col gap-1 text-sm">
          <span className="font-medium">پاسخ متنی</span>
          <textarea
            value={answerText}
            onChange={(event) => setAnswerText(event.target.value)}
            className="min-h-24 rounded-xl border border-border bg-background px-3 py-2"
            placeholder="پاسخ را اینجا بنویسید…"
          />
        </label>
        {formError ? <p className="text-sm text-danger-strong">{formError}</p> : null}
        <Button type="submit" disabled={submitMutation.isPending}>
          {submitMutation.isPending ? 'در حال ثبت…' : 'ثبت پاسخ'}
        </Button>
      </form>

      <h2 className="mb-2 text-sm font-medium text-muted-foreground">ارسال‌ها و تصحیح</h2>
      {submissionsQuery.isPending ? (
        <LoadingSkeleton lines={3} label="بارگذاری ارسال‌ها" />
      ) : null}
      {submissionsQuery.isSuccess && submissionsQuery.data.length === 0 ? (
        <EmptyState title="هنوز ارسالی نیست" description="اولین پاسخ را از فرم بالا ثبت کنید." />
      ) : null}
      {submissionsQuery.isSuccess && submissionsQuery.data.length > 0 ? (
        <ul className="flex flex-col gap-3">
          {submissionsQuery.data.map((sub) => {
            const evaluation = evaluations[sub.id];
            const reviewList = reviews[sub.id] ?? [];
            return (
              <li
                key={sub.id}
                className="rounded-xl border border-border bg-card px-4 py-3 text-sm"
              >
                <p className="font-medium">{sub.studentName ?? sub.studentId}</p>
                <p className="mt-1 whitespace-pre-wrap text-muted-foreground">{sub.answerText}</p>
                <p className="mt-1 text-muted-foreground">
                  وضعیت: {sub.status}
                  {sub.finalScore != null ? ` · نمره نهایی: ${sub.finalScore}` : ''}
                </p>

                <div className="mt-3 flex flex-wrap gap-2">
                  <Button
                    variant="secondary"
                    disabled={evaluateMutation.isPending}
                    onClick={() => evaluateMutation.mutate(sub.id)}
                  >
                    تصحیح AI
                  </Button>
                </div>

                {evaluation ? (
                  <div className="mt-3 rounded-lg bg-muted/40 p-3">
                    <p>
                      نمره AI: {evaluation.aiScore ?? '—'}
                      {evaluation.requiresTeacherReview ? ' · نیاز به بازبینی' : ''}
                    </p>
                    {evaluation.feedback ? (
                      <p className="mt-1 text-muted-foreground">{evaluation.feedback}</p>
                    ) : null}
                  </div>
                ) : null}

                <div className="mt-3 flex flex-col gap-2 border-t border-border pt-3">
                  <p className="font-medium">بازبینی معلم</p>
                  <input
                    type="number"
                    step="0.5"
                    placeholder="نمره جدید"
                    value={reviewScore[sub.id] ?? ''}
                    onChange={(e) =>
                      setReviewScore((prev) => ({ ...prev, [sub.id]: e.target.value }))
                    }
                    className="rounded-xl border border-border bg-background px-3 py-2"
                  />
                  <input
                    type="text"
                    placeholder="توضیح"
                    value={reviewComment[sub.id] ?? ''}
                    onChange={(e) =>
                      setReviewComment((prev) => ({ ...prev, [sub.id]: e.target.value }))
                    }
                    className="rounded-xl border border-border bg-background px-3 py-2"
                  />
                  <div className="flex flex-wrap gap-2">
                    <Button
                      variant="secondary"
                      disabled={reviewMutation.isPending}
                      onClick={() =>
                        reviewMutation.mutate({
                          submissionId: sub.id,
                          action: 'ACCEPT',
                          newScore: parseScore(reviewScore[sub.id]),
                          comment: reviewComment[sub.id] ?? null,
                        })
                      }
                    >
                      تأیید
                    </Button>
                    <Button
                      variant="secondary"
                      disabled={reviewMutation.isPending}
                      onClick={() =>
                        reviewMutation.mutate({
                          submissionId: sub.id,
                          action: 'EDIT_SCORE',
                          newScore: parseScore(reviewScore[sub.id]),
                          comment: reviewComment[sub.id] ?? null,
                        })
                      }
                    >
                      ویرایش نمره
                    </Button>
                  </div>
                  {reviewList.length > 0 ? (
                    <ul className="mt-2 space-y-1 text-muted-foreground">
                      {reviewList.map((r) => (
                        <li key={r.id}>
                          {r.action}
                          {r.newScore != null ? ` → ${r.newScore}` : ''}
                          {r.comment ? ` · ${r.comment}` : ''}
                        </li>
                      ))}
                    </ul>
                  ) : null}
                </div>
              </li>
            );
          })}
        </ul>
      ) : null}
    </>
  );
}

function parseScore(value: string | undefined): number | null {
  if (value == null || value.trim() === '') {
    return null;
  }
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
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
