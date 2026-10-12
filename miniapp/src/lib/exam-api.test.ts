import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import {
  configureAuth,
  resetAuthBridgeForTest,
  type AuthBridge,
} from './api-client';
import {
  generateExam,
  getExamBlueprint,
  publishExam,
  updateQuestion,
} from './exam-api';

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

interface RecordedCall {
  url: string;
  method: string;
}

function urlOf(input: RequestInfo | URL): string {
  if (typeof input === 'string') return input;
  return input instanceof URL ? input.href : input.url;
}

function recordingFetch(
  handler: (call: RecordedCall) => Response | Promise<Response>,
): { fetchImpl: typeof fetch; calls: RecordedCall[] } {
  const calls: RecordedCall[] = [];
  const fetchImpl = (input: RequestInfo | URL, init?: RequestInit) => {
    const call: RecordedCall = {
      url: urlOf(input),
      method: init?.method ?? 'GET',
    };
    calls.push(call);
    return Promise.resolve(handler(call));
  };
  return { fetchImpl, calls };
}

function bridge(): AuthBridge {
  return {
    getAccessToken: () => 'access-1',
    refresh: () => Promise.resolve('access-2'),
    onSessionLost: () => undefined,
  };
}

describe('exam-api', () => {
  beforeEach(() => {
    configureAuth(bridge());
  });

  afterEach(() => {
    resetAuthBridgeForTest();
  });

  it('generateExam — POST /exams/generate', async () => {
    const { fetchImpl, calls } = recordingFetch(() =>
      jsonResponse(201, { id: 'e1', title: 'آزمون', questions: [] }),
    );

    await generateExam(
      {
        classroomId: 'c1',
        bookId: 'b1',
        pageFrom: 1,
        pageTo: 5,
        title: 'آزمون',
        mcqCount: 2,
        trueFalseCount: 1,
      },
      { fetchImpl },
    );

    expect(calls).toHaveLength(1);
    expect(calls[0]?.method).toBe('POST');
    expect(calls[0]?.url).toContain('/exams/generate');
  });

  it('getExamBlueprint — GET /exams/{id}/blueprint', async () => {
    const { fetchImpl, calls } = recordingFetch(() =>
      jsonResponse(200, {
        id: 'bp1',
        examId: 'e1',
        bookId: 'b1',
        pageFrom: 1,
        pageTo: 3,
        questionCount: 4,
        createdAt: '2026-01-01T00:00:00Z',
      }),
    );

    const result = await getExamBlueprint('e1', { fetchImpl });

    expect(result.examId).toBe('e1');
    expect(calls[0]?.method).toBe('GET');
    expect(calls[0]?.url).toContain('/exams/e1/blueprint');
  });

  it('updateQuestion — PUT /exams/{id}/questions/{qid}', async () => {
    const { fetchImpl, calls } = recordingFetch(() =>
      jsonResponse(200, {
        id: 'q1',
        examId: 'e1',
        type: 'MCQ',
        questionText: 'جدید',
        score: 1,
        orderIndex: 0,
      }),
    );

    await updateQuestion(
      'e1',
      'q1',
      { type: 'MCQ', questionText: 'جدید' },
      { fetchImpl },
    );

    expect(calls[0]?.method).toBe('PUT');
    expect(calls[0]?.url).toContain('/exams/e1/questions/q1');
  });

  it('publishExam — POST /exams/{id}/publish', async () => {
    const { fetchImpl, calls } = recordingFetch(() =>
      jsonResponse(200, { id: 'e1', title: 'آزمون', status: 'ACTIVE', questions: [] }),
    );

    await publishExam('e1', { eitaaChatId: '@group' }, { fetchImpl });

    expect(calls[0]?.method).toBe('POST');
    expect(calls[0]?.url).toContain('/exams/e1/publish');
  });
});
