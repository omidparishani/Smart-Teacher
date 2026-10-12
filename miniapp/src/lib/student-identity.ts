/** کل sessionStorage برای شناسه دانش‌آموز (وقتی از auth/session در دسترس باشد). */
const SESSION_STUDENT_ID_KEY = 'smartteacher:studentId';

/**
 * شناسه دانش‌آموز از نشست مرورگر — اگر قبلاً ذخیره شده باشد.
 *
 * <p>⚠️ در فاز فعلی auth، user.id همان student.id نیست؛ این کلید برای
 * پیش‌نمایش معلم یا پیوند آینده دانش‌آموز استفاده می‌شود.
 */
export function getSessionStudentId(): string | null {
  try {
    const value = sessionStorage.getItem(SESSION_STUDENT_ID_KEY);
    if (value && /^[0-9a-fA-F-]{36}$/.test(value)) {
      return value;
    }
  } catch {
    // sessionStorage ممکن است در WebView محدود باشد
  }
  return null;
}

/** ذخیره شناسه دانش‌آموز در نشست (مثلاً پس از انتخاب در پیش‌نمایش). */
export function setSessionStudentId(studentId: string): void {
  try {
    sessionStorage.setItem(SESSION_STUDENT_ID_KEY, studentId);
  } catch {
    // نادیده
  }
}
