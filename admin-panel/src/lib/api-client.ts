import type { ApiErrorBody } from '../types/api';

const BASE_URL: string = import.meta.env.VITE_API_URL ?? '/api/v1';

export class ApiError extends Error {
  readonly status: number;
  readonly code: string | undefined;
  readonly traceId: string | undefined;

  constructor(params: {
    message: string;
    status: number;
    code?: string | undefined;
    traceId?: string | undefined;
  }) {
    super(params.message);
    this.name = 'ApiError';
    this.status = params.status;
    this.code = params.code;
    this.traceId = params.traceId;
  }
}

export interface AuthBridge {
  getAccessToken(): string | null;
  onSessionLost(): void;
}

interface CsrfState {
  token: string | null;
  headerName: string;
}

let authBridge: AuthBridge | null = null;
let csrfState: CsrfState = { token: null, headerName: 'X-XSRF-TOKEN' };

const MUTATING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

export function configureAuth(bridge: AuthBridge): void {
  authBridge = bridge;
}

export function resetAuthBridgeForTest(): void {
  authBridge = null;
  csrfState = { token: null, headerName: 'X-XSRF-TOKEN' };
}

export function resetCsrfForTest(): void {
  csrfState = { token: null, headerName: 'X-XSRF-TOKEN' };
}

/** پیش‌بارگذاری CSRF از GET /admin/csrf — کوکی XSRF-TOKEN هم ست می‌شود. */
export async function prefetchCsrf(): Promise<void> {
  const response = await fetch(`${BASE_URL}/admin/csrf`, {
    method: 'GET',
    credentials: 'include',
  });
  if (!response.ok) {
    return;
  }
  const body = (await response.json()) as { token?: string; headerName?: string };
  if (body.token) {
    csrfState = {
      token: body.token,
      headerName: body.headerName ?? 'X-XSRF-TOKEN',
    };
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase();
  const headers = new Headers(init.headers);
  if (!headers.has('Content-Type') && init.body != null) {
    headers.set('Content-Type', 'application/json');
  }

  const token = authBridge?.getAccessToken() ?? null;
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const isMutating = MUTATING_METHODS.has(method);
  const csrfExempt = path === '/auth/login' || path === '/auth/refresh-admin';

  if (isMutating && !csrfExempt && csrfState.token == null) {
    await prefetchCsrf().catch(() => undefined);
  }

  // Bearer تست‌ها CSRF را دور می‌زند؛ بدون Bearer (کوکی) هدر CSRF لازم است
  if (isMutating && !csrfExempt && csrfState.token && !token) {
    headers.set(csrfState.headerName, csrfState.token);
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers,
    credentials: 'include',
  });

  if (response.status === 401) {
    authBridge?.onSessionLost();
  }

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as ApiErrorBody | null;
    throw new ApiError({
      message: body?.message ?? 'خطای غیرمنتظره رخ داد.',
      status: response.status,
      code: body?.code,
      traceId: body?.traceId,
    });
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) => {
    const init: RequestInit = { method: 'POST' };
    if (body !== undefined) {
      init.body = JSON.stringify(body);
    }
    return request<T>(path, init);
  },
  put: <T>(path: string, body?: unknown) => {
    const init: RequestInit = { method: 'PUT' };
    if (body !== undefined) {
      init.body = JSON.stringify(body);
    }
    return request<T>(path, init);
  },
  patch: <T>(path: string, body?: unknown) => {
    const init: RequestInit = { method: 'PATCH' };
    if (body !== undefined) {
      init.body = JSON.stringify(body);
    }
    return request<T>(path, init);
  },
  delete: <T = void>(path: string) => request<T>(path, { method: 'DELETE' }),
};
