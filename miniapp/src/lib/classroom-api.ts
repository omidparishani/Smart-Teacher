import { apiFetch } from './api-client';
import type {
  ClassDashboard,
  ClassRoom,
  ClassStudent,
  CreateClassRoomBody,
  EnrollStudentBody,
  PageResponse,
} from '../types/api';

/**
 * تماس‌های HTTP مربوط به کلاس و ثبت‌نام.
 *
 * <p>⚠️ جدا از {@code api-client} است تا مسیرها و شکل پاسخ در یک جا
 * بمانند و کامپوننت‌ها {@code fetch} ننویسند.
 */

export function listClasses(page = 0, size = 50): Promise<PageResponse<ClassRoom>> {
  return apiFetch<PageResponse<ClassRoom>>(`/classes?page=${page}&size=${size}`);
}

export function getClass(id: string): Promise<ClassRoom> {
  return apiFetch<ClassRoom>(`/classes/${id}`);
}

export function createClass(body: CreateClassRoomBody): Promise<ClassRoom> {
  return apiFetch<ClassRoom>('/classes', { method: 'POST', body });
}

export function getClassDashboard(id: string): Promise<ClassDashboard> {
  return apiFetch<ClassDashboard>(`/classes/${id}/dashboard`);
}

export function listClassStudents(
  classId: string,
  page = 0,
  size = 100,
): Promise<PageResponse<ClassStudent>> {
  return apiFetch<PageResponse<ClassStudent>>(
    `/classes/${classId}/students?page=${page}&size=${size}`,
  );
}

export function enrollStudent(
  classId: string,
  body: EnrollStudentBody,
): Promise<ClassStudent> {
  return apiFetch<ClassStudent>(`/classes/${classId}/students`, {
    method: 'POST',
    body,
  });
}

export function unenrollStudent(classId: string, studentId: string): Promise<void> {
  return apiFetch<void>(`/classes/${classId}/students/${studentId}`, {
    method: 'DELETE',
  });
}
