import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { ApiError, api, configureAuth, resetAuthBridgeForTest } from './api-client';

describe('api-client', () => {
  beforeEach(() => {
    resetAuthBridgeForTest();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    resetAuthBridgeForTest();
  });

  it('Authorization را از پل auth می‌چسباند', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    configureAuth({
      getAccessToken: () => 'test-token',
      onSessionLost: () => undefined,
    });

    await api.get<{ ok: boolean }>('/admin/dashboard');

    expect(fetchMock).toHaveBeenCalledOnce();
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    const headers = new Headers(init.headers);
    expect(headers.get('Authorization')).toBe('Bearer test-token');
    expect(init.credentials).toBe('include');
  });

  it('بدون Bearer، هدر CSRF روی POST می‌رود', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ token: 'csrf-1', headerName: 'X-XSRF-TOKEN' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ id: '1' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      );
    vi.stubGlobal('fetch', fetchMock);

    await api.post('/admin/schools', { name: 'test' });

    expect(fetchMock).toHaveBeenCalledTimes(2);
    const [, init] = fetchMock.mock.calls[1] as [string, RequestInit];
    const headers = new Headers(init.headers);
    expect(headers.get('X-XSRF-TOKEN')).toBe('csrf-1');
    expect(init.credentials).toBe('include');
  });

  it('خطای API را به ApiError تبدیل می‌کند', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            code: 'ACCESS_DENIED',
            message: 'شما به این بخش دسترسی ندارید.',
          }),
          { status: 403, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    );

    await expect(api.get('/admin/users')).rejects.toMatchObject({
      name: 'ApiError',
      status: 403,
      code: 'ACCESS_DENIED',
    } satisfies Partial<ApiError>);
  });

  it('متدهای delete و patch را با هدرهای درست صدا می‌زند', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(null, { status: 204 }),
    );
    vi.stubGlobal('fetch', fetchMock);

    await api.delete('/admin/schools/123');
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/schools/123', expect.objectContaining({
      method: 'DELETE',
    }));

    await api.patch('/admin/schools/123', { name: 'updated' });
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/admin/schools/123', expect.objectContaining({
      method: 'PATCH',
      body: JSON.stringify({ name: 'updated' }),
    }));
  });
});
