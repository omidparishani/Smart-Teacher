package ir.omid.smartteacher.file.internal;

import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.file.FileStorage;
import ir.omid.smartteacher.file.FileStorageProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * ذخیره‌سازی روی دیسک محلی زیر {@link FileStorageProperties#basePath()}.
 *
 * <p><b>🔴 قاعده امنیتی:</b> هر کلید نسبی پس از نرمال‌سازی باید همچنان
 * داخل ریشه بماند. اگر مهاجم {@code ../../../etc/passwd} بفرستد، اینجا
 * رد می‌شود — نه اینکه بی‌صدا بیرون از ریشه نوشته شود.
 */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "LOCAL", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

    private final Path basePath;

    public LocalFileStorage(FileStorageProperties properties) {
        this.basePath = properties.basePath().toAbsolutePath().normalize();
    }

    @PostConstruct
    void ensureBaseExists() throws IOException {
        Files.createDirectories(basePath);
        log.info("ریشه ذخیره‌سازی محلی آماده است: {}", basePath);
    }

    @Override
    public void store(String relativeKey, InputStream content, long sizeHint) throws IOException {
        Path target = resolveInsideBase(relativeKey);
        Files.createDirectories(target.getParent());

        // نوشتن موقت سپس جابه‌جایی — تا فایل نیمه‌نوشته به‌عنوان کامل دیده نشود.
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try (OutputStream out = Files.newOutputStream(temp)) {
            content.transferTo(out);
        } catch (IOException ex) {
            Files.deleteIfExists(temp);
            throw ex;
        }

        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException moveEx) {
            // AtomicMoveNotSupportedException زیرکلاس IOException است —
            // ویندوز/بعضی FSها ATOMIC_MOVE نمی‌دهند؛ بدون آن جابه‌جا می‌کنیم.
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                Files.deleteIfExists(temp);
                throw ex;
            }
        }
    }

    @Override
    public InputStream open(String relativeKey) throws IOException {
        Path target = resolveInsideBase(relativeKey);
        if (!Files.isRegularFile(target)) {
            throw new IOException("فایل یافت نشد: " + relativeKey);
        }
        return Files.newInputStream(target);
    }

    @Override
    public void delete(String relativeKey) throws IOException {
        Path target = resolveInsideBase(relativeKey);
        Files.deleteIfExists(target);
    }

    @Override
    public boolean exists(String relativeKey) {
        try {
            Path target = resolveInsideBase(relativeKey);
            return Files.isRegularFile(target);
        } catch (AppException ex) {
            return false;
        }
    }

    /**
     * تبدیل کلید نسبی به مسیر مطلق داخل ریشه.
     *
     * <p>⚠️ عمداً {@link AppException} می‌دهد نه {@link IOException} تا
     * تست مسیر پیمایش بتواند کد {@code INVALID_REQUEST} را ببیند.
     */
    Path resolveInsideBase(String relativeKey) {
        if (relativeKey == null || relativeKey.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "کلید ذخیره‌سازی خالی است.");
        }
        if (relativeKey.contains("\0")) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "کلید ذخیره‌سازی نامعتبر است.");
        }

        Path resolved = basePath.resolve(relativeKey).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "مسیر فایل خارج از ریشه ذخیره‌سازی مجاز نیست.");
        }
        return resolved;
    }

    /** فقط برای تست — ریشه نرمال‌شده. */
    Path basePath() {
        return basePath;
    }
}
