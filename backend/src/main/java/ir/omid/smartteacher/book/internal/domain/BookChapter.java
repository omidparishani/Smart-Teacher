package ir.omid.smartteacher.book.internal.domain;

import ir.omid.smartteacher.tenant.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * فصل کتاب — جدول {@code book_chapters}.
 *
 * <p>در گام ۱ فقط Schema و موجودیت؛ API فصل در گام‌های بعدی فاز ۶.
 */
@Entity
@Table(name = "book_chapters")
public class BookChapter extends TenantAwareEntity {

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "start_page")
    private Integer startPage;

    @Column(name = "end_page")
    private Integer endPage;

    protected BookChapter() {
    }

    public BookChapter(UUID bookId, String title, int orderIndex) {
        this.bookId = bookId;
        this.title = title;
        this.orderIndex = orderIndex;
    }

    public UUID getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Integer getStartPage() {
        return startPage;
    }

    public void setStartPage(Integer startPage) {
        this.startPage = startPage;
    }

    public Integer getEndPage() {
        return endPage;
    }

    public void setEndPage(Integer endPage) {
        this.endPage = endPage;
    }
}
