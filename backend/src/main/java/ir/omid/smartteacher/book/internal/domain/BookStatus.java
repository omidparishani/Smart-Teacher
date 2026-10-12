package ir.omid.smartteacher.book.internal.domain;

/**
 * وضعیت چرخه عمر کتاب — مطابق ERD {@code docs/database.md} بخش ۴.
 */
public enum BookStatus {
    DRAFT,
    PROCESSING,
    READY,
    FAILED
}
