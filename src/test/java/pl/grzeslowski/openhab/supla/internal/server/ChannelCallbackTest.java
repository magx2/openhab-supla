package pl.grzeslowski.openhab.supla.internal.server;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.*;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.type.ChannelTypeUID;
import pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;

class ChannelCallbackTest {
    private final ThingUID thingUID = new ThingUID("supla:test:1");

    private static Stream<Arguments> semanticChannels() {
        return Stream.of(
                Arguments.of(
                        "Gate", GATE_VALUE_CHANNEL_ID, "supla", "Contact", (Function<ChannelCallback, Stream<Channel>>)
                                ChannelCallback::onGateValue),
                Arguments.of("Gateway Lock", GATEWAY_LOCK_VALUE_CHANNEL_ID, "supla", "Switch", (Function<
                                ChannelCallback, Stream<Channel>>)
                        ChannelCallback::onGatewayLockValue),
                Arguments.of("Garage Door", GARAGE_DOOR_VALUE_CHANNEL_ID, "supla", "Contact", (Function<
                                ChannelCallback, Stream<Channel>>)
                        ChannelCallback::onGarageDoorValue),
                Arguments.of("Door Lock", DOOR_LOCK_VALUE_CHANNEL_ID, "supla", "Switch", (Function<
                                ChannelCallback, Stream<Channel>>)
                        ChannelCallback::onDoorLockValue),
                Arguments.of("Roller Shutter", ROLLER_SHUTTER_VALUE_CHANNEL_ID, "supla", "Contact", (Function<
                                ChannelCallback, Stream<Channel>>)
                        ChannelCallback::onRollerShutterValue),
                Arguments.of("Power Switch", SYSTEM_POWER_CHANNEL_TYPE, "system", "Switch", (Function<
                                ChannelCallback, Stream<Channel>>)
                        ChannelCallback::onPowerSwitchValue),
                Arguments.of("Light Switch", LIGHT_SWITCH_VALUE_CHANNEL_ID, "supla", "Switch", (Function<
                                ChannelCallback, Stream<Channel>>)
                        ChannelCallback::onLightSwitchValue),
                Arguments.of("Staircase Timer", STAIRCASE_TIMER_VALUE_CHANNEL_ID, "supla", "Switch", (Function<
                                ChannelCallback, Stream<Channel>>)
                        ChannelCallback::onStaircaseTimerValue),
                Arguments.of("Roof Window", ROOF_WINDOW_VALUE_CHANNEL_ID, "supla", "Contact", (Function<
                                ChannelCallback, Stream<Channel>>)
                        ChannelCallback::onRoofWindowValue),
                Arguments.of("Facade Blind", FACADE_BLIND_VALUE_CHANNEL_ID, "supla", "Contact", (Function<
                                ChannelCallback, Stream<Channel>>)
                        ChannelCallback::onFacadeBlindValue),
                Arguments.of("Terrace Awning", TERRACE_AWNING_VALUE_CHANNEL_ID, "supla", "Contact", (Function<
                                ChannelCallback, Stream<Channel>>)
                        ChannelCallback::onTerraceAwningValue),
                Arguments.of("Projector Screen", PROJECTOR_SCREEN_VALUE_CHANNEL_ID, "supla", "Contact", (Function<
                                ChannelCallback, Stream<Channel>>)
                        ChannelCallback::onProjectorScreenValue),
                Arguments.of("Curtain", CURTAIN_VALUE_CHANNEL_ID, "supla", "Contact", (Function<
                                ChannelCallback, Stream<Channel>>)
                        ChannelCallback::onCurtainValue),
                Arguments.of("Vertical Blind", VERTICAL_BLIND_VALUE_CHANNEL_ID, "supla", "Contact", (Function<
                                ChannelCallback, Stream<Channel>>)
                        ChannelCallback::onVerticalBlindValue),
                Arguments.of("Roller Garage Door", ROLLER_GARAGE_DOOR_VALUE_CHANNEL_ID, "supla", "Contact", (Function<
                                ChannelCallback, Stream<Channel>>)
                        ChannelCallback::onRollerGarageDoorValue),
                Arguments.of("Pump Switch", PUMP_SWITCH_VALUE_CHANNEL_ID, "supla", "Switch", (Function<
                                ChannelCallback, Stream<Channel>>)
                        ChannelCallback::onPumpSwitchValue),
                Arguments.of(
                        "Heat Or Cold Source Switch",
                        HEAT_OR_COLD_SOURCE_SWITCH_VALUE_CHANNEL_ID,
                        "supla",
                        "Switch",
                        (Function<ChannelCallback, Stream<Channel>>) ChannelCallback::onHeatOrColdSourceSwitchValue));
    }

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

