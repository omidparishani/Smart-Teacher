import { apiFetch } from './api-client';
import type {
  Assignment,
  AssignmentSubmission,
  CreateAssignmentBody,
  CreateSubmissionBody,
  Evaluation,
  PageResponse,
  PublishAssignmentBody,
  ReviewAction,
  TeacherReview,
} from '../types/api';

/**
 * تماس‌های HTTP تکالیف — {@code /assignments}.
 */

export function listAssignments(page = 0, size = 50): Promise<PageResponse<Assignment>> {
  return apiFetch<PageResponse<Assignment>>(`/assignments?page=${page}&size=${size}`);
}

export function getAssignment(id: string): Promise<Assignment> {
  return apiFetch<Assignment>(`/assignments/${id}`);
}

export function createAssignment(body: CreateAssignmentBody): Promise<Assignment> {
  return apiFetch<Assignment>('/assignments', { method: 'POST', body });
}

export function publishAssignment(
  id: string,
  body: PublishAssignmentBody = {},
): Promise<Assignment> {
  return apiFetch<Assignment>(`/assignments/${id}/publish`, { method: 'POST', body });
}

export function listSubmissions(assignmentId: string): Promise<AssignmentSubmission[]> {
  return apiFetch<AssignmentSubmission[]>(`/assignments/${assignmentId}/submissions`);
}

export function submitAnswer(
  assignmentId: string,
  body: CreateSubmissionBody,
): Promise<AssignmentSubmission> {
  return apiFetch<AssignmentSubmission>(`/assignments/${assignmentId}/submissions`, {
    method: 'POST',
    body,
  });
}

export function evaluateSubmission(
  assignmentId: string,
  submissionId: string,
): Promise<Evaluation> {
  return apiFetch<Evaluation>(
    `/assignments/${assignmentId}/submissions/${submissionId}/evaluate`,
    { method: 'POST' },
  );
}

export function getEvaluation(
  assignmentId: string,
  submissionId: string,
): Promise<Evaluation> {
  return apiFetch<Evaluation>(
    `/assignments/${assignmentId}/submissions/${submissionId}/evaluation`,
  );
}

export function reviewSubmission(
  assignmentId: string,
  submissionId: string,
  body: { action: ReviewAction; newScore?: number | null; comment?: string | null },
): Promise<TeacherReview> {
  return apiFetch<TeacherReview>(
    `/assignments/${assignmentId}/submissions/${submissionId}/review`,
    { method: 'POST', body },
  );
}

export function listReviews(
  assignmentId: string,
  submissionId: string,
): Promise<TeacherReview[]> {
  return apiFetch<TeacherReview[]>(
    `/assignments/${assignmentId}/submissions/${submissionId}/reviews`,
  );
}

export function reevaluateAll(assignmentId: string): Promise<Evaluation[]> {
  return apiFetch<Evaluation[]>(`/assignments/${assignmentId}/reevaluate-all`, {
    method: 'POST',
  });
}
