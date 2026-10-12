import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { AdminLayout } from '../components/layout/AdminLayout';
import { DashboardPage } from '../features/dashboard/DashboardPage';
import { LoginPage } from '../features/dashboard/LoginPage';
import { ProvidersPage } from '../features/ai/ProvidersPage';
import { CouponsPage } from '../features/billing/CouponsPage';
import { PlansPage } from '../features/billing/PlansPage';
import { SubscriptionsPage } from '../features/billing/SubscriptionsPage';
import { FeaturesPage } from '../features/features/FeaturesPage';
import { PromptsPage } from '../features/prompts/PromptsPage';
import { ReportsPage } from '../features/reports/ReportsPage';
import { SchoolsPage } from '../features/schools/SchoolsPage';
import { TenantsPage } from '../features/tenants/TenantsPage';
import { UsersPage } from '../features/users/UsersPage';
import { AuditLogsPage } from '../features/audit/AuditLogsPage';
import { SettingsPage } from '../features/settings/SettingsPage';
import { EitaaPage } from '../features/eitaa/EitaaPage';
import { useAuthStore } from '../stores/auth.store';

function RequireAuth() {
  const user = useAuthStore((s) => s.user);
  const accessToken = useAuthStore((s) => s.accessToken);
  if (!user && !accessToken) {
    return <Navigate to="/login" replace />;
  }
  return <Outlet />;
}

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AdminLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="users" element={<UsersPage />} />
          <Route path="tenants" element={<TenantsPage />} />
          <Route path="schools" element={<SchoolsPage />} />
          <Route path="features" element={<FeaturesPage />} />
          <Route path="prompts" element={<PromptsPage />} />
          <Route path="ai/providers" element={<ProvidersPage />} />
          <Route path="plans" element={<PlansPage />} />
          <Route path="coupons" element={<CouponsPage />} />
          <Route path="subscriptions" element={<SubscriptionsPage />} />
          <Route path="audit-logs" element={<AuditLogsPage />} />
          <Route path="settings" element={<SettingsPage />} />
          <Route path="eitaa" element={<EitaaPage />} />
          <Route path="reports" element={<ReportsPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
