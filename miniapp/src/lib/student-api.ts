import { apiFetch } from './api-client';
import type { CreateStudentBody, PageResponse, Student } from '../types/api';

/**
 * تماس‌های HTTP مربوط به دانش‌آموزان مدرسه.
 *
 * <p>⚠️ دانش‌آموز دارایی <i>مدرسه</i> است؛ فهرست اینجا همه دانش‌آموزان
 * مستأجر را می‌آورد، نه فقط یک کلاس. عضویت کلاس از
 * {@code classroom-api} می‌آید.
 */

export function listStudents(page = 0, size = 50, search?: string): Promise<PageResponse<Student>> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  if (search && search.trim().length > 0) {
    params.set('search', search.trim());
  }

  return apiFetch<PageResponse<Student>>(`/students?${params.toString()}`);
}

export function getStudent(id: string): Promise<Student> {
  return apiFetch<Student>(`/students/${id}`);
}

export function createStudent(body: CreateStudentBody): Promise<Student> {
  return apiFetch<Student>('/students', { method: 'POST', body });
}

export function deleteStudent(id: string): Promise<void> {
  return apiFetch<void>(`/students/${id}`, { method: 'DELETE' });
}
