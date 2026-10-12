package ir.omid.smartteacher.common.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * abstraction برای کش — InMemory پیش‌فرض؛ Redis stub وقتی {@code app.redis.enabled=true}.
 */
public interface CacheBackend {

    <V> Optional<V> get(String key, Class<V> type);

    <V> V getOrLoad(String key, Class<V> type, Duration ttl, Supplier<V> loader);

    void invalidate(String key);
}
