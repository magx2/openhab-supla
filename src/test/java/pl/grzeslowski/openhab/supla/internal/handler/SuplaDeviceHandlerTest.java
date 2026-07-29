package pl.grzeslowski.openhab.supla.internal.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.openhab.core.library.types.OnOffType.ON;
import static org.openhab.core.types.RefreshType.REFRESH;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.BINDING_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.GATEWAY_LOCK_VALUE_CHANNEL_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.PUMP_SWITCH_VALUE_CHANNEL_ID;

import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.slf4j.Logger;

class SuplaDeviceHandlerTest {
    private final ThingUID thingUID = new ThingUID("supla:test:1");

    @Test
    void shouldIgnoreCommandsForReadOnlySemanticChannels() {
        var channelUID = new ChannelUID(thingUID, "6");
        var handler = new TestSuplaDeviceHandler(thingWithChannelType(channelUID, PUMP_SWITCH_VALUE_CHANNEL_ID));

        handler.handleCommand(channelUID, ON);

        assertThat(handler.onOffCommands).isZero();
    }

    @Test
    void shouldHandleRefreshForReadOnlySemanticChannels() {
        var channelUID = new ChannelUID(thingUID, "6");
        var handler = new TestSuplaDeviceHandler(thingWithChannelType(channelUID, PUMP_SWITCH_VALUE_CHANNEL_ID));

        handler.handleCommand(channelUID, REFRESH);

        assertThat(handler.refreshCommands).isEqualTo(1);
    }

    @Test
    void shouldDispatchCommandsForWritableChannels() {
        var channelUID = new ChannelUID(thingUID, "6");
        var handler = new TestSuplaDeviceHandler(thingWithChannelType(channelUID, GATEWAY_LOCK_VALUE_CHANNEL_ID));

        handler.handleCommand(channelUID, ON);

        assertThat(handler.onOffCommands).isEqualTo(1);
    }

    private Thing thingWithChannelType(ChannelUID channelUID, String channelTypeId) {
        var thing = mock(Thing.class);
        var channel = mock(Channel.class);
        when(thing.getChannel(channelUID)).thenReturn(channel);
        when(channel.getChannelTypeUID()).thenReturn(new ChannelTypeUID(BINDING_ID, channelTypeId));
        return thing;
    }

    private static class TestSuplaDeviceHandler extends SuplaDeviceHandler {
        private final Logger logger = mock(Logger.class);
        private int refreshCommands;
        private int onOffCommands;

        private TestSuplaDeviceHandler(Thing thing) {
            super(thing);
        }

        @Override
        protected @Nullable String findGuid() {
            return null;
        }

        @Override
        protected void internalInitialize() {}

        @Override
        protected Logger getLogger() {
            return logger;
        }

        @Override
        public void handleRefreshCommand(ChannelUID channelUID) {
            refreshCommands++;
        }

        @Override
        public void handleOnOffCommand(ChannelUID channelUID, OnOffType command) {
            onOffCommands++;
        }

        @Override
        public void handleUpDownCommand(ChannelUID channelUID, UpDownType command) {}

        @Override
        public void handleHsbCommand(ChannelUID channelUID, HSBType command) {}

        @Override
        public void handleOpenClosedCommand(ChannelUID channelUID, OpenClosedType command) {}

        @Override
        public void handlePercentCommand(ChannelUID channelUID, PercentType command) {}

        @Override
        public void handleStopMoveTypeCommand(ChannelUID channelUID, StopMoveType command) {}

        @Override
        public void handleStringCommand(ChannelUID channelUID, StringType command) {}

        @Override
        public void handleQuantityType(ChannelUID channelUID, QuantityType<?> command) {}
    }
}
