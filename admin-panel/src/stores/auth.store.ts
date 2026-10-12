import { create } from 'zustand';
import type { AdminUserView } from '../types/api';

const ACCESS_KEY = 'st_admin_access';
const REFRESH_KEY = 'st_admin_refresh';
const USER_KEY = 'st_admin_user';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: AdminUserView | null;
  setSession: (access: string, refresh: string, user: AdminUserView) => void;
  clearSession: () => void;
  hydrateFromStorage: () => void;
  isAuthenticated: () => boolean;
}

function read(key: string): string | null {
  try {
    return window.sessionStorage.getItem(key);
  } catch {
    return null;
  }
}

function write(key: string, value: string | null): void {
  try {
    if (value == null) {
      window.sessionStorage.removeItem(key);
    } else {
      window.sessionStorage.setItem(key, value);
    }
  } catch {
    // حافظه پر یا خصوصی — نشست فقط در حافظه می‌ماند
  }
}

export const useAuthStore = create<AuthState>((set, get) => ({
  accessToken: null,
  refreshToken: null,
  user: null,

  setSession: (access, refresh, user) => {
    write(ACCESS_KEY, access);
    write(REFRESH_KEY, refresh);
    write(USER_KEY, JSON.stringify(user));
    set({ accessToken: access, refreshToken: refresh, user });
  },

  clearSession: () => {
    write(ACCESS_KEY, null);
    write(REFRESH_KEY, null);
    write(USER_KEY, null);
    set({ accessToken: null, refreshToken: null, user: null });
  },

  hydrateFromStorage: () => {
    const access = read(ACCESS_KEY);
    const refresh = read(REFRESH_KEY);
    const userRaw = read(USER_KEY);
    let user: AdminUserView | null = null;
    if (userRaw) {
      try {
        user = JSON.parse(userRaw) as AdminUserView;
      } catch {
        user = null;
      }
    }
    if (access || refresh || user) {
      set({ accessToken: access, refreshToken: refresh, user });
    }
  },

  isAuthenticated: () => {
    const state = get();
    return Boolean(state.user ?? state.accessToken);
  },
}));
