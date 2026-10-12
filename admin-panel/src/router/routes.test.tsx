import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AppRoutes } from '../router/routes';
import { useAuthStore } from '../stores/auth.store';

function renderApp(path: string) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[path]}>
        <AppRoutes />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('مسیریابی پنل ادمین', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession();
    vi.unstubAllGlobals();
  });

  it('بدون توکن به صفحه ورود هدایت می‌شود', () => {
    renderApp('/');
    expect(screen.getByRole('heading', { name: /ورود به پنل مدیریت/ })).toBeInTheDocument();
  });

  it('با توکن، داشبورد را بارگذاری می‌کند', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input);
        if (url.includes('/admin/reports/revenue') || url.includes('/reports/revenue')) {
          return new Response(
            JSON.stringify({ paidTotal: 0, paidInvoiceCount: 0 }),
            { status: 200, headers: { 'Content-Type': 'application/json' } },
          );
        }
        if (url.includes('/charts') || url.includes('/dashboard/charts')) {
          return new Response(
            JSON.stringify({ revenue: [], newUsers: [] }),
            { status: 200, headers: { 'Content-Type': 'application/json' } },
          );
        }
        return new Response(
          JSON.stringify({
            users: { total: 3, active: 2 },
            tenants: { total: 1 },
            education: { classrooms: 4, students: 10 },
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        );
      }),
    );

    useAuthStore.getState().setSession('access', 'refresh', {
      id: '1',
      fullName: 'مدیر ارشد',
      username: 'admin',
      roles: ['SUPER_ADMIN'],
    });

    renderApp('/');

    expect(await screen.findByRole('heading', { name: 'داشبورد' })).toBeInTheDocument();
    expect(await screen.findByText('کل کاربران')).toBeInTheDocument();
  });
});
