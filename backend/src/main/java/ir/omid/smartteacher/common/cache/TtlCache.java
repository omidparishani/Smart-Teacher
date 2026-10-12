package ir.omid.smartteacher.common.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * کش TTL در حافظه — پیش‌فرض وقتی Redis خاموش است.
 */
public final class TtlCache<K, V> {

    private final ConcurrentHashMap<K, Entry<V>> store = new ConcurrentHashMap<>();
    private final Duration ttl;

    public TtlCache(Duration ttl) {
        this.ttl = ttl == null ? Duration.ofMinutes(1) : ttl;
    }

    public Optional<V> get(K key) {
        Entry<V> entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt.isBefore(Instant.now())) {
            store.remove(key, entry);
            return Optional.empty();
        }
        return Optional.of(entry.value);
    }

    public V getOrLoad(K key, Supplier<V> loader) {
        return get(key).orElseGet(() -> {
            V loaded = loader.get();
            put(key, loaded);
            return loaded;
        });
    }

    public void put(K key, V value) {
        store.put(key, new Entry<>(value, Instant.now().plus(ttl)));
    }

    public void invalidate(K key) {
        store.remove(key);
    }

    public void clear() {
        store.clear();
    }

    private record Entry<V>(V value, Instant expiresAt) {
    }
}
