package pl.grzeslowski.openhab.supla.internal.server.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.openhab.core.types.UnDefType.UNDEF;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;
import org.slf4j.LoggerFactory;

class InMemoryStateCacheIT {
    final InMemoryStateCache cache = new InMemoryStateCache(LoggerFactory.getLogger(InMemoryStateCache.class));

    final ChannelUID uid = new ChannelUID("binding:thing:1:channel");
    final State state = new StringType("some state");

    @Test
    @DisplayName("should return UNDEF if channel state go invalid")
    void validity() {
        // when
        cache.saveState(uid, state, Duration.ofSeconds(1));

        // then
        assertThat(cache.findState(uid)).contains(state);
        await().untilAsserted(() -> assertThat(cache.findState(uid)).contains(UNDEF));
        assertThat(cache.findState(uid)).isEmpty();
    }
}
