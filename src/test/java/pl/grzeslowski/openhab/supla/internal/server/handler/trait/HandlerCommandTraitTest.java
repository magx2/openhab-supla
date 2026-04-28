package pl.grzeslowski.openhab.supla.internal.server.handler.trait;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.openhab.core.library.types.OnOffType.OFF;
import static org.openhab.core.library.types.OnOffType.ON;
import static org.openhab.core.thing.ThingStatus.ONLINE;

import io.netty.util.concurrent.GenericFutureListener;
import java.util.HashMap;
import java.util.Optional;
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
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaChannelNewValue;
import pl.grzeslowski.jsupla.server.SuplaWriteFuture;

@ExtendWith(MockitoExtension.class)
class HandlerCommandTraitTest {
    private static final long MESSAGE_ID = 13L;

    @Mock
    private ServerDevice serverDevice;

    @Mock
    private Logger logger;

    @InjectMocks
    private HandlerCommandTrait handlerCommandTrait;

    private HashMap<Long, ServerDevice.ChannelAndPreviousState> messageMap;
    private SuplaWriteFuture successfulFuture;

    @BeforeEach
    void setUp() {
        messageMap = new HashMap<>();
        successfulFuture = successfulWriteFuture(MESSAGE_ID);

        lenient().when(serverDevice.getLogger()).thenReturn(logger);
        lenient().when(serverDevice.getMessageIdToChannelUID()).thenReturn(messageMap);
        lenient().when(serverDevice.write(any())).thenReturn(successfulFuture);
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
        when(serverDevice.findState(channelUID)).thenReturn(Optional.of(state));

        handlerCommandTrait.handleRefreshCommand(channelUID);

        verify(serverDevice).updateState(channelUID, state);
    }

    @Test
    void shouldNotRefreshWhenStateMissing() {
        ChannelUID channelUID = new ChannelUID("binding:thing:sub:1");
        when(serverDevice.findState(channelUID)).thenReturn(Optional.empty());

        handlerCommandTrait.handleRefreshCommand(channelUID);

        verify(serverDevice, never()).updateState(any(), any());
    }

    @Test
    void shouldSendOnOffCommandAndRecordPreviousState() {
        ChannelUID channelUID = new ChannelUID("binding:thing:sub:1");

        handlerCommandTrait.handleOnOffCommand(channelUID, ON);

        assertThat(messageMap).hasSize(1);
        var entry = messageMap.entrySet().iterator().next();
        assertThat(entry.getKey()).isEqualTo(MESSAGE_ID);
        ServerDevice.ChannelAndPreviousState value = entry.getValue();
        assertThat(value.channelUID()).isEqualTo(channelUID);
        assertThat((OnOffType) value.previousState()).isEqualTo(OFF);
        verify(serverDevice)
                .write(argThat(proto -> proto instanceof SuplaChannelNewValue newValue
                        && newValue.senderId() == ServerDevice.SENDER_ID));
        verify(serverDevice).updateStatus(ONLINE);
    }

    @Test
    void shouldFailWhenChannelNumberCannotBeParsed() {
        ChannelUID channelUID = new ChannelUID("binding:thing:sub:notANumber");

        assertThatThrownBy(() -> handlerCommandTrait.handleOnOffCommand(channelUID, ON))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot find channel number from");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static SuplaWriteFuture successfulWriteFuture(long messageId) {
        var future = mock(SuplaWriteFuture.class);
        lenient().when(future.msgId()).thenReturn(messageId);
        lenient().when(future.addListener(any())).thenAnswer(invocation -> {
            var listener = (GenericFutureListener) invocation.getArgument(0);
            listener.operationComplete(future);
            return future;
        });
        return future;
    }
}
