import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { BookOpen, Plus } from 'lucide-react';
import { useState } from 'react';
import { PageHeader } from '../../components/shared/PageHeader';
import { EmptyState } from '../../components/shared/EmptyState';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import { createBook, listBooks } from '../../lib/book-api';

/**
 * فهرست کتاب‌های مستأجر — داده واقعی از {@code GET /books}.
 */
export function BooksPage() {
  const queryClient = useQueryClient();
  const [creating, setCreating] = useState(false);
  const [title, setTitle] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const booksQuery = useQuery({
    queryKey: ['books'],
    queryFn: () => listBooks(),
  });

  const createMutation = useMutation({
    mutationFn: () => createBook({ title: title.trim() }),
    onSuccess: async () => {
      setTitle('');
      setCreating(false);
      setFormError(null);
      await queryClient.invalidateQueries({ queryKey: ['books'] });
    },
    onError: (error) => {
      setFormError(error instanceof ApiError ? error.message : 'ساخت کتاب انجام نشد.');
    },
  });

  return (
    <>
      <PageHeader
        title="کتاب‌ها"
        description="کتاب‌های درسی مدرسه"
        action={
          <Button
            variant="secondary"
            onClick={() => {
              setCreating((open) => !open);
              setFormError(null);
            }}
          >
            <Plus className="size-4" aria-hidden="true" />
            کتاب جدید
          </Button>
        }
      />

      {creating ? (
        <form
          className="mb-4 flex flex-col gap-3 rounded-xl border border-border bg-card p-4"
          onSubmit={(event) => {
            event.preventDefault();
            if (title.trim().length === 0) {
              setFormError('عنوان کتاب اجباری است.');
              return;
            }
            createMutation.mutate();
          }}
        >
          <label className="flex flex-col gap-1 text-sm">
            <span className="font-medium">عنوان کتاب</span>
            <input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              className="rounded-xl border border-border bg-background px-3 py-2"
              placeholder="مثلاً ریاضی پنجم"
              maxLength={250}
              autoFocus
            />
          </label>
          {formError ? <p className="text-sm text-danger-strong">{formError}</p> : null}
          <div className="flex gap-2">
            <Button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? 'در حال ساخت…' : 'ساخت'}
            </Button>
            <Button
              type="button"
              variant="ghost"
              onClick={() => {
                setCreating(false);
                setFormError(null);
              }}
            >
              انصراف
            </Button>
          </div>
        </form>
      ) : null}

      {booksQuery.isPending ? <LoadingSkeleton lines={4} label="بارگذاری کتاب‌ها" /> : null}

      {booksQuery.isError ? (
        <ErrorCard
          title="کتاب‌ها بارگذاری نشد"
          message={
            booksQuery.error instanceof ApiError
              ? booksQuery.error.message
              : 'ارتباط با سرور برقرار نشد.'
          }
          action={
            <Button variant="secondary" onClick={() => void booksQuery.refetch()}>
              تلاش مجدد
            </Button>
          }
        />
      ) : null}

      {booksQuery.isSuccess && booksQuery.data.totalElements === 0 ? (
        <EmptyState
          title="هنوز کتابی ندارید"
          description="اولین کتاب را بسازید تا بتوانید صفحه اضافه کنید."
          icon={<BookOpen className="size-8" aria-hidden="true" />}
          action={
            <Button onClick={() => setCreating(true)}>
              <Plus className="size-4" aria-hidden="true" />
              ساخت کتاب
            </Button>
          }
        />
      ) : null}

      {booksQuery.isSuccess && booksQuery.data.content.length > 0 ? (
        <ul className="flex flex-col gap-2">
          {booksQuery.data.content.map((book) => (
            <li key={book.id}>
              <Link
                to={`/books/${book.id}`}
                className="flex items-center justify-between rounded-xl border border-border bg-card px-4 py-3 transition-colors hover:bg-muted/50"
              >
                <div>
                  <p className="font-medium">{book.title}</p>
                  <p className="text-sm text-muted-foreground">
                    {[book.gradeLevel, book.publisher, book.totalPages != null ? `${book.totalPages} صفحه` : null]
                      .filter(Boolean)
                      .join(' · ') || 'بدون جزئیات'}
                  </p>
                </div>
                <span className="text-xs text-muted-foreground">{statusLabel(book.status)}</span>
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
    case 'READY':
      return 'آماده';
    case 'PROCESSING':
      return 'در حال پردازش';
    case 'FAILED':
      return 'ناموفق';
    default:
      return 'پیش‌نویس';
  }
}
