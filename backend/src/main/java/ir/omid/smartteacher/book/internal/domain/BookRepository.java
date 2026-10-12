package ir.omid.smartteacher.book.internal.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * دسترسی به کتاب‌ها.
 *
 * <p>فیلتر مستأجر خودکار است. حذف نرم صریح در کوئری‌ها.
 */
public interface BookRepository extends JpaRepository<Book, UUID> {

    Optional<Book> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            select b from Book b
            where b.deletedAt is null
              and (:pattern is null or lower(b.title) like :pattern escape '\\')
              and (:status is null or b.status = :status)
            """)
    Page<Book> findForListing(@Param("pattern") String pattern,
                              @Param("status") BookStatus status,
                              Pageable pageable);
}
