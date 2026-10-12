import { describe, expect, it } from 'vitest';
import { useAuthStore } from './auth.store';

describe('auth.store', () => {
  it('نشست را در sessionStorage نگه می‌دارد و پاک می‌کند', () => {
    useAuthStore.getState().clearSession();

    useAuthStore.getState().setSession('a', 'r', {
      id: 'u1',
      fullName: 'علی',
      username: 'ali',
      roles: ['SUPER_ADMIN'],
    });

    expect(window.sessionStorage.getItem('st_admin_access')).toBe('a');
    expect(window.sessionStorage.getItem('st_admin_user')).toContain('علی');
    expect(useAuthStore.getState().user?.fullName).toBe('علی');

    useAuthStore.getState().clearSession();
    expect(window.sessionStorage.getItem('st_admin_access')).toBeNull();
    expect(useAuthStore.getState().accessToken).toBeNull();
  });
});
