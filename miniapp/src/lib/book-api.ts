import { apiFetch } from './api-client';
import type {
  Book,
  BookPage,
  ContentAnalysis,
  CreateBookBody,
  PageResponse,
} from '../types/api';

/**
 * تماس‌های HTTP کتابخانه — {@code /books}.
 */

export function listBooks(page = 0, size = 50): Promise<PageResponse<Book>> {
  return apiFetch<PageResponse<Book>>(`/books?page=${page}&size=${size}`);
}

export function getBook(id: string): Promise<Book> {
  return apiFetch<Book>(`/books/${id}`);
}

export function createBook(body: CreateBookBody): Promise<Book> {
  return apiFetch<Book>('/books', { method: 'POST', body });
}

export function listBookPages(bookId: string): Promise<BookPage[]> {
  return apiFetch<BookPage[]>(`/books/${bookId}/pages`);
}

/** آپلود تصویر یا PDF صفحه. */
export function uploadBookPage(
  bookId: string,
  pageNumber: number,
  file: File,
): Promise<BookPage> {
  const form = new FormData();
  form.append('pageNumber', String(pageNumber));
  form.append('file', file);
  return apiFetch<BookPage>(`/books/${bookId}/pages`, {
    method: 'POST',
    body: form,
  });
}

/** آپلود PDF کامل و استخراج صفحات. */
export function uploadBookPdf(bookId: string, file: File): Promise<BookPage[]> {
  const form = new FormData();
  form.append('file', file);
  return apiFetch<BookPage[]>(`/books/${bookId}/pdf`, {
    method: 'POST',
    body: form,
  });
}

export function runPageOcr(bookId: string, pageNumber: number): Promise<BookPage> {
  return apiFetch<BookPage>(`/books/${bookId}/pages/${pageNumber}/ocr`, {
    method: 'POST',
  });
}

export function analyzePage(
  bookId: string,
  pageNumber: number,
): Promise<ContentAnalysis> {
  return apiFetch<ContentAnalysis>(`/books/${bookId}/pages/${pageNumber}/analyze`, {
    method: 'POST',
  });
}

export function listBookAnalyses(bookId: string): Promise<ContentAnalysis[]> {
  return apiFetch<ContentAnalysis[]>(`/books/${bookId}/analyses`);
}
