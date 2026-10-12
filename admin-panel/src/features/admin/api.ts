import { api } from '../../lib/api-client';
import type {
  AdminAuthResponse,
  AdminChartsResponse,
  AdminDashboardResponse,
  AdminSchoolResponse,
  AdminSubscriptionResponse,
  AdminTenantResponse,
  AdminUserResponse,
  AiUsageReportResponse,
  CouponResponse,
  CreateAdminSchoolRequest,
  FeatureResponse,
  PlanResponse,
  PageResponse,
  RevenueReportResponse,
  SubscriptionsReportResponse,
  UsersReportResponse,
} from '../../types/api';

export function login(username: string, password: string): Promise<AdminAuthResponse> {
  return api.post<AdminAuthResponse>('/auth/login', { username, password });
}

export function logoutAdmin(refreshToken?: string | null): Promise<void> {
  const body = refreshToken ? { refreshToken } : undefined;
  return api.post<void>('/auth/logout-admin', body);
}

export function fetchSchools(page = 0, size = 20): Promise<PageResponse<AdminSchoolResponse>> {
  return api.get<PageResponse<AdminSchoolResponse>>(
    `/admin/schools?page=${page}&size=${size}&sort=createdAt,desc`,
  );
}

export function createSchool(body: CreateAdminSchoolRequest): Promise<AdminSchoolResponse> {
  return api.post<AdminSchoolResponse>('/admin/schools', body);
}

export function fetchDashboard(): Promise<AdminDashboardResponse> {
  return api.get<AdminDashboardResponse>('/admin/dashboard');
}

export function fetchCharts(period = '30d'): Promise<AdminChartsResponse> {
  return api.get<AdminChartsResponse>(`/admin/dashboard/charts?period=${encodeURIComponent(period)}`);
}

export function fetchUsersReport(): Promise<UsersReportResponse> {
  return api.get<UsersReportResponse>('/admin/reports/users');
}

export function fetchSubscriptionsReport(): Promise<SubscriptionsReportResponse> {
  return api.get<SubscriptionsReportResponse>('/admin/reports/subscriptions');
}

export function fetchAiReport(): Promise<AiUsageReportResponse> {
  return api.get<AiUsageReportResponse>('/admin/reports/ai');
}

export function fetchRevenueReport(): Promise<RevenueReportResponse> {
  return api.get<RevenueReportResponse>('/admin/reports/revenue');
}

export function fetchUsers(page = 0, size = 20): Promise<PageResponse<AdminUserResponse>> {
  return api.get<PageResponse<AdminUserResponse>>(
    `/admin/users?page=${page}&size=${size}&sort=createdAt,desc`,
  );
}

export function fetchTenants(page = 0, size = 20): Promise<PageResponse<AdminTenantResponse>> {
  return api.get<PageResponse<AdminTenantResponse>>(
    `/admin/tenants?page=${page}&size=${size}&sort=createdAt,desc`,
  );
}

export function fetchFeatures(): Promise<FeatureResponse[]> {
  return api.get<FeatureResponse[]>('/admin/features');
}

export function updateFeature(
  code: string,
  body: { defaultEnabled?: boolean; name?: string; description?: string },
): Promise<FeatureResponse> {
  return api.put<FeatureResponse>(`/admin/features/${encodeURIComponent(code)}`, body);
}

export function disableUser(id: string): Promise<AdminUserResponse> {
  return api.post<AdminUserResponse>(`/admin/users/${id}/disable`);
}

export function enableUser(id: string): Promise<AdminUserResponse> {
  return api.post<AdminUserResponse>(`/admin/users/${id}/enable`);
}

export function suspendUser(id: string, reason: string): Promise<AdminUserResponse> {
  return api.post<AdminUserResponse>(`/admin/users/${id}/suspend`, { reason });
}

export function fetchPlans(): Promise<PlanResponse[]> {
  return api.get<PlanResponse[]>('/admin/plans');
}

export function createPlan(body: {
  code: string;
  name: string;
  price: number;
  currency?: string;
  billingPeriod?: string;
  isPublic?: boolean;
}): Promise<PlanResponse> {
  return api.post<PlanResponse>('/admin/plans', body);
}

export function updatePlan(
  id: string,
  body: { name?: string; price?: number },
): Promise<PlanResponse> {
  return api.put<PlanResponse>(`/admin/plans/${id}`, body);
}

export function fetchCoupons(): Promise<CouponResponse[]> {
  return api.get<CouponResponse[]>('/admin/coupons');
}

