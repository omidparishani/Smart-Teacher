import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { useState } from 'react';
import { PageHeader } from '../../components/shared/PageHeader';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import { createStudent } from '../../lib/student-api';

/**
 * فرم ساخت دانش‌آموز — {@code POST /students}.
 */
export function NewStudentPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [fullName, setFullName] = useState('');
  const [nationalCode, setNationalCode] = useState('');
  const [guardianMobile, setGuardianMobile] = useState('');
  const [error, setError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: () =>
      createStudent({
        fullName: fullName.trim(),
        nationalCode: nationalCode.trim() || null,
        guardianMobile: guardianMobile.trim() || null,
      }),
    onSuccess: async (student) => {
      await queryClient.invalidateQueries({ queryKey: ['students'] });
      void navigate(`/students/${student.id}`, { replace: true });
    },
    onError: (err) => {
      setError(err instanceof ApiError ? err.message : 'ساخت دانش‌آموز انجام نشد.');
    },
  });

  return (
    <>
      <PageHeader
        title="دانش‌آموز جدید"
        action={
          <Link to="/students" className="inline-flex items-center gap-1 text-sm text-primary">
            <ArrowRight className="size-4" aria-hidden="true" />
            بازگشت
          </Link>
        }
      />

      <form
        className="flex flex-col gap-3"
        onSubmit={(event) => {
          event.preventDefault();
          if (fullName.trim().length === 0) {
            setError('نام دانش‌آموز اجباری است.');
            return;
          }
          setError(null);
          mutation.mutate();
        }}
      >
        <Field
          label="نام کامل"
          value={fullName}
          onChange={setFullName}
          required
          maxLength={150}
          autoFocus
        />
        <Field
          label="کد ملی (اختیاری)"
          value={nationalCode}
          onChange={setNationalCode}
          maxLength={20}
          inputMode="numeric"
        />
        <Field
          label="موبایل ولی (اختیاری)"
          value={guardianMobile}
          onChange={setGuardianMobile}
          maxLength={11}
          inputMode="tel"
          placeholder="09xxxxxxxxx"
        />

        {error ? <p className="text-sm text-danger-strong">{error}</p> : null}

        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'در حال ثبت…' : 'ثبت دانش‌آموز'}
        </Button>
      </form>
    </>
  );
}

function Field(props: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  required?: boolean;
  maxLength?: number;
  inputMode?: 'numeric' | 'tel' | 'text';
  placeholder?: string;
  autoFocus?: boolean;
}) {
  return (
    <label className="flex flex-col gap-1 text-sm">
      <span className="font-medium">{props.label}</span>
      <input
        value={props.value}
        onChange={(event) => props.onChange(event.target.value)}
        className="rounded-xl border border-border bg-background px-3 py-2"
        required={props.required}
        maxLength={props.maxLength}
        inputMode={props.inputMode}
        placeholder={props.placeholder}
        autoFocus={props.autoFocus}
      />
    </label>
  );
}
