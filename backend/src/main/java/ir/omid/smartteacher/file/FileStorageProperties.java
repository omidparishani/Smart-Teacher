package ir.omid.smartteacher.file;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * پیکربندی ذخیره‌سازی محلی فایل.
 *
 * @param basePath ریشهٔ مطلق یا نسبی برای {@code LocalFileStorage}؛
 *                 پیش‌فرض {@code ./data/files}
 */
@ConfigurationProperties(prefix = "app.storage.local")
public record FileStorageProperties(Path basePath) {

    public FileStorageProperties {
        if (basePath == null) {
            basePath = Path.of("./data/files");
        }
    }
}
