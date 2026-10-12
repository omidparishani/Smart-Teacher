package ir.omid.smartteacher.book.internal.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * دسترسی به فصل‌های کتاب.
 */
public interface BookChapterRepository extends JpaRepository<BookChapter, UUID> {

    List<BookChapter> findByBookIdOrderByOrderIndexAsc(UUID bookId);

    long countByBookId(UUID bookId);
}
