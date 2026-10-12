package ir.omid.smartteacher.book.internal.domain;

import ir.omid.smartteacher.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * یک صفحه از کتاب — جدول {@code book_pages}.
 *
 * <p>حذف نرم ندارد؛ با حذف نرم کتاب از API غایب می‌شود و صفحه‌ها
 * پشت همان شناسه می‌مانند ({@code ON DELETE CASCADE} فقط برای حذف
 * سخت کتاب در دیتابیس).
 */
@Entity
@Table(name = "book_pages")
public class BookPage extends TenantAwareEntity {

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "file_id", nullable = false)
    private UUID fileId;

    @Column(name = "extracted_text")
    private String extractedText;

    @Enumerated(EnumType.STRING)
    @Column(name = "ocr_status", nullable = false, length = 20)
    private OcrStatus ocrStatus = OcrStatus.PENDING;

    @Column(name = "ocr_confidence", precision = 5, scale = 4)
    private BigDecimal ocrConfidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_metadata", columnDefinition = "jsonb")
    private String layoutMetadata;

    protected BookPage() {
    }

    public BookPage(UUID bookId, int pageNumber, UUID fileId) {
        this.bookId = bookId;
        this.pageNumber = pageNumber;
        this.fileId = fileId;
    }

    public UUID getBookId() {
        return bookId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public OcrStatus getOcrStatus() {
        return ocrStatus;
    }

    public void setOcrStatus(OcrStatus ocrStatus) {
        this.ocrStatus = ocrStatus;
    }

    public BigDecimal getOcrConfidence() {
        return ocrConfidence;
    }

    public void setOcrConfidence(BigDecimal ocrConfidence) {
        this.ocrConfidence = ocrConfidence;
    }

    public String getLayoutMetadata() {
        return layoutMetadata;
    }

    public void setLayoutMetadata(String layoutMetadata) {
        this.layoutMetadata = layoutMetadata;
    }
}
