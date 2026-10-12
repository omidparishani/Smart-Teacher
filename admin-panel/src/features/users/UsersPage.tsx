import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ApiError } from '../../lib/api-client';
import type { AdminUserResponse } from '../../types/api';
import { disableUser, enableUser, fetchUserRoles, fetchUsers, suspendUser, updateUserRoles } from '../admin/api';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Input } from '../../components/ui/Input';
import { Badge } from '../../components/ui/Badge';
import { Modal } from '../../components/ui/Modal';

const STATUS_CONFIG: Record<
  AdminUserResponse['status'],
  { label: string; variant: 'success' | 'warning' | 'danger' | 'neutral' }
> = {
  ACTIVE: { label: 'فعال', variant: 'success' },
  DISABLED: { label: 'غیرفعال', variant: 'neutral' },
  SUSPENDED: { label: 'معلق', variant: 'danger' },
  PENDING: { label: 'در انتظار', variant: 'warning' },
};

const AVAILABLE_ROLES = [
  { id: 'ROLE_ADMIN', label: 'مدیر ارشد (ADMIN)' },
  { id: 'ROLE_TEACHER', label: 'معلم (TEACHER)' },
  { id: 'ROLE_PRINCIPAL', label: 'مدیر مدرسه (PRINCIPAL)' },
  { id: 'ROLE_STAFF', label: 'کادر اجرایی (STAFF)' },
  { id: 'ROLE_USER', label: 'کاربر عادی (USER)' },
];

