package ir.omid.smartteacher.book.internal.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * دسترسی به صفحات کتاب.
 */
public interface BookPageRepository extends JpaRepository<BookPage, UUID> {

    List<BookPage> findByBookIdOrderByPageNumberAsc(UUID bookId);

    Optional<BookPage> findByBookIdAndPageNumber(UUID bookId, int pageNumber);

    boolean existsByBookIdAndPageNumber(UUID bookId, int pageNumber);

    long countByBookId(UUID bookId);
}
