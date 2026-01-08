package pl.grzeslowski.openhab.supla.internal.server;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.HUMIDITY_CHANNEL_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.HVAC_MODE_CHANNEL_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.HVAC_TEMPERATURE_COOL_CHANNEL_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.HVAC_TEMPERATURE_HEAT_CHANNEL_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.HVAC_WORKING_CHANNEL_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.SWITCH_CHANNEL_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.TEMPERATURE_CHANNEL_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.UNKNOWN_CHANNEL_ID;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.type.ChannelTypeUID;
import pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;

class ChannelCallbackTest {
    private final ThingUID thingUID = new ThingUID("supla:test:1");

    @Test
    void shouldCreateSwitchChannel() {
        var callback = new ChannelCallback(thingUID, mockDeviceChannel(5));

        List<Channel> channels = callback.onOnOff().toList();

        assertThat(channels).singleElement().satisfies(channel -> {
            assertThat(channel.getUID()).isEqualTo(new ChannelUID(thingUID, "5"));
            assertThat(channel.getChannelTypeUID())
                    .isEqualTo(new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, SWITCH_CHANNEL_ID));
            assertThat(channel.getLabel()).isEqualTo("Allows you to turn thing ON/OFF");
            assertThat(channel.getAcceptedItemType()).isEqualTo("Switch");
        });
    }

    private DeviceChannel mockDeviceChannel(int number) {
        return new DeviceChannel(number, false, null, null, null, null, new byte[8], null, null, null, 0, Set.of(), 0);
    }

    @Test
    void shouldCreateTemperatureAndHumidityGroup() {
        var callback = new ChannelCallback(thingUID, mockDeviceChannel(3));

        List<Channel> channels = callback.onTemperatureAndHumidityValue().toList();

        assertThat(channels).hasSize(2);
        assertThat(channels)
                .extracting(Channel::getUID)
                .containsExactly(
                        new ChannelUID(new ChannelGroupUID(thingUID, "3"), "temperature"),
                        new ChannelUID(new ChannelGroupUID(thingUID, "3"), "humidity"));
        assertThat(channels)
                .extracting(Channel::getChannelTypeUID)
                .containsExactly(
                        new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, TEMPERATURE_CHANNEL_ID),
                        new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, HUMIDITY_CHANNEL_ID));
    }

    @Test
    void shouldCreateHvacChannels() {
        var callback = new ChannelCallback(thingUID, mockDeviceChannel(7));

        List<Channel> channels = callback.onHvacValue().toList();

        assertThat(channels).hasSizeGreaterThanOrEqualTo(4);
        assertThat(channels)
                .extracting(Channel::getChannelTypeUID)
                .contains(
                        new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, HVAC_WORKING_CHANNEL_ID),
                        new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, HVAC_MODE_CHANNEL_ID),
                        new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, HVAC_TEMPERATURE_HEAT_CHANNEL_ID),
                        new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, HVAC_TEMPERATURE_COOL_CHANNEL_ID));
    }

    @Test
    void shouldCreateUnknownChannel() {
        var callback = new ChannelCallback(thingUID, mockDeviceChannel(10));

        Channel channel = callback.onUnknownValue().findFirst().orElseThrow();

        assertThat(channel.getUID()).isEqualTo(new ChannelUID(thingUID, "10"));
        assertThat(channel.getChannelTypeUID())
                .isEqualTo(new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, UNKNOWN_CHANNEL_ID));
        assertThat(channel.getLabel()).isEqualTo("Unknown");
    }
}