export function createCoupon(body: {
  code: string;
  discountType: string;
  discountValue: number;
  validFrom: string;
  validUntil: string;
  usageLimit?: number | undefined;
}): Promise<CouponResponse> {
  return api.post<CouponResponse>('/admin/coupons', body);
}

export function disableCoupon(id: string): Promise<CouponResponse> {
  return api.post<CouponResponse>(`/admin/coupons/${id}/disable`);
}

export function fetchSubscriptions(
  page = 0,
  size = 50,
): Promise<AdminSubscriptionResponse[]> {
  return api.get<AdminSubscriptionResponse[]>(
    `/admin/subscriptions?page=${page}&size=${size}`,
  );
}

// Schools CRUD
export function deleteSchool(id: string): Promise<void> {
  return api.delete<void>(`/admin/schools/${id}`);
}

// Tenants CRUD
export function updateTenant(
  id: string,
  body: { name?: string; status?: string; settings?: string },
): Promise<AdminTenantResponse> {
  return api.put<AdminTenantResponse>(`/admin/tenants/${id}`, body);
}

export function deleteTenant(id: string): Promise<void> {
  return api.delete<void>(`/admin/tenants/${id}`);
}

// Plans CRUD
export function disablePlan(id: string): Promise<PlanResponse> {
  return api.post<PlanResponse>(`/admin/plans/${id}/disable`);
}

export function deletePlan(id: string): Promise<void> {
  return api.delete<void>(`/admin/plans/${id}`);
}

// Coupons CRUD
export function enableCoupon(id: string): Promise<CouponResponse> {
  return api.post<CouponResponse>(`/admin/coupons/${id}/enable`);
}

export function updateCoupon(
  id: string,
  body: {
    discountValue?: number;
    validFrom?: string;
    validUntil?: string;
    usageLimit?: number;
    usageLimitPerTenant?: number;
  },
): Promise<CouponResponse> {
  return api.put<CouponResponse>(`/admin/coupons/${id}`, body);
}

export function deleteCoupon(id: string): Promise<void> {
  return api.delete<void>(`/admin/coupons/${id}`);
}

// Subscriptions management
export function cancelSubscription(id: string, reason?: string): Promise<AdminSubscriptionResponse> {
  return api.post<AdminSubscriptionResponse>(`/admin/subscriptions/${id}/cancel`, { reason });
}

export function extendSubscription(id: string, days: number): Promise<AdminSubscriptionResponse> {
  return api.post<AdminSubscriptionResponse>(`/admin/subscriptions/${id}/extend`, { days });
}

// User Roles
export function fetchUserRoles(id: string): Promise<string[]> {
  return api.get<string[]>(`/admin/users/${id}/roles`);
}

export function updateUserRoles(id: string, roles: string[]): Promise<AdminUserResponse> {
  return api.put<AdminUserResponse>(`/admin/users/${id}/roles`, { roles });
}

// Audit Logs
export function fetchAuditLogs(
  page = 0,
  size = 20,
  tenantId?: string,
): Promise<PageResponse<import('../../types/api').AdminAuditLogResponse>> {
  const url = tenantId
    ? `/admin/audit-log?page=${page}&size=${size}&tenantId=${tenantId}`
    : `/admin/audit-log?page=${page}&size=${size}`;
  return api.get<PageResponse<import('../../types/api').AdminAuditLogResponse>>(url);
}

// System Settings
export function fetchSettings(): Promise<import('../../types/api').SystemSettingResponse[]> {
  return api.get<import('../../types/api').SystemSettingResponse[]>('/admin/settings');
}

export function updateSetting(
  key: string,
  value: string,
  description?: string,
): Promise<import('../../types/api').SystemSettingResponse> {
  return api.put<import('../../types/api').SystemSettingResponse>(`/admin/settings/${encodeURIComponent(key)}`, {
    value,
    description,
  });
}

// Eitaa Health
export function fetchEitaaHealth(): Promise<import('../../types/api').EitaaHealthResponse> {
  return api.get<import('../../types/api').EitaaHealthResponse>('/eitaa/health');
}

// Features
export function createFeature(body: {
  code: string;
  name: string;
  category?: string | undefined;
  description?: string | undefined;
  defaultEnabled?: boolean | undefined;
  sortOrder?: number | undefined;
}): Promise<FeatureResponse> {
  return api.post<FeatureResponse>('/admin/features', body);
}

export function deleteFeatureOverride(id: string): Promise<void> {
  return api.delete<void>(`/admin/features/overrides/${id}`);
}

// Prompts
export function deletePrompt(id: string): Promise<void> {
  return api.delete<void>(`/admin/prompts/${id}`);
}

