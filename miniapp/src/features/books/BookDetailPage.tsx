import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ArrowRight, FileImage, Sparkles, Upload } from 'lucide-react';
import { useRef, useState } from 'react';
import { PageHeader } from '../../components/shared/PageHeader';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { EmptyState } from '../../components/shared/EmptyState';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import {
  analyzePage,
  getBook,
  listBookAnalyses,
  listBookPages,
  uploadBookPage,
  uploadBookPdf,
} from '../../lib/book-api';
import type { ContentAnalysis } from '../../types/api';

/**
 * جزئیات کتاب: آپلود صفحه/PDF، نمایش متن OCR، دکمه تحلیل AI.
 */
export function BookDetailPage() {
  const { id } = useParams<{ id: string }>();
  const queryClient = useQueryClient();
  const fileRef = useRef<HTMLInputElement>(null);
  const [pageNumber, setPageNumber] = useState(1);
  const [uploadNote, setUploadNote] = useState<string | null>(null);
  const [analysisByPage, setAnalysisByPage] = useState<Record<number, ContentAnalysis>>({});

  const bookQuery = useQuery({
    queryKey: ['books', id],
    queryFn: () => getBook(id!),
    enabled: Boolean(id),
  });

  const pagesQuery = useQuery({
    queryKey: ['books', id, 'pages'],
    queryFn: () => listBookPages(id!),
    enabled: Boolean(id),
  });

  const analysesQuery = useQuery({
    queryKey: ['books', id, 'analyses'],
    queryFn: () => listBookAnalyses(id!),
    enabled: Boolean(id),
  });

  const uploadMutation = useMutation({
    mutationFn: async (file: File) => {
      if (file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf')) {
        return uploadBookPdf(id!, file);
      }
      return uploadBookPage(id!, pageNumber, file);
    },
    onSuccess: async () => {
      setUploadNote('بارگذاری موفق بود.');
      await queryClient.invalidateQueries({ queryKey: ['books', id] });
      await queryClient.invalidateQueries({ queryKey: ['books', id, 'pages'] });
    },
    onError: (error) => {
      setUploadNote(error instanceof ApiError ? error.message : 'بارگذاری ناموفق بود.');
    },
  });

  const analyzeMutation = useMutation({
    mutationFn: (n: number) => analyzePage(id!, n),
    onSuccess: async (result, n) => {
      setAnalysisByPage((prev) => ({ ...prev, [n]: result }));
      await queryClient.invalidateQueries({ queryKey: ['books', id, 'analyses'] });
    },
  });

  if (!id) {
    return <ErrorCard title="کتاب نامشخص" message="شناسه کتاب در آدرس نیست." />;
  }

  if (bookQuery.isPending) {
    return <LoadingSkeleton lines={5} label="بارگذاری کتاب" />;
  }

  if (bookQuery.isError) {
    return (
      <ErrorCard
        title="کتاب بارگذاری نشد"
        message={
          bookQuery.error instanceof ApiError
            ? bookQuery.error.message
            : 'ارتباط با سرور برقرار نشد.'
        }
        action={
          <Button variant="secondary" onClick={() => void bookQuery.refetch()}>
            تلاش مجدد
          </Button>
        }
      />
    );
  }

  const book = bookQuery.data;
  const description = [book.gradeLevel, book.publisher].filter(Boolean).join(' · ');

  return (
    <>
      <PageHeader
        title={book.title}
        {...(description ? { description } : {})}
        action={
          <Link to="/books" className="inline-flex items-center gap-1 text-sm text-primary">
            <ArrowRight className="size-4" aria-hidden="true" />
            بازگشت
          </Link>
        }
      />

      <div className="mb-4 grid grid-cols-2 gap-3">
        <StatCard label="صفحات" value={book.totalPages == null ? '۰' : String(book.totalPages)} />
        <StatCard label="وضعیت" value={statusLabel(book.status)} />
      </div>

      <section className="mb-6 rounded-xl border border-border bg-card p-4">
        <h2 className="mb-3 text-sm font-medium">بارگذاری صفحه یا PDF</h2>
        <div className="flex flex-col gap-3">
          <label className="flex flex-col gap-1 text-sm">
            <span className="text-muted-foreground">شماره صفحه (برای تصویر)</span>
            <input
              type="number"
              min={1}
              value={pageNumber}
              onChange={(e) => setPageNumber(Number(e.target.value) || 1)}
              className="rounded-xl border border-border bg-background px-3 py-2"
            />
          </label>
          <input
            ref={fileRef}
            type="file"
            accept="image/*,application/pdf,.pdf"
            className="hidden"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) {
                uploadMutation.mutate(file);
              }
              e.target.value = '';
            }}
          />
          <Button
            type="button"
            disabled={uploadMutation.isPending}
            onClick={() => fileRef.current?.click()}
          >
            <Upload className="size-4" aria-hidden="true" />
            {uploadMutation.isPending ? 'در حال بارگذاری…' : 'انتخاب فایل'}
          </Button>
          {uploadNote ? <p className="text-sm text-muted-foreground">{uploadNote}</p> : null}
        </div>
      </section>

      <h2 className="mb-2 text-sm font-medium text-muted-foreground">صفحات</h2>

      {pagesQuery.isPending ? <LoadingSkeleton lines={3} label="بارگذاری صفحات" /> : null}

      {pagesQuery.isError ? (
        <ErrorCard
          title="صفحات بارگذاری نشد"
          message={
            pagesQuery.error instanceof ApiError
              ? pagesQuery.error.message
              : 'ارتباط با سرور برقرار نشد.'
          }
          action={
            <Button variant="secondary" onClick={() => void pagesQuery.refetch()}>
              تلاش مجدد
            </Button>
          }
        />
      ) : null}

      {pagesQuery.isSuccess && pagesQuery.data.length === 0 ? (
        <EmptyState
          title="هنوز صفحه‌ای نیست"
          description="یک تصویر صفحه یا فایل PDF بارگذاری کنید."
          icon={<FileImage className="size-8" aria-hidden="true" />}
        />
      ) : null}

      {pagesQuery.isSuccess && pagesQuery.data.length > 0 ? (
        <ul className="flex flex-col gap-3">
          {pagesQuery.data.map((page) => {
            const analysis =
              analysisByPage[page.pageNumber] ??
              analysesQuery.data?.find((a) => a.targetId === page.id);
            return (
              <li
                key={page.id}
                className="rounded-xl border border-border bg-card px-4 py-3"
              >
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="font-medium">صفحه {page.pageNumber}</p>
                    <p className="text-sm text-muted-foreground">
                      OCR: {ocrLabel(page.ocrStatus)}
                    </p>
                  </div>
                  <Button
                    variant="secondary"
                    disabled={analyzeMutation.isPending}
                    onClick={() => analyzeMutation.mutate(page.pageNumber)}
                  >
                    <Sparkles className="size-4" aria-hidden="true" />
                    تحلیل
                  </Button>
                </div>
                {page.extractedText ? (
                  <p className="mt-2 whitespace-pre-wrap text-sm text-muted-foreground">
                    {page.extractedText.length > 400
                      ? `${page.extractedText.slice(0, 400)}…`
                      : page.extractedText}
                  </p>
                ) : null}
                {analysis?.resultText ? (
                  <p className="mt-2 rounded-lg bg-muted/40 p-2 text-sm">
                    <span className="font-medium">تحلیل: </span>
                    {analysis.resultText}
                  </p>
                ) : null}
              </li>
            );
          })}
        </ul>
      ) : null}
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

function ocrLabel(status: string): string {
  switch (status) {
    case 'DONE':
      return 'انجام‌شده';
    case 'FAILED':
      return 'ناموفق';
    case 'SKIPPED':
      return 'ردشده (Mock)';
    default:
      return 'در انتظار';
  }
}
