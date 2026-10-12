package ir.omid.smartteacher.file.internal;

import ir.omid.smartteacher.file.FileStorage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * آداپتر stub MinIO — وقتی {@code app.storage.provider=MINIO}.
 *
 * <p>بدون کلاینت واقعی MinIO: در حافظه نگه‌داری می‌کند تا تست‌ها و
 * پروفایل بدون MinIO زنده بمانند. اتصال واقعی بعداً بدون تغییر قرارداد
 * {@link FileStorage} اضافه می‌شود.
 */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "MINIO")
public class MinioFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(MinioFileStorage.class);

    private final ConcurrentHashMap<String, byte[]> objects = new ConcurrentHashMap<>();

    public MinioFileStorage() {
        log.info("MinioFileStorage stub فعال است (بدون سرور MinIO واقعی).");
    }

    @Override
    public void store(String relativeKey, InputStream content, long sizeHint) throws IOException {
        objects.put(relativeKey, content.readAllBytes());
    }

    @Override
    public InputStream open(String relativeKey) throws IOException {
        byte[] data = objects.get(relativeKey);
        if (data == null) {
            throw new IOException("شیء MinIO یافت نشد: " + relativeKey);
        }
        return new ByteArrayInputStream(data);
    }

    @Override
    public void delete(String relativeKey) {
        objects.remove(relativeKey);
    }

    @Override
    public boolean exists(String relativeKey) {
        return objects.containsKey(relativeKey);
    }
}
