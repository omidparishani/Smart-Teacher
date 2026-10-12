import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { BookOpen, ClipboardList, GraduationCap, Users } from 'lucide-react';
import { useAuthStore } from '../../stores/auth.store';
import { PageHeader } from '../../components/shared/PageHeader';
import { LoadingSkeleton } from '../../components/shared/LoadingSkeleton';
import { ErrorCard } from '../../components/shared/ErrorCard';
import { Button } from '../../components/shared/Button';
import { ApiError } from '../../lib/api-client';
import { listClasses } from '../../lib/classroom-api';
import { listStudents } from '../../lib/student-api';

/**
 * زبانه خانه — خلاصه واقعی کلاس‌ها و دانش‌آموزان.
 *
 * <p>⚠️ تکالیف، AI و اشتراک عمداً با عدد جعلی نشان داده <b>نمی‌شوند</b>.
 * فقط داده‌ای که سرور الان دارد.
 */
export function DashboardPage() {
  const user = useAuthStore((state) => state.user);
  const tenant = useAuthStore((state) => state.tenant);

  const classesQuery = useQuery({
    queryKey: ['classes'],
    queryFn: () => listClasses(0, 5),
  });

  const studentsQuery = useQuery({
    queryKey: ['students', 'summary'],
    queryFn: () => listStudents(0, 1),
  });

  return (
    <>
      <PageHeader
        title={user ? `سلام ${user.fullName}` : 'دستیار هوشمند معلم'}
        description={tenant ? tenant.name : 'مدرسه‌ای متصل نیست'}
      />

      <div className="mb-4 grid grid-cols-2 gap-3">
        <SummaryCard
          loading={classesQuery.isPending}
          label="کلاس‌ها"
          value={classesQuery.data?.totalElements}
          to="/classes"
          icon={<GraduationCap className="size-4" aria-hidden="true" />}
        />
        <SummaryCard
          loading={studentsQuery.isPending}
          label="دانش‌آموزان"
          value={studentsQuery.data?.totalElements}
          to="/students"
          icon={<Users className="size-4" aria-hidden="true" />}
        />
      </div>

      {classesQuery.isError ? (
        <ErrorCard
          title="خلاصه کلاس‌ها نیامد"
          message={
            classesQuery.error instanceof ApiError
              ? classesQuery.error.message
              : 'ارتباط با سرور برقرار نشد.'
          }
          action={
            <Button variant="secondary" onClick={() => void classesQuery.refetch()}>
              تلاش مجدد
            </Button>
          }
        />
      ) : null}

      {classesQuery.isPending ? <LoadingSkeleton lines={3} label="بارگذاری کلاس‌ها" /> : null}

      {classesQuery.isSuccess ? (
        <section className="mb-4">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="text-sm font-medium">کلاس‌های اخیر</h2>
            <Link to="/classes" className="text-sm text-primary">
              همه
            </Link>
          </div>
          {(classesQuery.data?.content?.length ?? 0) === 0 ? (
            <p className="rounded-xl border border-dashed border-border px-4 py-6 text-center text-sm text-muted-foreground">
              هنوز کلاسی ندارید.{' '}
              <Link to="/classes" className="text-primary">
                یکی بسازید
              </Link>
            </p>
          ) : (
            <ul className="flex flex-col gap-2">
              {classesQuery.data.content.map((room) => (
                <li key={room.id}>
                  <Link
                    to={`/classes/${room.id}`}
                    className="block rounded-xl border border-border bg-card px-4 py-3 transition-colors hover:bg-muted/50"
                  >
                    <p className="font-medium">{room.name}</p>
                    <p className="text-sm text-muted-foreground">
                      {[room.gradeLevel, room.subjectName].filter(Boolean).join(' · ') ||
                        'بدون جزئیات'}
                    </p>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>
      ) : null}

      <section>
        <h2 className="mb-2 text-sm font-medium">میان‌بُرها</h2>
        <div className="grid grid-cols-2 gap-2">
          <Shortcut to="/classes" icon={<GraduationCap className="size-4" />} label="کلاس‌ها" />
          <Shortcut to="/students" icon={<Users className="size-4" />} label="دانش‌آموزان" />
          <Shortcut to="/books" icon={<BookOpen className="size-4" />} label="کتاب‌ها" />
          <Shortcut
            to="/assignments"
            icon={<ClipboardList className="size-4" />}
            label="تکالیف"
          />
        </div>
      </section>
    </>
  );
}

function SummaryCard(props: {
  label: string;
  value: number | undefined;
  loading: boolean;
  to: string;
  icon: React.ReactNode;
}) {
  return (
    <Link
      to={props.to}
      className="rounded-xl border border-border bg-card px-3 py-3 transition-colors hover:bg-muted/50"
    >
      <div className="mb-2 flex items-center gap-2 text-muted-foreground">
        {props.icon}
        <span className="text-xs">{props.label}</span>
      </div>
      {props.loading ? (
        <div className="h-7 w-10 animate-pulse rounded bg-muted" />
      ) : (
        <p className="text-2xl font-semibold">{props.value ?? '—'}</p>
      )}
    </Link>
  );
}

function Shortcut(props: { to: string; icon: React.ReactNode; label: string }) {
  return (
    <Link
      to={props.to}
      className="flex items-center gap-2 rounded-xl border border-border bg-card px-3 py-3 text-sm transition-colors hover:bg-muted/50"
    >
      <span className="text-primary">{props.icon}</span>
      {props.label}
    </Link>
  );
}
