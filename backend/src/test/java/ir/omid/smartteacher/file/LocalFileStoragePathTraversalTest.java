package ir.omid.smartteacher.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ir.omid.smartteacher.common.error.AppException;
import ir.omid.smartteacher.common.error.ErrorCode;
import ir.omid.smartteacher.file.internal.LocalFileStorage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * آزمون مسیر پیمایش — فایل هرگز بیرون از ریشه ذخیره نمی‌شود.
 *
 * <p>از API عمومی {@link FileStorage#store} استفاده می‌کند تا به متدهای
 * داخلی بسته {@code internal} وابسته نباشد.
 */
class LocalFileStoragePathTraversalTest {

    @TempDir
    Path tempDir;

    private FileStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalFileStorage(new FileStorageProperties(tempDir));
    }

    @Test
    @DisplayName("کلید با ../ بیرون از ریشه رد می‌شود")
    void rejectsParentTraversal() {
        byte[] bytes = "x".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> storage.store(
                "../outside.txt", new ByteArrayInputStream(bytes), bytes.length))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST));

        assertThat(Files.exists(tempDir.resolveSibling("outside.txt"))).isFalse();
    }

    @Test
    @DisplayName("کلید با a/../../ بیرون از ریشه رد می‌شود")
    void rejectsNestedTraversal() {
        byte[] bytes = "x".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> storage.store(
                "a/../../outside.txt", new ByteArrayInputStream(bytes), bytes.length))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    @DisplayName("کلید سالم داخل ریشه ذخیره می‌شود")
    void storesInsideBase() throws Exception {
        byte[] bytes = "سلام".getBytes(StandardCharsets.UTF_8);
        storage.store("tenant-1/page-1.png", new ByteArrayInputStream(bytes), bytes.length);

        Path stored = tempDir.resolve("tenant-1").resolve("page-1.png");
        assertThat(Files.isRegularFile(stored)).isTrue();
        assertThat(Files.readAllBytes(stored)).isEqualTo(bytes);
        assertThat(stored.toAbsolutePath().normalize()
                .startsWith(tempDir.toAbsolutePath().normalize()))
                .isTrue();
    }

    @Test
    @DisplayName("کلید خالی رد می‌شود")
    void rejectsBlankKey() {
        byte[] bytes = "x".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> storage.store(
                "  ", new ByteArrayInputStream(bytes), bytes.length))
                .isInstanceOf(AppException.class);
    }
}
