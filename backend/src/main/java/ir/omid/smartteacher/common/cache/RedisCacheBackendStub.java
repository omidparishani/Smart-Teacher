package ir.omid.smartteacher.common.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stub Redis cache — وقتی {@code app.redis.enabled=true} فعال می‌شود.
 *
 * <p>فعلاً در حافظه نگه می‌دارد؛ اتصال Lettuce در گام بعد.
 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisCacheBackendStub implements CacheBackend {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheBackendStub.class);

    private final InMemoryCacheBackend delegate = new InMemoryCacheBackend();

    public RedisCacheBackendStub() {
        log.info("RedisCacheBackendStub فعال — کش فعلاً in-memory (stub)");
    }

    @Override
    public <V> Optional<V> get(String key, Class<V> type) {
        return delegate.get(key, type);
    }

    @Override
    public <V> V getOrLoad(String key, Class<V> type, Duration ttl, Supplier<V> loader) {
        return delegate.getOrLoad(key, type, ttl, loader);
    }

    @Override
    public void invalidate(String key) {
        delegate.invalidate(key);
    }
}
