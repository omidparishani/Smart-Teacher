package ir.omid.smartteacher.file.internal.application;

import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.file.FileCatalog;
import ir.omid.smartteacher.file.FileStorage;
import ir.omid.smartteacher.file.internal.domain.ScanStatus;
import ir.omid.smartteacher.file.internal.domain.StorageProvider;
import ir.omid.smartteacher.file.internal.domain.StoredFile;
import ir.omid.smartteacher.file.internal.domain.StoredFileRepository;
import ir.omid.smartteacher.security.TenantGuard;
import ir.omid.smartteacher.tenant.TenantContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * پیاده‌سازی {@link FileCatalog} — ذخیره روی {@link FileStorage} +
 * رکورد در جدول {@code files}.
 *
 * <p>سقف {@code storageBytes} در لایه فراخوان (مثلاً {@code BookService})
 * با {@code EntitlementService} اعمال می‌شود تا ماژول file به billing
 * وابسته نشود.
 */
@Service
public class FileCatalogService implements FileCatalog {

    private final FileStorage fileStorage;
    private final StoredFileRepository storedFileRepository;
    private final TenantGuard tenantGuard;

    public FileCatalogService(FileStorage fileStorage,
                              StoredFileRepository storedFileRepository,
                              TenantGuard tenantGuard) {
        this.fileStorage = fileStorage;
        this.storedFileRepository = storedFileRepository;
        this.tenantGuard = tenantGuard;
    }

    @Override
    @Transactional
    public StoredFileRef store(String originalFilename,
                               String contentType,
                               InputStream content,
                               long sizeBytes,
                               String entityType,
                               UUID entityId) {
        if (content == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "محتوای فایل خالی است.");
        }
        if (sizeBytes < 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "اندازه فایل نامعتبر است.");
        }

        // شناسه از BaseEntity (UUID v7) — کلید ذخیره‌سازی همان شناسه است تا
        // مسیر و رکورد یکی بمانند. پیشوند tenant جلوی هم‌کلیدی دو مستأجر را
        // می‌گیرد و «../» در کلید ساخته‌شده ممکن نیست.
        StoredFile entity = new StoredFile("pending", sizeBytes);
        UUID tenantId = TenantContext.requireTenantId();
        String storageKey = tenantId + "/" + entity.getId();
        entity.setStorageKey(storageKey);

        String checksum;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream digester = new DigestInputStream(content, digest)) {
                fileStorage.store(storageKey, digester, sizeBytes);
            }
            checksum = HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "الگوریتم خلاصه در دسترس نیست.", null, ex);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "ذخیره فایل ناموفق بود.", null, ex);
        }

        entity.setStorageProvider(StorageProvider.LOCAL);
        entity.setOriginalFilename(trimToNull(originalFilename));
        entity.setContentType(trimToNull(contentType));
        entity.setChecksumSha256(checksum);
        entity.setEntityType(trimToNull(entityType));
        entity.setEntityId(entityId);
        entity.setUploadedBy(TenantContext.requireUserId());
        entity.setScanStatus(ScanStatus.PENDING);

        StoredFile saved = storedFileRepository.save(entity);
        return toRef(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredFileRef> findActive(UUID fileId) {
        return storedFileRepository.findByIdAndDeletedAtIsNull(fileId)
                .filter(file -> file.getTenantId().equals(TenantContext.requireTenantId()))
                .map(this::toRef);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream open(UUID fileId) {
        StoredFile file = tenantGuard.requireOwned(
                storedFileRepository.findByIdAndDeletedAtIsNull(fileId).orElse(null),
                "StoredFile");
        try {
            return fileStorage.open(file.getStorageKey());
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "خواندن فایل ناموفق بود.", null, ex);
        }
    }

    @Override
    @Transactional
    public void bindEntity(UUID fileId, String entityType, UUID entityId) {
        StoredFile file = tenantGuard.requireOwned(
                storedFileRepository.findByIdAndDeletedAtIsNull(fileId).orElse(null),
                "StoredFile");
        file.setEntityType(trimToNull(entityType));
        file.setEntityId(entityId);
        storedFileRepository.save(file);
    }

    @Override
    @Transactional(readOnly = true)
    public long totalSizeBytes() {
        Long sum = storedFileRepository.sumSizeBytesActive();
        return sum != null ? sum : 0L;
    }

    private StoredFileRef toRef(StoredFile file) {
        return new StoredFileRef(
                file.getId(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSizeBytes(),
                file.getStorageKey());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
