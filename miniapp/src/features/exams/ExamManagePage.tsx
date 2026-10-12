import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { useState } from 'react';
import { PageHeader } from '../../components/shared/PageHeader';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { Button } from '../../components/shared/Button';
import { NotFoundPage } from '../misc/NotFoundPage';
import { ApiError } from '../../lib/api-client';
import {
  deleteQuestion,
  getExam,
  getExamBlueprint,
  publishExam,
  updateQuestion,
  type CreateExamQuestionBody,
  type ExamQuestion,
} from '../../lib/exam-api';

/**
 * مدیریت آزمون — ویرایش سؤال‌ها، مشاهده blueprint، انتشار.
 */
export function ExamManagePage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const [publishNote, setPublishNote] = useState<string | null>(null);
  const [eitaaChatId, setEitaaChatId] = useState('');
  const [editError, setEditError] = useState<string | null>(null);

  const idValid = Boolean(id && /^[0-9a-fA-F-]{36}$/.test(id));

  const examQuery = useQuery({
    queryKey: ['exams', id],
    queryFn: () => getExam(id!),
    enabled: idValid,
  });

  const blueprintQuery = useQuery({
    queryKey: ['exams', id, 'blueprint'],
    queryFn: () => getExamBlueprint(id!),
    enabled: idValid,
    retry: false,
  });

  const publishMutation = useMutation({
    mutationFn: () =>
      publishExam(id!, eitaaChatId.trim() ? { eitaaChatId: eitaaChatId.trim() } : {}),
    onSuccess: async () => {
      setPublishNote(
        eitaaChatId.trim()
          ? 'منتشر شد؛ در صورت تنظیم chatId، پیام exm_ به ایتا ارسال شد.'
          : 'منتشر شد.',
      );
      await queryClient.invalidateQueries({ queryKey: ['exams', id] });
      await queryClient.invalidateQueries({ queryKey: ['exams'] });
    },
    onError: (error) => {
      setPublishNote(error instanceof ApiError ? error.message : 'انتشار انجام نشد.');
    },
  });

  const updateMutation = useMutation({
    mutationFn: (params: { question: ExamQuestion; text: string }) => {
      const body: CreateExamQuestionBody = {
        type: params.question.type,
        questionText: params.text.trim(),
        score: params.question.score,
        orderIndex: params.question.orderIndex,
      };
      if (params.question.options != null && params.question.options !== '') {
        body.options = params.question.options;
      }
      if (params.question.correctAnswer != null && params.question.correctAnswer !== '') {
        body.correctAnswer = params.question.correctAnswer;
      }
      return updateQuestion(id!, params.question.id, body);
    },
    onSuccess: async () => {
      setEditError(null);
      await queryClient.invalidateQueries({ queryKey: ['exams', id] });
    },
    onError: (error) => {
      setEditError(error instanceof ApiError ? error.message : 'ویرایش سؤال انجام نشد.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (questionId: string) => deleteQuestion(id!, questionId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['exams', id] });
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
        description={`وضعیت: ${exam.status} · ${exam.questions.length} سؤال`}
        action={
          <Link to="/exams" className="inline-flex items-center gap-1 text-sm text-primary">
            <ArrowRight className="size-4" aria-hidden="true" />
            فهرست
          </Link>
        }
      />

      <div className="mb-4 flex flex-wrap gap-2 text-sm">
        <Link to={`/exams/${id}/take`} className="text-primary">
          شرکت در آزمون
        </Link>
        <Link to={`/exams/${id}/results`} className="text-primary">
          نتایج
        </Link>
      </div>

      {blueprintQuery.isSuccess ? (
        <section className="mb-4 rounded-xl border border-border bg-card p-3 text-sm">
          <h2 className="mb-2 font-medium">طرح AI</h2>
          <p>
            کتاب · صفحات {blueprintQuery.data.pageFrom}–{blueprintQuery.data.pageTo}
          </p>
          <p className="text-muted-foreground">{blueprintQuery.data.questionTypeDistribution}</p>
        </section>
      ) : null}

      <section className="mb-4 space-y-3 rounded-xl border border-border bg-card p-4">
        <h2 className="text-sm font-medium">انتشار</h2>
        <label className="block text-sm">
          chatId ایتا (اختیاری)
          <input
            className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            value={eitaaChatId}
            onChange={(e) => setEitaaChatId(e.target.value)}
            placeholder="@group_username"
          />
        </label>
        <Button
          disabled={publishMutation.isPending || exam.questions.length === 0}
          onClick={() => publishMutation.mutate()}
        >
          انتشار آزمون
        </Button>
        {publishNote ? <p className="text-sm text-muted-foreground">{publishNote}</p> : null}
      </section>

      <section className="space-y-4">
        <h2 className="text-sm font-medium">سؤال‌ها</h2>
        {editError ? <p className="text-sm text-destructive">{editError}</p> : null}
        {exam.questions.map((q) => (
          <QuestionEditor
            key={q.id}
            question={q}
            saving={updateMutation.isPending}
            onSave={(text) => updateMutation.mutate({ question: q, text })}
            onDelete={() => deleteMutation.mutate(q.id)}
          />
        ))}
      </section>
    </>
  );
}

function QuestionEditor(props: {
  question: ExamQuestion;
  saving: boolean;
  onSave: (text: string) => void;
  onDelete: () => void;
}) {
  const { question, saving, onSave, onDelete } = props;
  const [text, setText] = useState(question.questionText);

  return (
    <div className="border-b border-border pb-4">
      <div className="mb-1 text-xs text-muted-foreground">
        {question.type} · نمره {question.score}
        {question.aiGenerated ? ' · AI' : null}
      </div>
      <textarea
        className="min-h-20 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm"
        value={text}
        onChange={(e) => setText(e.target.value)}
      />
      <div className="mt-2 flex gap-2">
        <Button
          variant="secondary"
          disabled={saving || text.trim() === question.questionText}
          onClick={() => onSave(text)}
        >
          ذخیره
        </Button>
        <Button variant="secondary" onClick={onDelete}>
          حذف
        </Button>
      </div>
    </div>
  );
}
