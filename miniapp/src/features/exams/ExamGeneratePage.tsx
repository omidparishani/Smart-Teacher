import { useMutation, useQuery } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { useState } from 'react';
import { PageHeader } from '../../components/shared/PageHeader';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import { generateExam } from '../../lib/exam-api';
import { listBooks } from '../../lib/book-api';
import { listClasses } from '../../lib/classroom-api';

/**
 * ساخت آزمون با AI — {@code POST /exams/generate}.
 */
export function ExamGeneratePage() {
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [classroomId, setClassroomId] = useState('');
  const [bookId, setBookId] = useState('');
  const [pageFrom, setPageFrom] = useState('1');
  const [pageTo, setPageTo] = useState('5');
  const [mcqCount, setMcqCount] = useState('3');
  const [trueFalseCount, setTrueFalseCount] = useState('0');
  const [durationMinutes, setDurationMinutes] = useState('30');
  const [formError, setFormError] = useState<string | null>(null);

  const classesQuery = useQuery({
    queryKey: ['classes'],
    queryFn: () => listClasses(),
  });

  const booksQuery = useQuery({
    queryKey: ['books'],
    queryFn: () => listBooks(),
  });

  const generateMutation = useMutation({
    mutationFn: () =>
      generateExam({
        title: title.trim(),
        classroomId,
        bookId,
        pageFrom: Number.parseInt(pageFrom, 10),
        pageTo: Number.parseInt(pageTo, 10),
        durationMinutes: Number.parseInt(durationMinutes, 10),
        mcqCount: Number.parseInt(mcqCount, 10),
        trueFalseCount: Number.parseInt(trueFalseCount, 10),
      }),
    onSuccess: (exam) => {
      setFormError(null);
      void navigate(`/exams/${exam.id}/manage`);
    },
    onError: (error) => {
      setFormError(error instanceof ApiError ? error.message : 'تولید آزمون انجام نشد.');
    },
  });

  if (classesQuery.isPending || booksQuery.isPending) {
    return <LoadingSkeleton lines={4} label="بارگذاری فرم" />;
  }

  if (classesQuery.isError || booksQuery.isError) {
    return (
      <ErrorCard
        title="فرم بارگذاری نشد"
        message="کلاس‌ها یا کتاب‌ها در دسترس نیستند."
      />
    );
  }

  return (
    <>
      <PageHeader
        title="ساخت آزمون با AI"
        description="تولید سؤال از کتاب و بازه صفحه"
        action={
          <Link to="/exams" className="inline-flex items-center gap-1 text-sm text-primary">
            <ArrowRight className="size-4" aria-hidden="true" />
            بازگشت
          </Link>
        }
      />

      <form
        className="flex flex-col gap-3 rounded-xl border border-border bg-card p-4"
        onSubmit={(event) => {
          event.preventDefault();
          if (title.trim().length === 0) {
            setFormError('عنوان آزمون اجباری است.');
            return;
          }
          if (!classroomId || !bookId) {
            setFormError('کلاس و کتاب را انتخاب کنید.');
            return;
          }
          const from = Number.parseInt(pageFrom, 10);
          const to = Number.parseInt(pageTo, 10);
          if (!Number.isFinite(from) || !Number.isFinite(to) || to < from) {
            setFormError('بازه صفحه نامعتبر است.');
            return;
          }
          generateMutation.mutate();
        }}
      >
        <label className="block text-sm">
          عنوان
          <input
            className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="آزمون فصل ۳"
          />
        </label>

        <label className="block text-sm">
          کلاس
          <select
            className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            value={classroomId}
            onChange={(e) => setClassroomId(e.target.value)}
          >
            <option value="">انتخاب کنید</option>
            {(classesQuery.data?.content ?? []).map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </label>

        <label className="block text-sm">
          کتاب
          <select
            className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
            value={bookId}
            onChange={(e) => setBookId(e.target.value)}
          >
            <option value="">انتخاب کنید</option>
            {(booksQuery.data?.content ?? []).map((b) => (
              <option key={b.id} value={b.id}>
                {b.title}
              </option>
            ))}
          </select>
        </label>

        <div className="grid grid-cols-2 gap-3">
          <label className="block text-sm">
            از صفحه
            <input
              type="number"
              min={1}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
              value={pageFrom}
              onChange={(e) => setPageFrom(e.target.value)}
            />
          </label>
          <label className="block text-sm">
            تا صفحه
            <input
              type="number"
              min={1}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
              value={pageTo}
              onChange={(e) => setPageTo(e.target.value)}
            />
          </label>
        </div>

        <div className="grid grid-cols-3 gap-3">
          <label className="block text-sm">
            چهارگزینه‌ای
            <input
              type="number"
              min={1}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
              value={mcqCount}
              onChange={(e) => setMcqCount(e.target.value)}
            />
          </label>
          <label className="block text-sm">
            درست/نادرست
            <input
              type="number"
              min={0}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
              value={trueFalseCount}
              onChange={(e) => setTrueFalseCount(e.target.value)}
            />
          </label>
          <label className="block text-sm">
            مدت (دقیقه)
            <input
              type="number"
              min={1}
              className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2"
              value={durationMinutes}
              onChange={(e) => setDurationMinutes(e.target.value)}
            />
          </label>
        </div>

        {formError ? <p className="text-sm text-destructive">{formError}</p> : null}

        <Button type="submit" disabled={generateMutation.isPending}>
          {generateMutation.isPending ? 'در حال تولید…' : 'تولید آزمون'}
        </Button>
      </form>
    </>
  );
}
