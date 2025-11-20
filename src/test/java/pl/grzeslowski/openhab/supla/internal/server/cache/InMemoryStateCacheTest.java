package pl.grzeslowski.openhab.supla.internal.server.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class InMemoryStateCacheTest {
    @Mock
    private Logger logger;

    @InjectMocks
    private InMemoryStateCache cache;

    @Test
    void shouldStoreAndReturnState() {
        ChannelUID channelUID = new ChannelUID("binding:thing:1:channel");
        State state = new State() {
            @Override
            public <T extends State> T as(Class<T> type) {
                return null;
            }

            @Override
            public String format(String pattern) {
                return "state";
            }

            @Override
            public String toFullString() {
                return "state";
            }
        };

        cache.saveState(channelUID, state);
        var found = cache.findState(channelUID);

        assertThat(found).isEqualTo(state);
        verify(logger).debug("Saving state {}={}", channelUID, state);
        verify(logger).debug("Current state for {} is {}", channelUID, state);
    }

    @Test
    void shouldAllowNullState() {
        ChannelUID channelUID = new ChannelUID("binding:thing:2:channel");

        cache.saveState(channelUID, null);
        var found = cache.findState(channelUID);

        assertThat(found).isNull();
        verify(logger).debug("Saving state {}={}", channelUID, null);
        verify(logger).debug("Current state for {} is {}", channelUID, null);
    }
}
