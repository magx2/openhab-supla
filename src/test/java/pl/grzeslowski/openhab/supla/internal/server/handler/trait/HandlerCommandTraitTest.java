package pl.grzeslowski.openhab.supla.internal.server.handler.trait;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.openhab.core.library.types.OnOffType.OFF;
import static org.openhab.core.library.types.OnOffType.ON;
import static org.openhab.core.thing.ThingStatus.ONLINE;

import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class HandlerCommandTraitTest {
    @Mock
    private SuplaDevice suplaDevice;

    @Mock
    private Logger logger;

    @InjectMocks
    private HandlerCommandTrait handlerCommandTrait;

    private AtomicInteger senderId;
    private HashMap<Integer, SuplaDevice.ChannelAndPreviousState> senderMap;
    private ChannelFuture successfulFuture;

    @BeforeEach
    void setUp() {
        senderId = new AtomicInteger();
        senderMap = new HashMap<>();
        var channel = new EmbeddedChannel();
        successfulFuture = new DefaultChannelPromise(channel).setSuccess(null);

        lenient().when(suplaDevice.getLogger()).thenReturn(logger);
        lenient().when(suplaDevice.getSenderId()).thenReturn(senderId);
        lenient().when(suplaDevice.getSenderIdToChannelUID()).thenReturn(senderMap);
        lenient().when(suplaDevice.write(any())).thenReturn(successfulFuture);
    }

    @Test
    void shouldRefreshStateWhenPresent() {
        ChannelUID channelUID = new ChannelUID("binding:thing:sub:1");
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
        when(suplaDevice.findState(channelUID)).thenReturn(state);

        handlerCommandTrait.handleRefreshCommand(channelUID);

        verify(suplaDevice).updateState(channelUID, state);
    }

    @Test
    void shouldNotRefreshWhenStateMissing() {
        ChannelUID channelUID = new ChannelUID("binding:thing:sub:1");
        when(suplaDevice.findState(channelUID)).thenReturn(null);

        handlerCommandTrait.handleRefreshCommand(channelUID);

        verify(suplaDevice, never()).updateState(any(), any());
    }

    @Test
    void shouldSendOnOffCommandAndRecordPreviousState() {
        ChannelUID channelUID = new ChannelUID("binding:thing:sub:1");

        handlerCommandTrait.handleOnOffCommand(channelUID, ON);

        assertThat(senderMap).hasSize(1);
        var entry = senderMap.entrySet().iterator().next();
        assertThat(entry.getKey()).isZero();
        SuplaDevice.ChannelAndPreviousState value = entry.getValue();
        assertThat(value.channelUID()).isEqualTo(channelUID);
        assertThat((OnOffType) value.previousState()).isEqualTo(OFF);
        verify(suplaDevice).write(any());
        verify(suplaDevice).updateStatus(ONLINE);
    }

    @Test
    void shouldFailWhenChannelNumberCannotBeParsed() {
        ChannelUID channelUID = new ChannelUID("binding:thing:sub:notANumber");

        assertThatThrownBy(() -> handlerCommandTrait.handleOnOffCommand(channelUID, ON))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot find channel number from");
    }
}
