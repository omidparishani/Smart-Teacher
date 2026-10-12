package ir.omid.smartteacher.book.internal.domain;

/**
 * وضعیت OCR یک صفحه — مطابق ERD.
 */
public enum OcrStatus {
    PENDING,
    DONE,
    FAILED,
    SKIPPED
}
