import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { useAuthStore } from '../../stores/auth.store';
import { getSessionStudentId, setSessionStudentId } from '../../lib/student-identity';
import { PageHeader } from '../../components/shared/PageHeader';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { Button } from '../../components/shared/Button';
import { NotFoundPage } from '../misc/NotFoundPage';
import { ApiError } from '../../lib/api-client';
import { getExam, saveAnswer, startExam, submitAttempt, type ExamQuestion } from '../../lib/exam-api';
import { listClassStudents } from '../../lib/classroom-api';

/**
 * برگزاری آزمون — شروع، پاسخ، ارسال نهایی با مهلت.
 */
export function ExamTakePage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const user = useAuthStore((state) => state.user);
  const sessionStudentId = getSessionStudentId();
  const isStudentRole = user?.roles.includes('TENANT_STUDENT') ?? false;
  const prefilledStudentId = sessionStudentId ?? '';

  // ⚠️ انتخاب کاربر روی مقدار پیش‌پرشده سوار می‌شود — به‌صورت <b>مشتق</b>،
  // نه با یک effect که state را همگام کند. همگام‌سازی با effect یک رندر
  // آبشاری می‌سازد، و همین الگو بود که قاعده
  // `react-hooks/set-state-in-effect` می‌گرفت.
  const [pickedStudentId, setPickedStudentId] = useState('');
  const studentId = pickedStudentId || prefilledStudentId;

  const showStudentPicker = !prefilledStudentId || !isStudentRole;
  const [attemptId, setAttemptId] = useState<string | null>(null);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [deadline, setDeadline] = useState<Date | null>(null);

  // 🔴 این با `deadline` و در همان callback پر می‌شود — نه هنگام رندر.
  // خواندن `Date.now()` در رندر تابع را ناخالص می‌کند. پس ساعت اینجا
  // نگه داشته می‌شود و تایمر پایین آن را جلو می‌برد.
  const [nowMs, setNowMs] = useState(0);

  const idValid = Boolean(id && /^[0-9a-fA-F-]{36}$/.test(id));

  const examQuery = useQuery({
    queryKey: ['exams', id],
    queryFn: () => getExam(id!),
    enabled: idValid,
  });

  const studentsQuery = useQuery({
    queryKey: ['classes', examQuery.data?.classroomId, 'students'],
    queryFn: () => listClassStudents(examQuery.data!.classroomId),
    enabled: Boolean(examQuery.data?.classroomId),
  });

  // 🔴 بدون این تایمر، شمارش معکوس عملاً <b>یخ می‌زد</b>. نسخه پیشین
  // `Date.now()` را داخل `useMemo` با وابستگی `[deadline, answers]`
  // می‌خواند، پس برچسب فقط وقتی عوض می‌شد که کاربر جوابی را تغییر دهد —
  // یعنی زمان نمایش‌داده‌شده با زمان واقعی فاصله می‌گرفت و کسی هم
  // متوجه نمی‌شد. حالا هر ثانیه یک‌بار ساعت تازه می‌شود.
  //
  // ⚠️ بدنه effect فقط `setInterval` می‌سازد و <i>همزمان</i> state ست
  // نمی‌کند — که همان چیزی است که قاعده set-state-in-effect منع می‌کند.
  useEffect(() => {
    if (!deadline) {
      return;
    }

    const timer = setInterval(() => setNowMs(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [deadline]);

  const remainingLabel = useMemo(() => {
    if (!deadline) return null;
    const ms = deadline.getTime() - nowMs;
    if (ms <= 0) return 'مهلت تمام شد';
    const mins = Math.floor(ms / 60000);
    const secs = Math.floor((ms % 60000) / 1000);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }, [deadline, nowMs]);

  const startMutation = useMutation({
    mutationFn: () => startExam(id!, studentId),
    onSuccess: (attempt) => {
      setAttemptId(attempt.id);
      setFormError(null);
      if (examQuery.data?.durationMinutes) {
        // ⚠️ یک‌بار خوانده می‌شود و هر دو state از همان مقدار پر
        // می‌شوند، تا `nowMs` و `deadline` همیشه هم‌زمان ست شوند.
        const now = Date.now();
        setNowMs(now);
        setDeadline(new Date(now + examQuery.data.durationMinutes * 60_000));
      }
    },
    onError: (error) => {
      setFormError(error instanceof ApiError ? error.message : 'شروع آزمون ناموفق بود.');
    },
  });

  const submitMutation = useMutation({
    mutationFn: async () => {
      if (!attemptId) throw new Error('تلاش شروع نشده');
      const questions = examQuery.data?.questions ?? [];
      for (const q of questions) {
        const value = answers[q.id]?.trim();
        if (value) {
          await saveAnswer(attemptId, studentId, {
            questionId: q.id,
            answerText: value,
          });
        }
      }
      return submitAttempt(attemptId, studentId);
    },
    onSuccess: async (attempt) => {
      await queryClient.invalidateQueries({ queryKey: ['exams', id] });
      void navigate(`/exams/attempts/${attempt.id}/results`);
    },
    onError: (error) => {
      setFormError(error instanceof ApiError ? error.message : 'ارسال آزمون ناموفق بود.');
    },
  });

  if (!idValid) {
    return <NotFoundPage message="آزمون خواسته‌شده پیدا نشد." />;
  }

  if (examQuery.isPending) {
    return <LoadingSkeleton lines={5} label="بارگذاری آزمون" />;
  }

  if (examQuery.isError || !examQuery.data) {
    return (
      <ErrorCard
        title="آزمون بارگذاری نشد"
        message={
          examQuery.error instanceof ApiError
            ? examQuery.error.message
            : 'ارتباط با سرور برقرار نشد.'
        }
      />
    );
  }

  const exam = examQuery.data;

  return (
    <>
      <PageHeader
        title={exam.title}
        description={exam.description ?? `وضعیت: ${exam.status}`}
        action={
          <Link to="/exams" className="inline-flex items-center gap-1 text-sm text-primary">
            <ArrowRight className="size-4" aria-hidden="true" />
            بازگشت
          </Link>
        }
      />

      {!attemptId ? (
        <section className="space-y-3">
          {showStudentPicker ? (
            <label className="block text-sm">
              دانش‌آموز
              <select
                className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
                value={studentId}
                onChange={(e) => {
                  setPickedStudentId(e.target.value);
                  if (e.target.value) {
                    setSessionStudentId(e.target.value);
                  }
                }}
              >
                <option value="">انتخاب کنید</option>
                {(studentsQuery.data?.content ?? []).map((s) => (
                  <option key={s.studentId} value={s.studentId}>
                    {s.fullName}
                  </option>
                ))}
              </select>
            </label>
          ) : (
            <p className="text-sm text-muted-foreground">
              شناسه دانش‌آموز از نشست: {studentId.slice(0, 8)}…
            </p>
          )}
          {formError ? <p className="text-sm text-destructive">{formError}</p> : null}
          <Button
            disabled={!studentId || startMutation.isPending}
            onClick={() => startMutation.mutate()}
          >
            شروع آزمون
          </Button>
          <Link to={`/exams/${id}/results`} className="block text-sm text-primary">
            مشاهده نتایج معلم
          </Link>
        </section>
      ) : (
        <section className="space-y-4">
          {remainingLabel ? (
            <p className="text-sm text-muted-foreground">زمان باقی‌مانده: {remainingLabel}</p>
          ) : null}
          {exam.questions.map((q) => (
            <QuestionField
              key={q.id}
              question={q}
              value={answers[q.id] ?? ''}
              onChange={(value) => setAnswers((prev) => ({ ...prev, [q.id]: value }))}
            />
          ))}
          {formError ? <p className="text-sm text-destructive">{formError}</p> : null}
          <Button
            disabled={submitMutation.isPending}
            onClick={() => submitMutation.mutate()}
          >
            ارسال نهایی
          </Button>
        </section>
      )}
    </>
  );
}

function QuestionField(props: {
  question: ExamQuestion;
  value: string;
  onChange: (value: string) => void;
}) {
  const { question, value, onChange } = props;
  let options: string[] = [];
  if (question.options) {
    try {
      const parsed = JSON.parse(question.options) as unknown;
      if (Array.isArray(parsed)) {
        options = parsed.map(String);
      }
    } catch {
      options = [];
    }
  }

  return (
    <div className="border-b border-border pb-4">
      <p className="mb-2 text-sm font-medium">
        {question.questionText}
        <span className="ms-2 text-xs text-muted-foreground">({question.type})</span>
      </p>
      {options.length > 0 ? (
        <div className="space-y-2">
          {options.map((opt) => (
            <label key={opt} className="flex items-center gap-2 text-sm">
              <input
                type="radio"
                name={question.id}
                checked={value === opt}
                onChange={() => onChange(opt)}
              />
              {opt}
            </label>
          ))}
        </div>
      ) : (
        <textarea
          className="mt-1 min-h-24 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder="پاسخ خود را بنویسید"
        />
      )}
    </div>
  );
}
