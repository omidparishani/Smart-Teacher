import { apiFetch, type ApiRequestOptions } from './api-client';
import type { PageResponse } from '../types/api';

/** خلاصه آزمون در فهرست. */
export interface ExamSummary {
  id: string;
  title: string;
  status: string;
  classroomId: string;
  durationMinutes?: number | null;
}

export interface ExamQuestion {
  id: string;
  examId: string;
  type: string;
  questionText: string;
  options?: string | null;
  correctAnswer?: string | null;
  score: number;
  orderIndex: number;
  aiGenerated?: boolean;
}

export interface ExamDetail extends ExamSummary {
  bookId?: string | null;
  description?: string | null;
  questions: ExamQuestion[];
  showResultsImmediately?: boolean;
}

export interface ExamBlueprint {
  id: string;
  examId: string;
  bookId: string;
  pageFrom: number;
  pageTo: number;
  difficultyDistribution?: string | null;
  questionTypeDistribution?: string | null;
  questionCount: number;
  aiUsageId?: string | null;
  createdAt: string;
}

export interface GenerateExamBody {
  classroomId: string;
  bookId: string;
  pageFrom: number;
  pageTo: number;
  title: string;
  description?: string;
  durationMinutes?: number;
  totalScore?: number;
  mcqCount?: number;
  trueFalseCount?: number;
  difficultyDistribution?: string;
  questionTypeDistribution?: string;
}

export interface CreateExamQuestionBody {
  type: string;
  questionText: string;
  options?: string;
  correctAnswer?: string;
  score?: number;
  difficulty?: string;
  explanation?: string;
  orderIndex?: number;
}

export interface PublishExamBody {
  eitaaChatId?: string;
}

export interface ExamAnswer {
  id: string;
  questionId: string;
  answerText?: string | null;
  selectedOptions?: string | null;
  awardedScore?: number | null;
  correct?: boolean | null;
  aiGraded?: boolean;
  aiFeedback?: string | null;
}

export interface ExamAttempt {
  id: string;
  examId: string;
  studentId: string;
  startedAt: string;
  submittedAt?: string | null;
  score?: number | null;
  status: string;
  attemptNumber: number;
  answers: ExamAnswer[];
}

export interface ExamAttemptReport {
  attemptId: string;
  examId: string;
  examTitle: string;
  studentId: string;
  startedAt: string;
  submittedAt?: string | null;
  score?: number | null;
  totalScore?: number | null;
  status: string;
  durationMinutes?: number | null;
  answers: ExamAnswer[];
  questions: ExamQuestion[];
}

export interface ExamResults {
  examId: string;
  title: string;
  attemptCount: number;
  averageScore: number;
  attempts: ExamAttempt[];
}

/** تماس‌های HTTP آزمون — {@code /exams}. */
export function listExams(
  page = 0,
  size = 50,
  options?: ApiRequestOptions,
): Promise<PageResponse<ExamSummary>> {
  return apiFetch<PageResponse<ExamSummary>>(`/exams?page=${page}&size=${size}`, options);
}

export function getExam(id: string, options?: ApiRequestOptions): Promise<ExamDetail> {
  return apiFetch<ExamDetail>(`/exams/${id}`, options);
}

export function generateExam(body: GenerateExamBody, options?: ApiRequestOptions): Promise<ExamDetail> {
  return apiFetch<ExamDetail>('/exams/generate', { method: 'POST', body, ...options });
}

export function getExamBlueprint(examId: string, options?: ApiRequestOptions): Promise<ExamBlueprint> {
  return apiFetch<ExamBlueprint>(`/exams/${examId}/blueprint`, options);
}

export function updateExam(
  id: string,
  body: Partial<{
    title: string;
    description: string;
    durationMinutes: number;
    totalScore: number;
    status: string;
  }>,
  options?: ApiRequestOptions,
): Promise<ExamDetail> {
  return apiFetch<ExamDetail>(`/exams/${id}`, { method: 'PUT', body, ...options });
}

export function deleteExam(id: string, options?: ApiRequestOptions): Promise<void> {
  return apiFetch<void>(`/exams/${id}`, { method: 'DELETE', ...options });
}

export function addQuestion(
  examId: string,
  body: CreateExamQuestionBody,
  options?: ApiRequestOptions,
): Promise<ExamQuestion> {
  return apiFetch<ExamQuestion>(`/exams/${examId}/questions`, {
    method: 'POST',
    body,
    ...options,
  });
}

export function updateQuestion(
  examId: string,
  questionId: string,
  body: CreateExamQuestionBody,
  options?: ApiRequestOptions,
): Promise<ExamQuestion> {
  return apiFetch<ExamQuestion>(`/exams/${examId}/questions/${questionId}`, {
    method: 'PUT',
    body,
    ...options,
  });
}

export function deleteQuestion(
  examId: string,
  questionId: string,
  options?: ApiRequestOptions,
): Promise<void> {
  return apiFetch<void>(`/exams/${examId}/questions/${questionId}`, {
    method: 'DELETE',
    ...options,
  });
}

export function publishExam(
  id: string,
  body: PublishExamBody = {},
  options?: ApiRequestOptions,
): Promise<ExamDetail> {
  return apiFetch<ExamDetail>(`/exams/${id}/publish`, { method: 'POST', body, ...options });
}

export function listAttempts(examId: string, options?: ApiRequestOptions): Promise<ExamAttempt[]> {
  return apiFetch<ExamAttempt[]>(`/exams/${examId}/attempts`, options);
}

export function startExam(
  examId: string,
  studentId: string,
  options?: ApiRequestOptions,
): Promise<ExamAttempt> {
  return apiFetch<ExamAttempt>(`/exams/${examId}/start`, {
    method: 'POST',
    body: { studentId },
    ...options,
  });
}

export function saveAnswer(
  attemptId: string,
  studentId: string,
  payload: { questionId: string; answerText?: string; selectedOptions?: string },
  options?: ApiRequestOptions,
): Promise<ExamAnswer> {
  return apiFetch<ExamAnswer>(
    `/exams/attempts/${attemptId}/answers?studentId=${encodeURIComponent(studentId)}`,
    { method: 'POST', body: payload, ...options },
  );
}

export function submitAttempt(
  attemptId: string,
  studentId: string,
  options?: ApiRequestOptions,
): Promise<ExamAttempt> {
  return apiFetch<ExamAttempt>(
    `/exams/attempts/${attemptId}/submit?studentId=${encodeURIComponent(studentId)}`,
    { method: 'POST', ...options },
  );
}

export function getAttemptReport(
  attemptId: string,
  options?: ApiRequestOptions,
): Promise<ExamAttemptReport> {
  return apiFetch<ExamAttemptReport>(`/exams/attempts/${attemptId}/report`, options);
}

export function getExamResults(examId: string, options?: ApiRequestOptions): Promise<ExamResults> {
  return apiFetch<ExamResults>(`/exams/${examId}/results`, options);
}
