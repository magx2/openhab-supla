package pl.grzeslowski.openhab.supla.internal.server;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_CMD_RESET_COUNTERS;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_RESULT_DONE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_RESULT_NOT_SUPPORTED;

import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.DeviceCalCfgResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.DeviceCalCfgRequest;
import pl.grzeslowski.jsupla.server.SuplaWriter;
import pl.grzeslowski.openhab.supla.actions.SuplaServerDeviceActions;
import pl.grzeslowski.openhab.supla.internal.server.handler.ServerSuplaDeviceHandler;

@ExtendWith(MockitoExtension.class)
class SuplaServerDeviceActionsTest {
    @Mock
    private ServerSuplaDeviceHandler handler;

    @Mock
    private SuplaWriter writer;

    @Mock
    private Thing thing;

    @InjectMocks
    private SuplaServerDeviceActions actions;

    private ChannelFuture successfulFuture;

    @BeforeEach
    void setUp() {
        var channel = new EmbeddedChannel();
        successfulFuture = new DefaultChannelPromise(channel).setSuccess(null);

        actions.setThingHandler(handler);
        org.mockito.Mockito.lenient().when(handler.getWriter()).thenReturn(new AtomicReference<>(writer));
        org.mockito.Mockito.lenient().when(handler.getThing()).thenReturn(thing);
        org.mockito.Mockito.lenient().when(thing.getUID()).thenReturn(new ThingUID("supla:test:1"));
        org.mockito.Mockito.lenient()
                .when(handler.hasRegisteredElectricityMeterChannel(7))
                .thenReturn(true);
        org.mockito.Mockito.lenient()
                .when(writer.write(argThat(proto -> proto instanceof DeviceCalCfgRequest)))
                .thenReturn(successfulFuture);
    }

    @Test
    void shouldSendResetCountersRequest() throws Exception {
        when(handler.listenForDeviceCalCfgResult(30, SECONDS))
                .thenReturn(new DeviceCalCfgResult(
                        0, 7, SUPLA_CALCFG_CMD_RESET_COUNTERS, SUPLA_CALCFG_RESULT_DONE, 0L, new byte[0]));

        actions.resetElectricMeterCounters("supla:test:1:7#power");

        var inOrder = inOrder(handler, writer);
        inOrder.verify(handler).clearDeviceCalCfgResult();
        inOrder.verify(writer)
                .write(argThat(proto -> proto instanceof DeviceCalCfgRequest request
                        && request.senderId() == 0
                        && request.channelNumber() == 7
                        && request.command() == SUPLA_CALCFG_CMD_RESET_COUNTERS
                        && request.superUserAuthorized() == 1
                        && request.dataType() == 0
                        && request.dataSize() == 0L
                        && request.data().length == 0));
    }

    @Test
    void shouldFailWhenDeviceRejectsResetCounters() throws Exception {
        when(handler.listenForDeviceCalCfgResult(30, SECONDS))
                .thenReturn(new DeviceCalCfgResult(
                        0, 7, SUPLA_CALCFG_CMD_RESET_COUNTERS, SUPLA_CALCFG_RESULT_NOT_SUPPORTED, 0L, new byte[0]));

        assertThatThrownBy(() -> actions.resetElectricMeterCounters("supla:test:1:7#power"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Reset counters did not succeed");
    }

    @Test
    void shouldFailWhenChannelNumberCannotBeParsed() {
        assertThatThrownBy(() -> actions.resetElectricMeterCounters("supla:test:1:not-a-channel"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot find channel number from");
    }

    @Test
    void shouldFailWhenChannelUidBelongsToDifferentThing() {
        assertThatThrownBy(() -> actions.resetElectricMeterCounters("supla:test:2:7#power"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to thing");
    }

    @Test
    void shouldSendResetCountersRequestForChannelNumber() throws Exception {
        when(handler.listenForDeviceCalCfgResult(30, SECONDS))
                .thenReturn(new DeviceCalCfgResult(
                        0, 7, SUPLA_CALCFG_CMD_RESET_COUNTERS, SUPLA_CALCFG_RESULT_DONE, 0L, new byte[0]));

        actions.resetElectricMeterCounters(7);

        verify(writer)
                .write(argThat(proto -> proto instanceof DeviceCalCfgRequest request
                        && request.channelNumber() == 7
                        && request.command() == SUPLA_CALCFG_CMD_RESET_COUNTERS));
    }

    @Test
    void shouldFailWhenChannelNumberIsNotRegistered() {
        assertThatThrownBy(() -> actions.resetElectricMeterCounters(11))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot find electricity meter device channel for channel number 11");
    }

    @Test
    void shouldFailWhenRegisteredChannelIsNotElectricityMeter() {
        when(handler.hasRegisteredElectricityMeterChannel(9)).thenReturn(false);

        assertThatThrownBy(() -> actions.resetElectricMeterCounters(9))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot find electricity meter device channel for channel number 9");
    }
}
