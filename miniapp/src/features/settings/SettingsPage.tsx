import { Link } from 'react-router-dom';
import { ClipboardCheck, Clock, GraduationCap, MessageSquare, Radio, Users, Zap } from 'lucide-react';
import { useAuthStore } from '../../stores/auth.store';
import { PageHeader } from '../../components/shared/PageHeader';
import { PhaseNote } from '../../components/shared/PhaseNote';

/**
 * زبانه تنظیمات — لینک‌های واقعی دامنه معلم + یادداشت فازهای بعد.
 */
export function SettingsPage() {
  const user = useAuthStore((state) => state.user);
  const tenant = useAuthStore((state) => state.tenant);

  return (
    <>
      <PageHeader title="تنظیمات" description="حساب و مدیریت مدرسه" />

      <div className="mb-4 rounded-xl border border-border bg-card p-4 text-sm">
        <p className="font-medium">{user?.fullName ?? 'کاربر'}</p>
        <p className="mt-1 text-muted-foreground">{tenant?.name ?? 'بدون مستأجر'}</p>
      </div>

      <nav className="mb-4 flex flex-col gap-2">
        <SettingsLink to="/classes" icon={<GraduationCap className="size-4" />} label="کلاس‌های من" />
        <SettingsLink to="/students" icon={<Users className="size-4" />} label="دانش‌آموزان مدرسه" />
        <SettingsLink to="/exams" icon={<ClipboardCheck className="size-4" />} label="آزمون‌ها" />
        <SettingsLink to="/groups" icon={<MessageSquare className="size-4" />} label="گروه‌های ایتا" />
        <SettingsLink to="/channels" icon={<Radio className="size-4" />} label="کانال‌های ایتا" />
        <SettingsLink to="/automation-rules" icon={<Zap className="size-4" />} label="قواعد خودکارسازی" />
        <SettingsLink to="/schedule" icon={<Clock className="size-4" />} label="زمان‌بندی پیام" />
      </nav>

      <PhaseNote
        phase={10}
        description="اشتراک، فاکتورها و ارتقای پلن اینجا اضافه می‌شود."
      />
    </>
  );
}

function SettingsLink(props: { to: string; icon: React.ReactNode; label: string }) {
  return (
    <Link
      to={props.to}
      className="flex items-center gap-3 rounded-xl border border-border bg-card px-4 py-3 transition-colors hover:bg-muted/50"
    >
      <span className="text-primary">{props.icon}</span>
      <span className="text-sm font-medium">{props.label}</span>
    </Link>
  );
}
