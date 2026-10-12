export type UserStatus = 'ACTIVE' | 'DISABLED' | 'SUSPENDED' | 'PENDING';

export interface AdminUserView {
  id: string;
  fullName: string;
  username: string | null;
  roles: string[];
}

export interface AdminAuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  refreshExpiresIn: number;
  user: AdminUserView;
}

export interface AdminDashboardResponse {
  users: { total: number; active: number };
  tenants: { total: number };
  education: { classrooms: number; students: number };
}

export interface DailyCount {
  date: string;
  count: number;
}

export interface DailyRevenue {
  date: string;
  invoiceCount: number;
  total: number | string;
}

export interface AdminChartsResponse {
  period: string;
  newUsers: DailyCount[];
  newTenants: DailyCount[];
  subscriptions: DailyCount[];
  revenue?: DailyRevenue[];
}

export interface UsersReportResponse {
  total: number;
  byStatus: Record<string, number>;
}

export interface SubscriptionsReportResponse {
  total: number;
  byStatus: Record<string, number>;
  churnRateApprox?: number;
  conversionRateApprox?: number;
  paidActiveCount?: number;
  freeActiveCount?: number;
}

export interface AiUsageReportResponse {
  requestCount: number;
  promptTokens: number;
  completionTokens: number;
  totalCost: number | string;
  byModel?: { model: string; requestCount: number; totalCost: number | string }[];
  byProvider?: { provider: string; requestCount: number; totalCost: number | string }[];
  byUser?: {
    userId: string;
    userName: string;
    requestCount: number;
    promptTokens: number;
    completionTokens: number;
    totalCost: number | string;
  }[];
}

export interface PlanResponse {
  id: string;
  code: string;
  name: string;
  description: string | null;
  price: number | string;
  currency: string;
  billingPeriod: string;
  trialDays: number | null;
  isPublic: boolean;
  isDefault: boolean;
  sortOrder: number;
  active: boolean;
}

export interface CouponResponse {
  id: string;
  code: string;
  discountType: string;
  discountValue: number | string;
  maxDiscountAmount: number | string | null;
  minPurchaseAmount: number | string;
  usageLimit: number | null;
  usageLimitPerTenant: number | null;
  usedCount: number;
  validFrom: string;
  validUntil: string;
  applicablePlanId: string | null;
  active: boolean;
}

export interface AdminSubscriptionResponse {
  id: string;
  tenantId: string;
  planId: string;
  planCode: string;
  planName: string;
  status: string;
  startDate: string | null;
  endDate: string | null;
  pricePaid: number | string | null;
  createdAt: string;
}

export interface RevenueReportResponse {
  paidInvoiceCount: number;
  paidTotal: number | string;
  currency: string | null;
  byStatus: Record<string, number>;
}

export interface AdminUserResponse {
  id: string;
  username: string | null;
  fullName: string;
  email: string | null;
  mobile: string | null;
  status: UserStatus;
  platformRoles?: string[];
  lastLoginAt: string | null;
  createdAt: string;
}

export type TenantType = 'TEACHER' | 'SCHOOL' | 'ORGANIZATION';
export type TenantStatus = 'ACTIVE' | 'SUSPENDED' | 'ARCHIVED';

export interface AdminSchoolResponse {
  id: string;
  name: string;
  slug: string;
  parentTenantId: string | null;
  ownerUserId: string;
  status: TenantStatus;
  settings: string;
  createdAt: string;
}

export interface CreateAdminSchoolRequest {
  name: string;
  slug: string;
  ownerUserId: string;
  parentTenantId?: string | null;
  settings?: string;
}

export interface CsrfTokenResponse {
  token: string;
  headerName: string;
  parameterName: string;
}

export interface AdminTenantResponse {
  id: string;
  type: TenantType;
  name: string;
  slug: string;
  parentTenantId: string | null;
  ownerUserId: string;
  status: TenantStatus;
  settings: string;
  createdAt: string;
}

export interface FeatureResponse {
  id: string;
  code: string;
  name: string;
  category: string | null;
  description: string | null;
  defaultEnabled: boolean;
  sortOrder: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ApiErrorBody {
  status?: number;
  code?: string;
  message?: string;
  traceId?: string;
  details?: unknown;
}

export interface PromptResponse {
  id: string;
  code: string;
  name: string;
  featureCode: string;
  description: string | null;
  system: boolean;
}

export interface PromptVersionResponse {
  id: string;
  promptId: string;
  versionNumber: number;
  systemPrompt: string;
  userPrompt: string;
  variables: string;
  responseSchema: string | null;
  temperature: number | null;
  maxTokens: number | null;
  status: string;
  publishedAt: string | null;
  changeNote: string | null;
}

export interface AiProviderResponse {
  id: string;
  code: string;
  name: string;
  type: string;
  baseUrl: string | null;
  tokenHint: string | null;
  enabled: boolean;
}

export interface ProviderResolveTraceStepResponse {
  level: string;
  found: boolean;
  providerId: string | null;
  providerCode: string | null;
  providerName: string | null;
  detail: string | null;
}

export interface ProviderResolveTraceResponse {
  featureCode: string;
  steps: ProviderResolveTraceStepResponse[];
  selectedProviderCode: string | null;
}

export interface AiModelResponse {
  id: string;
  providerId: string;
  code: string;
  displayName: string;
  enabled: boolean;
  inputCostPerMillion: number | string | null;
  outputCostPerMillion: number | string | null;
  currency: string | null;
}

export interface SystemSettingResponse {
  id: string;
  key: string;
  value: string;
  description: string | null;
  updatedAt: string;
}

export interface AdminAuditLogResponse {
  id: string;
  tenantId: string | null;
  actorId: string | null;
  eventType: string;
  entityType: string | null;
  entityId: string | null;
  metadata: string | null;
  clientIp: string | null;
  userAgent: string | null;
  createdAt: string;
}

export interface EitaaHealthResponse {
  status: string;
  botConfigured: boolean;
  appConfigured: boolean;
  mockMode: boolean;
  verifiedGroupsCount: number;
  verifiedChannelsCount: number;
}

export interface EitaaGroupResponse {
  id: string;
  tenantId: string;
  chatId: string;
  title: string | null;
  type: string;
  verified: boolean;
  createdAt: string;
}

export interface EitaaChannelResponse {
  id: string;
  tenantId: string;
  channelId: string;
  title: string | null;
  verified: boolean;
  createdAt: string;
}

