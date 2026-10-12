package ir.omid.smartteacher.common.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryCacheBackend implements CacheBackend {

    private final ConcurrentHashMap<String, Entry<?>> store = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <V> Optional<V> get(String key, Class<V> type) {
        Entry<?> entry = store.get(key);
        if (entry == null || entry.expiresAt.isBefore(Instant.now())) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.of((V) entry.value);
    }

    @Override
    public <V> V getOrLoad(String key, Class<V> type, Duration ttl, Supplier<V> loader) {
        return get(key, type).orElseGet(() -> {
            V loaded = loader.get();
            store.put(key, new Entry<>(loaded, Instant.now().plus(ttl)));
            return loaded;
        });
    }

    @Override
    public void invalidate(String key) {
        store.remove(key);
    }

    private record Entry<V>(V value, Instant expiresAt) {
    }
}
