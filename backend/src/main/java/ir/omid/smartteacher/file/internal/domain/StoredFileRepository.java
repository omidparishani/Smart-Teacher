package ir.omid.smartteacher.file.internal.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * دسترسی به متادیتای فایل.
 *
 * <p>فیلتر مستأجر خودکار است ({@code @TenantId}). حذف نرم فیلتر خودکار
 * ندارد — همیشه {@code findByIdAndDeletedAtIsNull}.
 */
public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {

    Optional<StoredFile> findByIdAndDeletedAtIsNull(UUID id);

    /** جمع اندازه فایل‌های فعال مستأجر جاری (فیلتر {@code @TenantId}). */
    @Query("select coalesce(sum(f.sizeBytes), 0) from StoredFile f where f.deletedAt is null")
    Long sumSizeBytesActive();
}
