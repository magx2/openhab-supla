package pl.grzeslowski.openhab.supla.internal.server.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class InMemoryStateCacheTest {
    @Mock
    private Logger logger;

    @InjectMocks
    private InMemoryStateCache cache;

    final ChannelUID uid = new ChannelUID("binding:thing:1:channel");
    final State state = new StringType("some state");

    @Test
    void shouldStoreAndReturnState() {
        cache.saveState(uid, state, null);
        var found = cache.findState(uid);

        assertThat(found).hasValue(state);
    }

    @Test
    void shouldAllowNullState() {
        cache.saveState(uid, null, null);
        var found = cache.findState(uid);

        assertThat(found).isEmpty();
    }
}