export function UsersPage() {
  const queryClient = useQueryClient();
  const [suspendTarget, setSuspendTarget] = useState<AdminUserResponse | null>(null);
  const [suspendReason, setSuspendReason] = useState('');
  const [roleEditTarget, setRoleEditTarget] = useState<AdminUserResponse | null>(null);
  const [selectedRoles, setSelectedRoles] = useState<string[]>([]);
  const [loadingRoles, setLoadingRoles] = useState(false);

  const query = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: () => fetchUsers(0, 50),
  });

  const invalidate = () => void queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });

  const disableMutation = useMutation({
    mutationFn: disableUser,
    onSuccess: invalidate,
    onError: (err) => alert(err instanceof ApiError ? err.message : 'غیرفعال‌سازی ناموفق بود.'),
  });

  const enableMutation = useMutation({
    mutationFn: enableUser,
    onSuccess: invalidate,
    onError: (err) => alert(err instanceof ApiError ? err.message : 'فعال‌سازی ناموفق بود.'),
  });

  const suspendMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => suspendUser(id, reason),
    onSuccess: () => {
      setSuspendTarget(null);
      setSuspendReason('');
      invalidate();
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'تعلیق کاربر ناموفق بود.'),
  });

  const updateRolesMutation = useMutation({
    mutationFn: () => updateUserRoles(roleEditTarget!.id, selectedRoles),
    onSuccess: () => {
      setRoleEditTarget(null);
      invalidate();
    },
    onError: (err) => alert(err instanceof ApiError ? err.message : 'ویرایش نقش‌ها ناموفق بود.'),
  });

  async function openRoleEditor(user: AdminUserResponse) {
    setRoleEditTarget(user);
    setLoadingRoles(true);
    try {
      const roles = await fetchUserRoles(user.id);
      setSelectedRoles(roles);
    } catch {
      setSelectedRoles([]);
    } finally {
      setLoadingRoles(false);
    }
  }

  function toggleRole(roleId: string) {
    setSelectedRoles((prev) =>
      prev.includes(roleId) ? prev.filter((r) => r !== roleId) : [...prev, roleId],
    );
  }

  if (query.isPending) {
    return (
      <div className="flex items-center justify-center min-h-[40vh]">
        <div className="w-8 h-8 border-3 border-blue-600 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (query.isError) {
    return (
      <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-sm">
        {query.error instanceof ApiError ? query.error.message : 'بارگذاری کاربران ناموفق بود.'}
      </div>
    );
  }

  const users = query.data.content;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900">مدیریت کاربران سامانه</h1>
          <p className="text-xs text-slate-500 mt-0.5">
            مجموع {query.data.totalElements.toLocaleString('fa-IR')} کاربر ثبت‌شده — کنترل وضعیت و انتساب نقش‌های امنیتی
          </p>
        </div>
      </div>

      <Card className="p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-right text-xs">
            <thead className="bg-slate-50 border-b border-slate-200/80 text-slate-600 font-bold">
              <tr>
                <th className="py-3 px-4">نام کاربر</th>
                <th className="py-3 px-4">شماره موبایل / نام کاربری</th>
                <th className="py-3 px-4">ایمیل</th>
                <th className="py-3 px-4">وضعیت</th>
                <th className="py-3 px-4 text-center">عملیات</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {users.map((user) => {
                const status = STATUS_CONFIG[user.status] || {
                  label: user.status,
                  variant: 'neutral' as const,
                };
                return (
                  <tr key={user.id} className="hover:bg-slate-50/50 transition-colors">
                    <td className="py-3.5 px-4 font-semibold text-slate-800">{user.fullName}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-600">{user.mobile || user.username || '—'}</td>
                    <td className="py-3.5 px-4 font-mono text-slate-500">{user.email || '—'}</td>
                    <td className="py-3.5 px-4">
                      <Badge variant={status.variant}>{status.label}</Badge>
                    </td>
                    <td className="py-3.5 px-4 text-center">
                      <div className="flex items-center justify-center gap-1.5">
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-blue-600 hover:bg-blue-50"
                          onClick={() => void openRoleEditor(user)}
                        >
                          نقش‌ها
                        </Button>
                        {user.status !== 'ACTIVE' ? (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-emerald-600 hover:bg-emerald-50"
                            onClick={() => enableMutation.mutate(user.id)}
                          >
                            فعال
                          </Button>
                        ) : (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-slate-600 hover:bg-slate-100"
                            onClick={() => disableMutation.mutate(user.id)}
                          >
                            غیرفعال
                          </Button>
                        )}
                        {user.status !== 'SUSPENDED' && (
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-rose-600 hover:bg-rose-50"
                            onClick={() => setSuspendTarget(user)}
                          >
                            تعلیق
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
              {users.length === 0 && (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-slate-400">
                    هیچ کاربری یافت نشد.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* Suspend Modal */}
      <Modal
        isOpen={!!suspendTarget}
        onClose={() => setSuspendTarget(null)}
        title="تعلیق کاربر"
        maxWidth="sm"
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-600">
            آیا از تعلیق حساب کاربری <strong className="text-slate-900 font-bold">{suspendTarget?.fullName}</strong> اطمینان
            دارید؟ ورود این کاربر فوراً مسدود می‌شود.
          </p>
          <Input
            label="دلیل تعلیق (جهت درج در لاگ امنیتی)"
            value={suspendReason}
            onChange={(e) => setSuspendReason(e.target.value)}
            placeholder="مثال: نقض قوانین یا گزارش تخلف"
            required
          />
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" onClick={() => setSuspendTarget(null)}>
              انصراف
            </Button>
            <Button
              variant="danger"
              isLoading={suspendMutation.isPending}
              disabled={!suspendReason.trim()}
              onClick={() =>
                suspendTarget &&
                suspendMutation.mutate({ id: suspendTarget.id, reason: suspendReason.trim() })
              }
            >
              تعلیق قطعی
            </Button>
          </div>
        </div>
      </Modal>

      {/* Role Editor Modal */}
      <Modal
        isOpen={!!roleEditTarget}
        onClose={() => setRoleEditTarget(null)}
        title={`مدیریت نقش‌های امنیتی: ${roleEditTarget?.fullName ?? ''}`}
        maxWidth="md"
      >
        <div className="space-y-4">
          <p className="text-xs text-slate-500">
            نقش‌های دسترسی مجاز برای این کاربر را علامت بزنید. تغییرات به صورت بلادرنگ اعمال می‌شوند.
          </p>
          {loadingRoles ? (
            <div className="py-6 flex justify-center">
              <div className="w-6 h-6 border-2 border-blue-600 border-t-transparent rounded-full animate-spin" />
            </div>
          ) : (
            <div className="space-y-2">
              {AVAILABLE_ROLES.map((role) => {
                const checked = selectedRoles.includes(role.id);
                return (
                  <label
                    key={role.id}
                    className={`flex items-center justify-between p-3 rounded-lg border cursor-pointer transition-all ${
                      checked
                        ? 'border-blue-500 bg-blue-50/50 text-blue-900'
                        : 'border-slate-200 hover:border-slate-300 text-slate-700'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => toggleRole(role.id)}
                        className="rounded border-slate-300 text-blue-600 focus:ring-blue-500 w-4 h-4 cursor-pointer"
                      />
                      <span className="text-xs font-semibold">{role.label}</span>
                    </div>
                    <span className="text-[10px] font-mono text-slate-400">{role.id}</span>
                  </label>
                );
              })}
            </div>
          )}
          <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
            <Button variant="secondary" onClick={() => setRoleEditTarget(null)}>
              انصراف
            </Button>
            <Button
              variant="primary"
              isLoading={updateRolesMutation.isPending}
              onClick={() => updateRolesMutation.mutate()}
            >
              ذخیره نقش‌ها
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
