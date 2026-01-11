package pl.grzeslowski.openhab.supla.internal.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openhab.core.library.unit.SIUnits.CELSIUS;
import static org.openhab.core.types.UnDefType.UNDEF;
import static pl.grzeslowski.jsupla.protocol.api.RgbwBitFunction.SUPLA_RGBW_BIT_FUNC_DIMMER;
import static pl.grzeslowski.jsupla.protocol.api.RgbwBitFunction.SUPLA_RGBW_BIT_FUNC_DIMMER_AND_RGB_LIGHTING;
import static pl.grzeslowski.jsupla.protocol.api.RgbwBitFunction.SUPLA_RGBW_BIT_FUNC_DIMMER_CCT;
import static pl.grzeslowski.jsupla.protocol.api.RgbwBitFunction.SUPLA_RGBW_BIT_FUNC_DIMMER_CCT_AND_RGB;
import static pl.grzeslowski.jsupla.protocol.api.RgbwBitFunction.SUPLA_RGBW_BIT_FUNC_RGB_LIGHTING;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.types.State;
import pl.grzeslowski.jsupla.protocol.api.RgbwBitFunction;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.*;
import pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ChannelIds.RgbwLed;
import pl.grzeslowski.openhab.supla.internal.server.ChannelValueToState.ChannelState;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;

@ExtendWith(MockitoExtension.class)
class ChannelValueToStateTest {
    private final ThingUID thingUID = new ThingUID("supla:test:1");
    private final RgbValue rgbValue = new RgbValue(55, 10, 1, 2, 3, 77);

    private static Stream<Arguments> onRgbValue() {
        return Stream.of(
                Arguments.of(SUPLA_RGBW_BIT_FUNC_RGB_LIGHTING, List.of(RgbwLed.COLOR)),
                Arguments.of(SUPLA_RGBW_BIT_FUNC_DIMMER_AND_RGB_LIGHTING, List.of(RgbwLed.COLOR, RgbwLed.BRIGHTNESS)),
                Arguments.of(SUPLA_RGBW_BIT_FUNC_DIMMER_CCT_AND_RGB, List.of(RgbwLed.COLOR, RgbwLed.BRIGHTNESS_CCT)),
                Arguments.of(SUPLA_RGBW_BIT_FUNC_DIMMER, List.of(RgbwLed.BRIGHTNESS)),
                Arguments.of(SUPLA_RGBW_BIT_FUNC_DIMMER_CCT, List.of(RgbwLed.BRIGHTNESS_CCT)));
    }

    private DeviceChannel mockDeviceChannel(int number) {
        return new DeviceChannel(
                number, false, null, Set.of(), null, Set.of(), new byte[8], null, null, null, 0, Set.of(), 0);
    }

    private DeviceChannel mockDeviceChannel(int number, RgbwBitFunction... functions) {
        return new DeviceChannel(
                number, false, null, Set.of(), null, Set.of(functions), new byte[8], null, null, null, 0, Set.of(), 0);
    }

    @DisplayName("should convert onRgbValue into proper channels")
    @ParameterizedTest
    @MethodSource
    void onRgbValue(RgbwBitFunction bitFunction, List<String> expectedChannels) {
        // given
        final int deviceChannelNumber = 6;
        var deviceChannel = mockDeviceChannel(deviceChannelNumber, bitFunction);
        var converter = new ChannelValueToState(thingUID, deviceChannel);
        final var expectedChannelIds = expectedChannels.stream()
                .map(channelId -> new ChannelUID(new ChannelGroupUID(thingUID, "" + deviceChannelNumber), channelId)
                        .getAsString())
                .toList();

        // when
        final List<ChannelState> channelStates = converter.switchOn(rgbValue).toList();

        // then
        assertThat(channelStates).hasSize(expectedChannels.size());
        final List<String> channelIds = channelStates.stream()
                .map(channelState -> channelState.uid().getAsString())
                .toList();
        assertThat(channelIds).containsExactlyInAnyOrderElementsOf(expectedChannelIds);
    }

