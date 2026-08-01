package pl.grzeslowski.openhab.supla.internal.server.handler.trait;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.openhab.core.library.types.OnOffType.OFF;
import static org.openhab.core.library.types.OnOffType.ON;
import static org.openhab.core.library.types.OpenClosedType.OPEN;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.BINDING_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.GATEWAY_LOCK_VALUE_CHANNEL_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.GATE_VALUE_CHANNEL_ID;

import io.netty.util.concurrent.GenericFutureListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import pl.grzeslowski.jsupla.protocol.api.channeltype.encoders.ChannelTypeEncoder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.GateValue;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.GatewayLockValue;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaChannelNewValue;
import pl.grzeslowski.jsupla.server.SuplaWriteFuture;

@ExtendWith(MockitoExtension.class)
class HandlerCommandTraitTest {
    @Mock
    private ServerDevice serverDevice;

    @Mock
    private Logger logger;

    @InjectMocks
    private HandlerCommandTrait handlerCommandTrait;

    private HashMap<Integer, ServerDevice.ChannelAndPreviousState> channelNumberMap;
    private SuplaWriteFuture successfulFuture;

    @BeforeEach
    void setUp() {
        channelNumberMap = new HashMap<>();
        successfulFuture = successfulWriteFuture();

        lenient().when(serverDevice.getLogger()).thenReturn(logger);
        lenient().when(serverDevice.getChannelNumberToChannelUID()).thenReturn(channelNumberMap);
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

        assertThat(channelNumberMap).hasSize(1);
        var entry = channelNumberMap.entrySet().iterator().next();
        assertThat(entry.getKey()).isEqualTo(1);
        ServerDevice.ChannelAndPreviousState value = entry.getValue();
        assertThat(value.channelUID()).isEqualTo(channelUID);
        assertThat((OnOffType) value.previousState()).isEqualTo(OFF);
        verify(serverDevice)
                .write(argThat(proto -> proto instanceof SuplaChannelNewValue newValue
                        && newValue.senderId() == ServerDevice.SENDER_ID));
        verify(serverDevice).updateStatus(ONLINE);
    }

    @Test
    void shouldSendSemanticOnOffCommand() {
        ChannelUID channelUID = new ChannelUID("binding:thing:sub:1");
        var thing = thingWithChannelType(channelUID, GATEWAY_LOCK_VALUE_CHANNEL_ID);
        when(serverDevice.getThing()).thenReturn(thing);

        handlerCommandTrait.handleOnOffCommand(channelUID, ON);

        verify(serverDevice)
                .write(argThat(proto -> proto instanceof SuplaChannelNewValue newValue
                        && Arrays.equals(
                                newValue.value(), ChannelTypeEncoder.INSTANCE.encode(GatewayLockValue.UNLOCKED))));
    }

    @Test
    void shouldSendSemanticGateOnOffCommandAndRecordPreviousState() {
        ChannelUID channelUID = new ChannelUID("binding:thing:sub:1");
        var thing = thingWithChannelType(channelUID, GATE_VALUE_CHANNEL_ID);
        when(serverDevice.getThing()).thenReturn(thing);

        handlerCommandTrait.handleOnOffCommand(channelUID, ON);

        verify(serverDevice)
                .write(argThat(proto -> proto instanceof SuplaChannelNewValue newValue
                        && Arrays.equals(newValue.value(), ChannelTypeEncoder.INSTANCE.encode(GateValue.OPEN))));
        assertThat(channelNumberMap.get(1).previousState()).isEqualTo(OFF);
    }

    @Test
    void shouldSendSemanticOpenClosedCommand() {
        ChannelUID channelUID = new ChannelUID("binding:thing:sub:1");
        var thing = thingWithChannelType(channelUID, GATE_VALUE_CHANNEL_ID);
        when(serverDevice.getThing()).thenReturn(thing);

        handlerCommandTrait.handleOpenClosedCommand(channelUID, OPEN);

        verify(serverDevice)
                .write(argThat(proto -> proto instanceof SuplaChannelNewValue newValue
                        && Arrays.equals(newValue.value(), ChannelTypeEncoder.INSTANCE.encode(GateValue.OPEN))));
    }

    @Test
    void shouldSendSemanticUpDownCommandAndRecordPreviousState() {
        ChannelUID channelUID = new ChannelUID("binding:thing:sub:1");
        var thing = thingWithChannelType(channelUID, GATE_VALUE_CHANNEL_ID);
        when(serverDevice.getThing()).thenReturn(thing);

        handlerCommandTrait.handleUpDownCommand(channelUID, UpDownType.UP);

        verify(serverDevice)
                .write(argThat(proto -> proto instanceof SuplaChannelNewValue newValue
                        && Arrays.equals(newValue.value(), ChannelTypeEncoder.INSTANCE.encode(GateValue.OPEN))));
        assertThat(channelNumberMap.get(1).previousState()).isEqualTo(UpDownType.DOWN);
    }

    @Test
    void shouldMapSemanticMovementPercentEndpoints() {
        ChannelUID channelUID = new ChannelUID("binding:thing:sub:1");
        var thing = thingWithChannelType(channelUID, GATE_VALUE_CHANNEL_ID);
        when(serverDevice.getThing()).thenReturn(thing);

        handlerCommandTrait.handlePercentCommand(channelUID, new PercentType(100));

        verify(serverDevice)
                .write(argThat(proto -> proto instanceof SuplaChannelNewValue newValue
                        && Arrays.equals(newValue.value(), ChannelTypeEncoder.INSTANCE.encode(GateValue.CLOSE))));
        assertThat(channelNumberMap.get(1).previousState()).isEqualTo(UpDownType.UP);
    }

    @Test
    void shouldIgnoreUnsupportedSemanticMovementStopCommandWithoutWarning() {
        ChannelUID channelUID = new ChannelUID("binding:thing:sub:1");
        var thing = thingWithChannelType(channelUID, GATE_VALUE_CHANNEL_ID);
        when(serverDevice.getThing()).thenReturn(thing);

        handlerCommandTrait.handleStopMoveTypeCommand(channelUID, StopMoveType.STOP);

        verify(serverDevice, never()).write(any());
        verify(logger)
                .debug(
                        "Ignoring `{}` ({}) on semantic movement channel `{}` because there is no STOP/MOVE payload",
                        StopMoveType.STOP,
                        StopMoveType.class.getSimpleName(),
                        channelUID);
        verify(logger, never()).warn(anyString(), any(), any(), any());
    }

    @Test
    void shouldFailWhenChannelNumberCannotBeParsed() {
        ChannelUID channelUID = new ChannelUID("binding:thing:sub:notANumber");

        assertThatThrownBy(() -> handlerCommandTrait.handleOnOffCommand(channelUID, ON))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot find channel number from");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static SuplaWriteFuture successfulWriteFuture() {
        var future = mock(SuplaWriteFuture.class);
        lenient().when(future.addListener(any())).thenAnswer(invocation -> {
            var listener = (GenericFutureListener) invocation.getArgument(0);
            listener.operationComplete(future);
            return future;
        });
        return future;
    }

    private static Thing thingWithChannelType(ChannelUID channelUID, String channelTypeId) {
        var thing = mock(Thing.class);
        var channel = mock(Channel.class);
        when(thing.getChannel(channelUID)).thenReturn(channel);
        when(channel.getChannelTypeUID()).thenReturn(new ChannelTypeUID(BINDING_ID, channelTypeId));
        return thing;
    }
}
