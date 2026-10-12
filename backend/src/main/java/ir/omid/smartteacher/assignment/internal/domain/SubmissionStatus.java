package ir.omid.smartteacher.assignment.internal.domain;

/**
 * وضعیت ارسال پاسخ دانش‌آموز.
 */
public enum SubmissionStatus {
    PENDING,
    SUBMITTED,
    AI_EVALUATED,
    REVIEWED,
    GRADED,
    RETURNED
}