    @ParameterizedTest
    @MethodSource("semanticChannels")
    void shouldCreateSemanticChannel(
            String label,
            String channelTypeId,
            String bindingId,
            String acceptedItemType,
            Function<ChannelCallback, Stream<Channel>> channelFactory) {
        var callback = new ChannelCallback(thingUID, mockDeviceChannel(6));

        List<Channel> channels = channelFactory.apply(callback).toList();

        assertThat(channels).singleElement().satisfies(channel -> {
            assertThat(channel.getUID()).isEqualTo(new ChannelUID(thingUID, "6"));
            assertThat(channel.getChannelTypeUID()).isEqualTo(new ChannelTypeUID(bindingId, channelTypeId));
            assertThat(channel.getLabel()).isEqualTo(label);
            assertThat(channel.getAcceptedItemType()).isEqualTo(acceptedItemType);
        });
    }

    private DeviceChannel mockDeviceChannel(int number) {
        return new DeviceChannel(
                number, false, null, Set.of(), null, Set.of(), new byte[8], null, null, null, 0, Set.of(), 0);
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

    @Test
    void shouldCreateElectricityMeterChannelsWithCompatibleItemTypes() {
        var callback = new ChannelCallback(thingUID, mockDeviceChannel(11));

        List<Channel> channels = callback.onElectricityMeter().toList();

        var groupUid = new ChannelGroupUID(thingUID, "11");
        assertThat(channels)
                .filteredOn(channel -> channel.getUID().equals(new ChannelUID(groupUid, "totalForwardActiveEnergy")))
                .singleElement()
                .extracting(Channel::getAcceptedItemType)
                .isEqualTo("Number:Energy");
        assertThat(channels)
                .filteredOn(channel -> channel.getUID().equals(new ChannelUID(groupUid, "totalReverseActiveEnergy")))
                .singleElement()
                .extracting(Channel::getAcceptedItemType)
                .isEqualTo("Number:Energy");
        assertThat(channels)
                .filteredOn(channel -> channel.getUID().equals(new ChannelUID(groupUid, "totalForwardReactiveEnergy")))
                .singleElement()
                .extracting(Channel::getAcceptedItemType)
                .isEqualTo("Number:Energy");
        assertThat(channels)
                .filteredOn(channel -> channel.getUID().equals(new ChannelUID(groupUid, "totalReverseReactiveEnergy")))
                .singleElement()
                .extracting(Channel::getAcceptedItemType)
                .isEqualTo("Number:Energy");
        assertThat(channels)
                .filteredOn(channel ->
                        channel.getUID().equals(new ChannelUID(groupUid, "totalForwardActiveEnergyBalanced")))
                .singleElement()
                .extracting(Channel::getAcceptedItemType)
                .isEqualTo("Number:Energy");
        assertThat(channels)
                .filteredOn(channel ->
                        channel.getUID().equals(new ChannelUID(groupUid, "totalReverseActiveEnergyBalanced")))
                .singleElement()
                .extracting(Channel::getAcceptedItemType)
                .isEqualTo("Number:Energy");
        assertThat(channels)
                .filteredOn(channel -> channel.getUID().equals(new ChannelUID(groupUid, "period")))
                .singleElement()
                .satisfies(channel -> {
                    assertThat(channel.getAcceptedItemType()).isEqualTo("Number:Time");
                    assertThat(channel.getChannelTypeUID())
                            .isEqualTo(new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, TIME_CHANNEL_ID));
                });
        assertThat(channels)
                .filteredOn(channel -> channel.getUID().equals(new ChannelUID(groupUid, "totalCost")))
                .singleElement()
                .satisfies(channel -> {
                    assertThat(channel.getAcceptedItemType()).isEqualTo("Number:Currency");
                    assertThat(channel.getChannelTypeUID())
                            .isEqualTo(new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, CURRENCY_CHANNEL_ID));
                });
        assertThat(channels)
                .filteredOn(channel -> channel.getUID().equals(new ChannelUID(groupUid, "pricePerUnit")))
                .singleElement()
                .satisfies(channel -> {
                    assertThat(channel.getAcceptedItemType()).isEqualTo("Number:EnergyPrice");
                    assertThat(channel.getChannelTypeUID())
                            .isEqualTo(new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, ENERGY_PRICE_CHANNEL_ID));
                });
        assertThat(channels)
                .filteredOn(channel -> channel.getUID().equals(new ChannelUID(groupUid, "voltagePhaseAngle12")))
                .singleElement()
                .satisfies(channel -> {
                    assertThat(channel.getAcceptedItemType()).isEqualTo("Number:Angle");
                    assertThat(channel.getChannelTypeUID())
                            .isEqualTo(new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, ANGLE_CHANNEL_ID));
                });
        assertThat(channels)
                .filteredOn(channel -> channel.getUID().equals(new ChannelUID(groupUid, "phase-1-frequency")))
                .singleElement()
                .satisfies(channel -> {
                    assertThat(channel.getAcceptedItemType()).isEqualTo("Number:Frequency");
                    assertThat(channel.getChannelTypeUID())
                            .isEqualTo(new ChannelTypeUID(
                                    SuplaBindingConstants.BINDING_ID,
                                    SuplaBindingConstants.Channels.FREQUENCY_CHANNEL_ID));
                });
    }
}
