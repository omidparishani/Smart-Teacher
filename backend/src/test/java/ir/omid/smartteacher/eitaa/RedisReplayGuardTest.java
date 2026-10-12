package ir.omid.smartteacher.eitaa;

import static org.assertj.core.api.Assertions.assertThat;

import ir.omid.smartteacher.eitaa.internal.RedisReplayGuard;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RedisReplayGuardTest {

    @Test
    @DisplayName("نگه‌بان Redis اثر انگشت تکراری را رد می‌کند")
    void redisReplayGuard_detectsDuplicate() {
        RedisReplayGuard.InProcessRedisReplayStore store =
                new RedisReplayGuard.InProcessRedisReplayStore();
        RedisReplayGuard guard = new RedisReplayGuard(store);
        Duration window = Duration.ofMinutes(5);

        assertThat(guard.markUsed("fp-1", window)).isTrue();
        assertThat(guard.markUsed("fp-1", window)).isFalse();
        assertThat(guard.markUsed("fp-2", window)).isTrue();
    }

    @Test
    @DisplayName("نگه‌بان Redis اثر انگشت خالی را رد می‌کند")
    void redisReplayGuard_rejectsBlank() {
        RedisReplayGuard.InProcessRedisReplayStore store =
                new RedisReplayGuard.InProcessRedisReplayStore();
        RedisReplayGuard guard = new RedisReplayGuard(store);

        assertThat(guard.markUsed("", Duration.ofMinutes(5))).isFalse();
        assertThat(guard.markUsed(null, Duration.ofMinutes(5))).isFalse();
    }
}