    @DisplayName("should convert onRgbValue into proper states")
    @ParameterizedTest
    @MethodSource("onRgbValue")
    void onRgbValueStates(RgbwBitFunction bitFunction, List<String> channels) {
        // given
        final int deviceChannelNumber = 6;
        var deviceChannel = mockDeviceChannel(deviceChannelNumber, bitFunction);
        var converter = new ChannelValueToState(thingUID, deviceChannel);

        // when
        final List<ChannelState> channelStates = converter.switchOn(rgbValue).toList();

        // then
        for (ChannelState channelState : channelStates) {
            if (channelState
                    .uid()
                    .getAsString()
                    .equals(new ChannelUID(new ChannelGroupUID(thingUID, "" + deviceChannelNumber), RgbwLed.COLOR)
                            .getAsString())) {
                var hsbType = HSBType.fromRGB(1, 2, 3);
                var expected = new HSBType(
                        hsbType.getHue(), hsbType.getSaturation(), new PercentType(rgbValue.colorBrightness()));
                assertThat(channelState.state()).isEqualTo(expected);
            } else if (channelState
                    .uid()
                    .getAsString()
                    .equals(new ChannelUID(new ChannelGroupUID(thingUID, "" + deviceChannelNumber), RgbwLed.BRIGHTNESS)
                            .getAsString())) {
                assertThat(channelState.state()).isEqualTo(new PercentType(55));
            } else if (channelState
                    .uid()
                    .getAsString()
                    .equals(new ChannelUID(
                                    new ChannelGroupUID(thingUID, "" + deviceChannelNumber), RgbwLed.BRIGHTNESS_CCT)
                            .getAsString())) {
                assertThat(channelState.state()).isEqualTo(new PercentType(77));
            }
        }
    }

    @Test
    void shouldReturnUndefForSpecialTemperatureValue() {
        var converter = new ChannelValueToState(thingUID, mockDeviceChannel(7));
        var temperatureValue = new TemperatureDoubleValue(BigDecimal.valueOf(-275.0));

        var state =
                converter.switchOn(temperatureValue).findFirst().orElseThrow().state();

        assertThat(state).isEqualTo(UNDEF);
    }

    @Test
    void shouldHandleUnknownValueNulls() {
        var converter = new ChannelValueToState(thingUID, mockDeviceChannel(5));
        var unknownValue = new UnknownValue(new byte[0], "oops");

        List<State> messageStates =
                converter.switchOn(unknownValue).map(ChannelState::state).toList();

        assertThat(messageStates).containsExactly(new StringType("oops"));
    }

    @Test
    void shouldReturnGroupedUidsForTemperatureAndHumidity() {
        var converter = new ChannelValueToState(thingUID, mockDeviceChannel(12));
        var channelValue = new TemperatureAndHumidityValue(BigDecimal.valueOf(777), BigDecimal.valueOf(88));

        List<ChannelUID> uids =
                converter.switchOn(channelValue).map(ChannelState::uid).toList();

        assertThat(uids)
                .containsExactly(
                        new ChannelUID(new ChannelGroupUID(thingUID, "12"), "temperature"),
                        new ChannelUID(new ChannelGroupUID(thingUID, "12"), "humidity"));
    }

    @Test
    void shouldUseCelsiusForHeatpolThermostatTemperatures() {
        var converter = new ChannelValueToState(thingUID, mockDeviceChannel(9));
        var heatpolValue = new HeatpolThermostatValue(true, Set.of(), BigDecimal.valueOf(21.5), BigDecimal.valueOf(19));

        List<ChannelState> channelStates = converter.switchOn(heatpolValue).toList();

        var channelGroupUid = new ChannelGroupUID(thingUID, "9");
        var measuredUid = new ChannelUID(channelGroupUid, "measuredTemperature");
        var presetUid = new ChannelUID(channelGroupUid, "presetTemperature");
        var measuredState = channelStates.stream()
                .filter(state -> state.uid().equals(measuredUid))
                .findFirst()
                .orElseThrow()
                .state();
        var presetState = channelStates.stream()
                .filter(state -> state.uid().equals(presetUid))
                .findFirst()
                .orElseThrow()
                .state();

        assertThat(measuredState).isEqualTo(new QuantityType<>(BigDecimal.valueOf(21.5), CELSIUS));
        assertThat(presetState).isEqualTo(new QuantityType<>(BigDecimal.valueOf(19), CELSIUS));
    }
}
