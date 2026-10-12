import { apiFetch, apiFetchAnonymous } from './api-client';
import type { AuthResponse, CurrentUserResponse } from '../types/api';

/**
 * فراخوانی‌های احراز هویت — مسیرها دقیقاً مطابق بک‌اند.
 *
 * <p>⚠️ مسیرها از {@code EitaaAuthController} گرفته شده‌اند، نه از حافظه:
 * {@code /auth/eitaa}, {@code /auth/me}, {@code /auth/refresh},
 * {@code /auth/logout}. پیشوند {@code /api/v1} در {@code api-client} اضافه
 * می‌شود.
 */

/** ورود با `initData` ایتا. */
export function loginWithEitaa(initData: string): Promise<AuthResponse> {
  return apiFetchAnonymous<AuthResponse>('/auth/eitaa', {
    method: 'POST',
    body: { initData },
  });
}

/**
 * تازه‌سازی نشست.
 *
 * <p>⚠️ <b>بدون توکن دسترسی فرستاده می‌شود</b> ({@code apiFetchAnonymous}).
 * دلیلش مهم است: وقتی توکن دسترسی منقضی شده، فرستادنش نه فایده‌ای دارد و
 * نه باید باعث شود این درخواست هم ۴۰۱ بگیرد و وارد چرخه تازه‌سازی شود.
 * ورودی این مسیر فقط توکن تازه‌سازی است.
 */
export function refreshSession(refreshToken: string): Promise<AuthResponse> {
  return apiFetchAnonymous<AuthResponse>('/auth/refresh', {
    method: 'POST',
    body: { refreshToken },
  });
}

/** پروفایل کاربر واردشده. */
export function fetchCurrentUser(): Promise<CurrentUserResponse> {
  return apiFetch<CurrentUserResponse>('/auth/me');
}

/**
 * خروج.
 *
 * <p>⚠️ حتی اگر این درخواست شکست بخورد، کلاینت باید وضعیت محلی را پاک
 * کند. یک خروج نیمه‌کاره — که توکن سرور زنده است ولی کاربر فکر می‌کند
 * خارج شده — بدتر از یک خطای صریح است. پس فراخوان تصمیم می‌گیرد که خطا
 * را نادیده بگیرد.
 */
export function logoutSession(refreshToken: string): Promise<void> {
  return apiFetch<void>('/auth/logout', {
    method: 'POST',
    body: { refreshToken },
  });
}
